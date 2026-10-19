#!/bin/bash
# Build LineageOS 21 for kiwi (Honor 5X) on a Crave build node.
# Start it from the devspace, inside the LOS 21 clone:
#   crave run --no-patch -- "curl -sfL https://raw.githubusercontent.com/Shaheddan/android_device_huawei_kiwi/lineage-21/crave/build-kiwi.sh | VARIANT=userdebug bash"
# WHY each step:
#  - manifests fetched fresh, so the node builds exactly what is on GitHub;
#  - /opt/crave/resync.sh instead of repo sync: Crave's rule, it manages their cache;
#  - patches applied AFTER the resync, because a resync resets the platform repos;
#  - userdebug by default (the daily-driver variant). The old userdebug `adb root` hang was a
#    kernel bug (f_fs AIO cancel leaked the ep files; fixed in kernel f792d4c); VARIANT=eng
#    still works for bring-up.
# No `set -e`: build/envsetup.sh is not written for it; critical steps fail explicitly.
VARIANT=${VARIANT:-userdebug}
RAW=https://raw.githubusercontent.com/Shaheddan/android_device_huawei_kiwi/lineage-21

# Crave has no LineageOS 21 project: the job runs in the LOS 22.1 workspace
# (project 93) and is re-initialised to Crave's own LineageOS 21 manifest.
# accupara/los21 has the same remotes and android-14.0.0_r67 tag as
# LineageOS/android lineage-21.0, so kiwi's manifests and patches fit as-is.
# -pixel-extra: same group filter as the local tree, keeps Pixel repos out.
repo init -u https://github.com/accupara/los21.git -b lineage-21.0 --git-lfs -g default,-pixel-extra \
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
bash device/huawei/kiwi/patches/apply-patches.sh lineage-21 || { echo "!! kiwi patches failed"; exit 1; }

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
lunch "lineage_kiwi-ap2a-$VARIANT" || { echo "!! lunch failed"; exit 1; }
mka bacon || { echo "!! build failed"; exit 1; }
if [ -n "${GH_TOKEN:-}" ]; then
  bash device/huawei/kiwi/crave/upload-kiwi.sh || { echo "!! upload failed"; exit 1; }
fi
