# Device tree for Huawei Honor 5X (kiwi) — LineageOS 18.1

Unofficial LineageOS 18.1 (Android 11) bringup for the Huawei Honor 5X,
ported from the official LineageOS 17.1 device tree.

## Device specifications

| Feature  | Specification                     |
| :------- | :-------------------------------- |
| SoC      | Qualcomm MSM8939 Snapdragon 616   |
| CPU      | 4x1.5 GHz + 4x1.2 GHz Cortex-A53  |
| GPU      | Adreno 405                        |
| Memory   | 2/3 GB RAM                        |
| Display  | 5.5" 1080x1920 IPS                |
| Storage  | 16 GB (microSD support)           |
| Battery  | 3000 mAh                          |
| Kernel   | 3.10 (arm64, non-Treble)          |

## Status

Everything works: RIL (calls, SMS, data), Wi-Fi, Bluetooth, camera
(photo + video), audio, sensors, GPS, fingerprint, MTP/ADB, SELinux
enforcing.

## Extras

- **Bypass charging** (KiwiParts): Settings → Battery → Bypass charging.
  Manual mode holds the current battery level; auto mode charges to a
  configurable threshold and holds there. Backed by the BQ24296 charger
  IC's charge-disable path (`factory_diag` sysfs node).

## Notable 17.1 → 18.1 changes

- Audio HAL @6.0 packages, binderized composer, health @2.1, AIDL Power
  HAL, software gatekeeper→hardware gatekeeper (required by the fpc
  fingerprint TZ app), software keymaster (Huawei's TZ keymaster applet
  cannot serve Android 11 crypto)
- Global `LD_PRELOAD libshim_cutils.so` removed from init.qcom.rc —
  it breaks APEX services (adbd, mediaswcodec, statsd) on Android 11 —
  replaced with targeted `TARGET_LD_SHIM_LIBS` entries for the vendor
  blobs that actually import Huawei's custom liblog symbols
- `config_wifiSaeUpgradeEnabled=false` (prima/WCNSS cannot do WPA3 SAE)
- Fingerprint HAL: send empty enumerate callback (Android 11
  FingerprintService requirement)
- Full sepolicy for enforcing boot on the 18.1 HAL set

## Kernel / vendor

- Kernel: LineageOS `android_kernel_huawei_kiwi` (lineage-17.1, unmodified)
- Vendor: TheMuppets `proprietary_vendor_huawei` (lineage-17.1) with
  ELF-check exemptions and the TZ keystore blob removed

## Credits

- LineageOS team (17.1 kiwi tree, msm8916 platform work)
- niclimcy's wt88047x 18.1 trees and the cyanogen/Motorola
  msm8916-common trees, used as references throughout the port
