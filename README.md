# Device tree for Huawei Honor 5X (kiwi) — LineageOS 20

Unofficial LineageOS 20 (Android 13) for the Huawei Honor 5X, running on the
phone's original 3.10 kernel with eBPF, PSI and cgroup v2 backported so that
Android 13 can boot and run on it.

## Device specifications

| Feature | Specification                    |
| ------- | -------------------------------- |
| SoC     | Qualcomm MSM8939 Snapdragon 616  |
| CPU     | 4x1.5 GHz + 4x1.2 GHz Cortex-A53 |
| GPU     | Adreno 405                       |
| Memory  | 2/3 GB RAM                       |
| Display | 5.5" 1080x1920 IPS               |
| Storage | 16 GB (microSD support)          |
| Battery | 3000 mAh                         |
| Kernel  | 3.10 (arm64, non-Treble)         |

## Status

**Working**

- Calls (with call audio), incoming calls, SMS send and receive, mobile data
- Wi-Fi, including WPA2/WPA3 transition-mode networks
- Bluetooth, including audio playback
- Camera: photos and video recording, front and back; flashlight
- Fingerprint (enrol and unlock)
- Audio, sensors, MTP/ADB
- SELinux enforcing
- eBPF (traffic accounting), PSI-based low-memory killer

**Not yet verified:** GPS, FM radio, Wi-Fi hotspot / USB tethering,
Bluetooth call audio.

**Known limitations**

- BPF tethering offload is unavailable on the 3.10 kernel; tethering uses the
  regular (non-offloaded) forwarding path.
- The cgroup v2 freezer does not exist on 3.10, so Android's cached-app
  freezer is disabled.

## Extras

- **Bypass charging** (KiwiParts): Settings → Battery → Bypass charging.
  "Bypass now" holds the current battery level; "Auto bypass" charges to a
  configurable level and holds there. A live status card shows what the
  charger and battery are doing. Backed by the BQ24296 charger IC's
  charge-disable path (`factory_diag` sysfs node). Normal charging always
  returns when the charger is unplugged.

## Notable 19.1 → 20 changes (device tree)

- Camera: the camera provider runs as a 32-bit binderized service, and the
  HALv1 devices are served through the restored framework path (see
  platform patches). cameraserver is built 32-bit to match the HALv1 video
  metadata layout.
- Bluetooth: audio HAL entry in the manifest, profiles enabled through
  properties, APCF extended features disabled (the WCNSS firmware answers
  that command with a malformed reply), `libshim_btaddr` for
  `set_sched_policy` (moved to libprocessgroup in Android 13).
- Fingerprint HAL starts once boot has completed.
- `mm-pp-daemon` (display post-processing) runs again: an `IPowerManager`
  shim for its 5.1-era library, and the unused partial-update node is hidden
  from it to stop a busy loop.
- Performance: all cores stay online while the screen is on.
- SELinux enforcing on Android 13: eMMC queue label, vendor property
  triggers that Android 13 discards removed, CFQ scheduler set directly.
- KiwiParts redesigned in Android 13's Material You style.

## Platform patches

Android 13 needs changes outside the device tree for this phone. They are
in `patches/lineage-20/` (one folder per repository) and are applied with
`patches/apply-patches.sh`:

- **frameworks/av, frameworks/base**: camera HALv1 support (Camera1 API and
  the legacy Camera2 shim), video recording from HALv1 cameras
- **frameworks/opt/telephony**: SIM activation for the legacy Qualcomm RIL
  (`ro.telephony.ril.config=simactivation`); without it incoming calls,
  incoming SMS and call state never arrive
- **packages/modules/Wifi**: ignore WPA3 "transition disable" when the
  driver has no SAE support
- **frameworks/base (HWUI)**: fix for a 4-second UI freeze
- **system/core, system/bpf, system/netd, packages/modules/Connectivity,
  hardware/…, vendor/…**: adaptations for the 3.10 kernel and this hardware

## Building

```
repo init -u https://github.com/LineageOS/android.git -b lineage-20.0
# copy manifests/*.xml from this repository into .repo/local_manifests/
repo sync
bash device/huawei/kiwi/patches/apply-patches.sh
source build/envsetup.sh
lunch lineage_kiwi-userdebug
mka bacon
```

Flash the zip from TWRP. If you use Magisk, flash it again after every ROM
update, since the update replaces the patched boot image.

## Kernel / vendor

- Kernel: [android_kernel_huawei_kiwi](https://github.com/Shaheddan/android_kernel_huawei_kiwi)
  (branch `lineage-20-ebpf`): 3.10.108 with eBPF, PSI, cgroup v2/kernfs,
  FunctionFS AIO (adbd) and `IFA_FLAGS` netlink support backported, plus a
  fix so SELinux labels cgroup2 inodes used in migration permission checks.
- Vendor: [proprietary_vendor_huawei_kiwi](https://github.com/Shaheddan/proprietary_vendor_huawei_kiwi)
  (branch `lineage-20`).

## Credits

- LineageOS team (kiwi and msm8916 platform work)
- acroreiser, whose LeEco Le 2 (s2) 3.10 kernel with eBPF/PSI backports was
  the reference for this kernel's backports
- The HTC One A9 (hiae) Android 13 tree, used as a boot reference
- niclimcy's wt88047x trees and the cyanogen/Motorola msm8916-common trees,
  used as references in earlier versions of this port
