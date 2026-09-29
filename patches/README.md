# LineageOS 20 platform patches for kiwi (Honor 5X)

Changes kiwi needs outside the device tree: camera HALv1 support (frameworks/av,
frameworks/base), legacy RIL SIM activation (frameworks/opt/telephony), Wi-Fi
WPA3 transition handling (packages/modules/Wifi), the HWUI surface-stats freeze
fix, and the 3.10-kernel eBPF/network adaptations.

1. Put the files from `manifests/` into `.repo/local_manifests/` and `repo sync`.
2. From the top of the tree: `bash device/huawei/kiwi/patches/apply-patches.sh`
3. `source build/envsetup.sh && lunch lineage_kiwi-userdebug && mka bacon`

Kernel: https://github.com/Shaheddan/android_kernel_huawei_kiwi (lineage-20-ebpf)
Vendor: https://github.com/Shaheddan/proprietary_vendor_huawei_kiwi (lineage-20)
