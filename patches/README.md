# LineageOS 21 platform patches for kiwi (Honor 5X)

Changes kiwi needs outside the device tree: camera HALv1 support (frameworks/av,
frameworks/base), legacy RIL SIM activation (frameworks/opt/telephony), Wi-Fi
WPA3 transition handling (packages/modules/Wifi), the release ART runtime on eng
builds (build/make), Android 14 build fixes for the msm8916 HALs, core parking in
the power HAL, and the 3.10-kernel adaptations.

1. Put the files from `manifests/` into `.repo/local_manifests/` and `repo sync`.
2. From the top of the tree: `bash device/huawei/kiwi/patches/apply-patches.sh lineage-21`
3. `source build/envsetup.sh && lunch lineage_kiwi-ap2a-userdebug && mka bacon`

`patches/lineage-20/` is the older LineageOS 20 set (`apply-patches.sh lineage-20`).

Kernel: https://github.com/Shaheddan/android_kernel_huawei_kiwi (lineage-21)
Vendor: https://github.com/Shaheddan/proprietary_vendor_huawei_kiwi (lineage-21)
