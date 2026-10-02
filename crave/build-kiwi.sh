#!/bin/bash
# Build LineageOS 21 for kiwi (Honor 5X) on a Crave build node.
# Start it from the devspace, inside the LOS 21 clone:
#   crave run --no-patch -- "curl -sfL https://raw.githubusercontent.com/Shaheddan/android_device_huawei_kiwi/lineage-21/crave/build-kiwi.sh | VARIANT=eng bash"
# WHY each step:
#  - manifests fetched fresh, so the node builds exactly what is on GitHub;
#  - /opt/crave/resync.sh instead of repo sync: Crave's rule, it manages their cache;
#  - patches applied AFTER the resync, because a resync resets the platform repos;
#  - eng by default: adbd runs as root, sidestepping the unfixed userdebug `adb root` hang.
# No `set -e`: build/envsetup.sh is not written for it; critical steps fail explicitly.
VARIANT=${VARIANT:-eng}
RAW=https://raw.githubusercontent.com/Shaheddan/android_device_huawei_kiwi/lineage-21

rm -rf .repo/local_manifests && mkdir -p .repo/local_manifests || exit 1
for m in kiwi.xml kiwi-support.xml; do
  curl -sfL "$RAW/manifests/$m" -o ".repo/local_manifests/$m" || { echo "!! could not fetch $m"; exit 1; }
done

/opt/crave/resync.sh || { echo "!! resync failed"; exit 1; }
bash device/huawei/kiwi/patches/apply-patches.sh lineage-21 || { echo "!! kiwi patches failed"; exit 1; }

source build/envsetup.sh
lunch "lineage_kiwi-ap2a-$VARIANT" || { echo "!! lunch failed"; exit 1; }
mka bacon
