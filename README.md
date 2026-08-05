# Device tree for Huawei Honor 5X (kiwi) — LineageOS 19.1

Unofficial LineageOS 19.1 (Android 12L) for the Huawei Honor 5X, ported
from this device's own LineageOS 18.1 tree.

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

Boots **SELinux enforcing** with a clean denial log. Working: RIL (calls,
SMS, data), Wi-Fi including toggle/reconnect, Bluetooth with A2DP audio,
camera, audio, sensors, GPS, fingerprint (enrolment survives reboot),
MTP/ADB, lockscreen, brightness and auto-brightness.

### Known limitations

- **Rear-camera HDR** fails in the stock Camera2 app. AOSP's Camera2→HAL1
  legacy shim calls `startPreview` while the HAL is still in `PIC_TAKING`;
  the longer rear-sensor HDR capture lands inside that window. The HAL is
  fine — any Camera API1 app (e.g. Open Camera) does HDR correctly.
- Flashing over an existing `/data` may leave stale SELinux labels on
  `/data/media`, which stops photos saving. Fix once with
  `adb shell restorecon -RF /data/media`.

## Extras

- **Bypass charging** (KiwiParts): Settings → Battery → Bypass charging,
  restyled for Android 12 with a collapsing toolbar and system dark mode.
  Manual mode holds the current battery level; auto mode charges to a
  configurable threshold and holds there. Backed by the BQ24296 charger
  IC's charge-disable path (`factory_diag` sysfs node).

## Notable 18.1 → 19.1 changes

- **Kernel is no longer stock.** Android 12's keystore2 authorizes callers
  via `getCallingSid()`, which needs `FLAT_BINDER_FLAG_TXN_SECURITY_CTX` —
  absent from the stock 3.10 binder, so every unlock was denied. A modern
  binder driver was transplanted in, with `READ_ONCE`/`WRITE_ONCE` and
  `wake_up_pollfree` backported to support it.
- `CONFIG_RT_GROUP_SCHED` disabled: Android 12 dropped init's RT-bandwidth
  writes, leaving every child cgroup at `rt_runtime_us=0` and aborting
  Bluetooth at `timer_create(CLOCK_BOOTTIME)`.
- prima restores `con_mode` to STA when a SoftAP adapter is closed —
  Android 12's Wi-Fi HAL deletes the AP interface instead of switching it
  back, which left the driver stuck creating `softap.0` and never `wlan0`.
- Camera: `TARGET_HAS_LEGACY_CAMERA_HAL1` (the blob is 32-bit only), plus
  the composer registering `display.qservice` on `/dev/binder`.
- Telephony: LineageOS `simactivation` patch, required for the legacy modem
  to activate its UICC subscription apps.
- `config_biometric_sensors` overlay — Android 12 registers no fingerprint
  sensor without it — and a HAL fix for the fpc blob returning the template
  *count* from `enumerate()` rather than 0-on-success.
- LiveDisplay removed: `libmm-abl.so` needs `android::IPowerManager::
  asInterface`, which Android 12 removed when PowerManager went AIDL.
- Keymaster 4.1, clearkey 1.4, Wi-Fi overlay moved to
  `packages/modules/Wifi` (resources became a mainline module).
- SELinux policy now uses TipzTeam's `device/qcom/sepolicy-legacy`, which
  has a real `msm8916` directory and reached enforcing on this platform.
- `ro.kernel.ebpf.supported=false` is set from the product rather than a
  vendor rc: `vendor_init` may not set `default_prop`, and without it the
  critical `bpfloader` service fails and reboots the device.

## Building

Requires two platform patches beyond this tree and the kernel:

- `packages/modules/Wifi` — ignore the WPA3 transition-disable indication
  when `config_wifiSaeUpgradeEnabled` is false. Without it, a WPA2/WPA3
  router permanently disables the PSK security type on the saved network
  and the device can never reconnect (prima cannot do SAE).
- `device/qcom/sepolicy-legacy` — `vendor_display_prop` marked
  `vendor_public_prop` so apps' GL init can read it.

Build `userdebug`; the sepolicy tree only relaxes neverallows for
eng/userdebug.

## Kernel / vendor

- Kernel: [android_kernel_huawei_kiwi](https://github.com/Shaheddan/android_kernel_huawei_kiwi) (lineage-19.1, **modified** — see above)
- Vendor: TheMuppets `proprietary_vendor_huawei` with ELF-check
  exemptions and the TZ keystore blob removed

## Credits

- LineageOS team (17.1 kiwi tree, msm8916 platform work)
- [TipzTeam](https://codeberg.org/TipzTeam) — `sepolicy-legacy`,
  msm8916-common and wt88047x trees, the main 19.1 references
- niclimcy's wt88047x trees and the cyanogen/Motorola msm8916-common
  trees, used as references throughout the original 18.1 port
