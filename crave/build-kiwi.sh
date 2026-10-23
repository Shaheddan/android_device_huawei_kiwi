#!/bin/bash
# Build LineageOS 22.2 for kiwi (Honor 5X) on a Crave build node.
# Start it from the devspace, inside the LOS 22.1 clone (project 93):
#   crave run --projectID 93 --no-patch -- "curl -sfL https://raw.githubusercontent.com/Shaheddan/android_device_huawei_kiwi/lineage-22.2/crave/build-kiwi.sh | VARIANT=userdebug bash"
# WHY each step:
#  - manifests fetched fresh, so the node builds exactly what is on GitHub;
#  - /opt/crave/resync.sh instead of repo sync: Crave's rule, it manages their cache;
#  - patches applied AFTER the resync, because a resync resets the platform repos;
#  - userdebug by default (the daily-driver variant). The old userdebug `adb root` hang was a
#    kernel bug (f_fs AIO cancel leaked the ep files; fixed in kernel f792d4c); VARIANT=eng
#    still works for bring-up.
# No `set -e`: build/envsetup.sh is not written for it; critical steps fail explicitly.
VARIANT=${VARIANT:-userdebug}
RAW=https://raw.githubusercontent.com/Shaheddan/android_device_huawei_kiwi/lineage-22.2

# Crave has no LineageOS 22.2 project: the job runs in the LOS 22.1 workspace
# (project 93) and is re-initialised to lineage-22.2. Crave's accupara/los22 only has
# lineage-22.0 and 22.1 (git ls-remote, 2026-10-23), so unlike 21 this uses LineageOS's
# own manifest (AOSP tag android-15.0.0_r32). UNTESTED on Crave so far: if resync.sh
# rejects a non-accupara manifest, the first job shows it here.
# -pixel-extra: same group filter as the local tree, keeps Pixel repos out.
repo init -u https://github.com/LineageOS/android.git -b lineage-22.2 --git-lfs -g default,-pixel-extra \
  || { echo "!! repo init failed"; exit 1; }

rm -rf .repo/local_manifests && mkdir -p .repo/local_manifests || exit 1
for m in kiwi.xml kiwi-support.xml; do
  curl -sfL "$RAW/manifests/$m" -o ".repo/local_manifests/$m" || { echo "!! could not fetch $m"; exit 1; }
done

# Crave keeps .repo between jobs, and this workspace is re-initialised between LineageOS
# versions (it started as LOS 22.1, kiwi syncs LOS 21 into it, Android 15 will sync LOS 22).
# The tablet's first LineageOS 20 job after its 19.1 ones died right here: "You are
# attempting to fetch <sha>, which is in the commit graph file but not in the object
# database". A project's commit-graph (an index git writes during fetch and gc) still listed
# commits whose objects were gone; Crave's git 2.55 refuses to fetch then, and resync.sh's
# retry deletes only the checkout, not the shared object store, so it fails the same way.
# A commit-graph is only an index: deleting it loses nothing, and the next fetch brings back
# whatever is really missing (reproduced with git 2.55 for the tablet: same error before,
# clean fetch after). The find skips loose-object and pack directories so it stays quick.
if [ -d .repo ]; then
  KIWI_CG=$(find .repo/project-objects .repo/projects \
      -path '*/objects/??' -prune -o -path '*/objects/pack' -prune -o \
      \( -name commit-graph -o -name commit-graphs \) -path '*/objects/info/*' -print 2>/dev/null)
  printf '%s\n' "$KIWI_CG" | grep . | xargs -r rm -rf
  echo "kiwi: removed $(printf '%s\n' "$KIWI_CG" | grep -c .) commit-graph indexes under .repo"
  [ -z "$(find .repo/project-objects .repo/projects -path '*/objects/??' -prune -o -path '*/objects/pack' -prune -o \( -name commit-graph -o -name commit-graphs \) -path '*/objects/info/*' -print 2>/dev/null)" ] \
    || { echo "!! commit-graph indexes still present under .repo"; exit 1; }
fi
/opt/crave/resync.sh || { echo "!! resync failed"; exit 1; }
bash device/huawei/kiwi/patches/apply-patches.sh lineage-22.2 || { echo "!! kiwi patches failed"; exit 1; }

# kiwi: Crave wraps compilers by renaming each one to mbt-bin-<name> (build 17's traceback).
# LineageOS's gcc-4.9 "gcc" is itself a python wrapper that execs real-<its own file name>,
# so after the rename it looks for real-mbt-bin-<name> and the kernel build dies. Point those
# names at the real binaries; harmless where nothing was renamed.
for t in aarch64/aarch64-linux-android-4.9:aarch64-linux-android arm/arm-linux-androideabi-4.9:arm-linux-androideabi; do
  d=prebuilts/gcc/linux-x86/${t%%:*}/bin
  p=${t##*:}
  for c in gcc g++; do
    [ -e "$d/real-$p-$c" ] && ln -sfn "real-$p-$c" "$d/real-mbt-bin-$p-$c"
  done
  echo "kiwi: toolchain wrappers in $d:"
  ls -la "$d" | grep -E "mbt-bin|real-|$p-gcc\$|androidkernel-gcc" | sed 's/^/kiwi:   /'
done

source build/envsetup.sh
# LineageOS 22.2's release config is bp1a (build/release aliases aosp_current to it;
# 21 was ap2a, 22.1 ap4a).
lunch "lineage_kiwi-bp1a-$VARIANT" || { echo "!! lunch failed"; exit 1; }

# soong_ui starts each build by running the tree's CleanSpec steps that are new to this
# out/, and an installclean when the product or variant changed. The tablet's Crave build
# 306425, its first LineageOS 20 build in the workspace its 19.1 builds used, ran "Clean
# step: rm -rf out/host/linux-x86" and "rm -rf out/soong/host" there, which deleted the
# ncurses links made before it, so bcc_strip_attr failed at 35%. kiwi's LineageOS 21
# builds have not needed the links so far; the block is the same as on 18.1-20 so a
# host tool that needs ncurses 5 cannot stop a future 21 build either, and it runs
# after the clean steps for the same reason. `m nothing` runs that whole start stage
# (soong, CleanSpec, kati, installclean) and builds nothing; mka bacon then reuses its
# results with nothing left to clean, so the links made after it stay.
m nothing || { echo "!! m nothing failed"; exit 1; }

# LineageOS 18.1 to 20 run old host tools that need libncurses.so.5 and libtinfo.so.5, which
# Crave's Debian 13 image only has as .so.6 (all found on the tablet's 19.1 Crave builds):
#  - prebuilts/clang/host/linux-x86/clang-3289846 (clang.real, llvm-link, ...), which
#    compiles RenderScript's libclcore.bc (tablet crave build 305623 stopped there at 14%);
#  - tools built in the tree against the glibc 2.17 sysroot's ncurses 5 through
#    libLLVM_android.so: bcc_strip_attr, which make runs from out/host/linux-x86/bin (crave
#    build 305713, 42%), and llvm-rs-cc, which soong runs from out/soong/host/linux-x86/bin
#    on 19.1 (crave build 305971, 21%) and from out/host/linux-x86/bin on 20;
#  - the RenderScript tools in prebuilts/sdk/tools/linux (bcc_compat, llvm-rs-cc).
# The ncurses functions these tools import are unversioned, and the .so.6 libraries provide
# them, so .so.5 names pointing at the system's .so.6 are enough. Each of these trees keeps
# its tools in bin/ and their libraries in lib64/, which soong-built tools reach through
# RUNPATH $ORIGIN/../lib64, so the links go in each tree's lib64. LD_LIBRARY_PATH would not
# work: soong_ui hands ninja only an allowlist of environment variables and it is not on it.
# The links are made after `m nothing` (just above), never before it, because the start
# of a build can rm -rf out/host/linux-x86 and out/soong/host.
RSCLANG=prebuilts/clang/host/linux-x86/clang-3289846
KIWI_O=${OUT_DIR:-out}
KIWI_NC5=0
for n in libncurses.so.5 libtinfo.so.5; do
  ls "/lib/x86_64-linux-gnu/$n" "/usr/lib/x86_64-linux-gnu/$n" "/lib64/$n" "/usr/lib64/$n" >/dev/null 2>&1 \
    || KIWI_NC5=1
done
if [ "$KIWI_NC5" = 1 ]; then
  TINFO6=$(ls /usr/lib/x86_64-linux-gnu/libtinfo.so.6 /lib/x86_64-linux-gnu/libtinfo.so.6 2>/dev/null | head -1)
  NCURSES6=$(ls /usr/lib/x86_64-linux-gnu/libncurses.so.6 /lib/x86_64-linux-gnu/libncurses.so.6 2>/dev/null | head -1)
  [ -n "$TINFO6" ] || { echo "!! no libtinfo.so.6 on this host"; exit 1; }
  KIWI_TREES="$RSCLANG prebuilts/sdk/tools/linux $KIWI_O/host/linux-x86 $KIWI_O/soong/host/linux-x86"
  KIWI_DIRS=
  for t in $KIWI_TREES; do
    # out/ trees may not exist yet on a fresh workspace; their tools are built later and
    # must find the links then. Source trees are only touched where they exist.
    case "$t" in
      "$KIWI_O"/*) mkdir -p "$t/lib64" || { echo "!! cannot create $t/lib64"; exit 1; } ;;
      *) [ -d "$t/lib64" ] || continue ;;
    esac
    ln -sfn "$TINFO6" "$t/lib64/libtinfo.so.5"
    ln -sfn "${NCURSES6:-$TINFO6}" "$t/lib64/libncurses.so.5"
    KIWI_DIRS="$KIWI_DIRS $t/lib64"
  done
  echo "kiwi: libncurses.so.5 -> ${NCURSES6:-$TINFO6}, libtinfo.so.5 -> $TINFO6 in:$KIWI_DIRS"
  # Check every tool already on disk in those trees (out/ ones exist only when the
  # workspace kept an earlier build). One that still misses a name is fatal now rather than
  # 20 minutes into the build, except in prebuilts/sdk, which a platform build may never
  # run: that one is reported with its dynamic section, so the log has the data to fix it.
  KIWI_BAD=; KIWI_WARN=; KIWI_N=0
  for t in $KIWI_TREES; do
    for f in "$t"/bin/*; do
      [ -f "$f" ] && [ -x "$f" ] || continue
      KIWI_N=$((KIWI_N + 1))
      l=$(ldd "$f" 2>/dev/null) || true
      case "$l" in
        *"libncurses.so.5 => not found"*|*"libtinfo.so.5 => not found"*)
          echo "kiwi: $f still misses ncurses 5; NEEDED/RUNPATH:"
          readelf -d "$f" 2>&1 | grep -E 'NEEDED|RPATH|RUNPATH|readelf' | sed 's/^/kiwi:   /'
          case "$f" in prebuilts/sdk/*) KIWI_WARN="$KIWI_WARN $f" ;; *) KIWI_BAD="$KIWI_BAD $f" ;; esac ;;
      esac
    done
  done
  [ -z "$KIWI_WARN" ] || echo "kiwi: WARNING, still no libncurses.so.5/libtinfo.so.5 for:$KIWI_WARN"
  [ -z "$KIWI_BAD" ] || { echo "!! still no libncurses.so.5/libtinfo.so.5 for:$KIWI_BAD"; exit 1; }
  if [ -x "$RSCLANG/bin/clang.real" ]; then
    "$RSCLANG/bin/clang.real" --version >/dev/null 2>&1 \
      || { echo "!! $RSCLANG still cannot start:"; "$RSCLANG/bin/clang.real" --version; exit 1; }
  fi
  echo "kiwi: checked $KIWI_N host tools in:$KIWI_DIRS (bin/ beside each)"
fi

mka bacon || { echo "!! build failed"; exit 1; }
if [ -n "${GH_TOKEN:-}" ]; then
  bash device/huawei/kiwi/crave/upload-kiwi.sh || { echo "!! upload failed"; exit 1; }
fi
