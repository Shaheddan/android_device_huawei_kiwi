#!/bin/bash
# Applies kiwi's platform patches to a synced LineageOS tree.
#   bash device/huawei/kiwi/patches/apply-patches.sh [lineage-21|lineage-20]
# Run from the top of the tree; the default patch set is lineage-21.
# Patches already present (same subject in the project's recent history) are
# skipped, so running it twice is safe, e.g. on a reused Crave tree.
# WHY a fixed git identity: git am refuses to commit without one, and build
#   machines (Crave nodes) usually have none configured.
# WHY no --3way: it needs the patches' pre-image blobs, which shallow
#   (--depth=1) trees do not have; the set is made against lineage-21.0 itself.
set -e
ver=${1:-lineage-21}
top=$(pwd)
base="$(cd "$(dirname "$0")" && pwd)/$ver"
[ -d "$top/.repo" ] || { echo "run this from the top of the LineageOS tree"; exit 1; }
[ -d "$base" ] || { echo "no patch set: $base"; exit 1; }
id="-c user.name=kiwi-patches -c user.email=kiwi-patches@localhost"
for dir in $(cd "$base" && find . -name '*.patch' -printf '%h\n' | sort -u); do
  proj=${dir#./}
  echo "== $proj"
  [ -e "$top/$proj/.git" ] || { echo "!! project missing from the tree: $proj"; exit 1; }
  for p in "$base/$proj"/*.patch; do
    subj=$(git mailinfo /dev/null /dev/null < "$p" | sed -n 's/^Subject: //p')
    if git -C "$top/$proj" log --format=%s -n 300 | grep -Fqx -- "$subj"; then
      echo "   already applied: $subj"; continue
    fi
    if ! git $id -C "$top/$proj" am -q "$p"; then
      git -C "$top/$proj" am --abort || true
      echo "!! failed: $p"; exit 1
    fi
    echo "   applied: $subj"
  done
done
echo "All kiwi $ver platform patches are in place."
