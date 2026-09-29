#!/bin/bash
# Applies kiwi's Android 13 platform patches to a synced LineageOS 20 tree.
# Run from the top of the tree: bash device/huawei/kiwi/patches/apply-patches.sh
# Patches already present (same subject in the project's recent history) are
# skipped, so running it twice is safe.
set -e
top=$(pwd)
base="$(cd "$(dirname "$0")" && pwd)/lineage-20"
[ -d "$top/.repo" ] || { echo "run this from the top of the LineageOS tree"; exit 1; }
for dir in $(cd "$base" && find . -name '*.patch' -printf '%h\n' | sort -u); do
  proj=${dir#./}
  echo "== $proj"
  for p in "$base/$proj"/*.patch; do
    subj=$(git mailinfo /dev/null /dev/null < "$p" | sed -n 's/^Subject: //p')
    if git -C "$top/$proj" log --format=%s -n 300 | grep -Fqx -- "$subj"; then
      echo "   already applied: $subj"; continue
    fi
    if ! git -C "$top/$proj" am -q --3way "$p"; then
      git -C "$top/$proj" am --abort
      echo "!! failed: $p"; exit 1
    fi
    echo "   applied: $subj"
  done
done
echo "All kiwi platform patches are in place."
