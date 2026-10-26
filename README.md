# Device tree for Huawei Honor 5X (kiwi) — LineageOS 21

Unofficial LineageOS 21 (Android 14) for the Huawei Honor 5X, running on the
phone's original 3.10 kernel. eBPF, PSI and cgroup v2 are backported into that
kernel, along with the few extra system calls and fixes Android 14 needs, so
Android 14 can boot and run on it. The ROM is a userdebug build and runs with
SELinux enforcing.

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

- Calls, SMS send and receive, mobile data
- Wi-Fi
- Bluetooth, including audio playback
- Camera
- Fingerprint (enrol and unlock)
- Sensors, including auto-rotation
- Battery level, charging and bypass charging
- MTP, ADB and `adb root`
- SELinux enforcing
- eBPF (traffic accounting), PSI-based low-memory killer

**Not yet verified:** video recording, flashlight, GPS, FM radio, Wi-Fi
hotspot / USB tethering, Bluetooth call audio.

**Known limitations**

- Wi-Fi is 2.4 GHz only.
- The phone cannot join a Google Pixel 7's hotspot; other phones' hotspots
  (for example a Galaxy A30's) work.
- The compass may need recalibrating after a reboot.
- The cgroup v2 freezer does not exist on 3.10, so Android's cached-app
  freezer is disabled.

## Extras

- **Bypass charging** (KiwiParts): Settings → Battery → Bypass charging.
  "Bypass now" holds the current battery level; "Auto bypass" charges to a
  configurable level and holds there. Redesigned for Android 14 in the
  Material You style, with an animated battery that shows whether the phone is
  charging, discharging, full or in bypass. Backed by the BQ24296 charger IC's
  charge-disable path (`factory_diag` sysfs node). Normal charging always
  returns when the charger is unplugged.
- **Smoothness**: all cores stay online while the screen is on and are parked
  again when it turns off (power HAL), and the eMMC uses the CFQ I/O
  scheduler.

## Notable 20 → 21 changes (device tree)

- Boot on Android 14 with no VNDK APEX and no separate vendor partition:
  `ro.vndk.version` is no longer set (linkerconfig), the EGL driver is named
  (`ro.hardware.egl=adreno`) and APEXes ship uncompressed (the 3.10
  dm-verity cannot mount decompressed APEXes).
- SurfaceFlinger: shader-cache priming is off (Adreno 405 cannot create its
  FP16 texture), and the gralloc and memalloc libraries are labelled as
  same-process HALs so every process can map graphics buffers.
- Android 14 build rules: vendor link types (the camera HAL builds its own
  CameraParameters; hwcomposer and the video encoder no longer link
  platform-only libraries), AIDL ClearKey, the basic USB HAL, no Trust HAL,
  target FCM level 5, and a device framework matrix for the legacy HIDL HALs.
- Telephony: Android 14 accepts only IRadio 1.4 or newer, so LineageOS's
  `radio@1.4-service.legacy` wrapper serves IRadio 1.4 in front of the
  phone's rild.
- Wi-Fi: AOSP's AIDL Wi-Fi HAL loads the built-in pronto driver, and
  wpa_supplicant no longer offers management frame protection (`pmf=0`),
  which made the prima driver reject every connection.
- Bluetooth: zygote no longer preloads the 2015 Adreno driver, which clobbers
  register x18, the shadow call stack pointer Android 14's Bluetooth uses.
- Sensors: the sensors HAL runs as a binderized service, since Android 14
  cannot load it into system_server any more.
- Runtime: full dexpreopt and the release ART runtime on every build variant.
- Battery: the fuel-gauge daemon (`vm_bms`) starts as soon as the filesystems
  are mounted, so the battery percentage is right after the first boot.
- SELinux enforcing on Android 14, with no device domains left permissive.
- KiwiParts redesigned for Android 14.

## Platform patches

Android 14 needs changes outside the device tree for this phone. They are in
`patches/lineage-21/` (one folder per repository) and are applied with
`patches/apply-patches.sh lineage-21`:

- **frameworks/av, frameworks/base**: camera HALv1 support (Camera1 API and
  the legacy Camera2 shim), ported to Android 14
- **frameworks/opt/telephony**: SIM activation for the legacy Qualcomm RIL
  (`ro.telephony.ril.config=simactivation`); without it incoming calls,
  incoming SMS and call state never arrive
- **packages/modules/Wifi**: ignore WPA3 "transition disable" when the
  driver has no SAE support
- **build/make**: lets a device ship the release ART runtime on eng builds
- **hardware/qcom-caf/msm8916 (audio, display, media), hardware/qcom-caf/common,
  hardware/lineage/interfaces, hardware/interfaces**: Android 14 build fixes
  for the msm8916 HALs and the legacy camera HAL
- **vendor/qcom/opensource/power**: msm8916 support and core parking on
  screen off
- **system/core, system/bpf, external/icu, vendor/lineage,
  device/lineage/sepolicy**: adaptations for the 3.10 kernel and this
  hardware

## Building

Locally:

```
repo init -u https://github.com/LineageOS/android.git -b lineage-21.0 --git-lfs -g default,-pixel-extra
# copy manifests/*.xml from this repository into .repo/local_manifests/
repo sync
bash device/huawei/kiwi/patches/apply-patches.sh lineage-21
source build/envsetup.sh
lunch lineage_kiwi-ap2a-userdebug
mka bacon
```

On [Crave](https://foss.crave.io), from a workspace of its LineageOS 22.1
project (the script switches it to LineageOS 21, applies the patches and
builds):

```
crave run --no-patch -- "curl -sfL https://raw.githubusercontent.com/Shaheddan/android_device_huawei_kiwi/lineage-21/crave/build-kiwi.sh | VARIANT=userdebug bash"
```

With `GH_TOKEN` set, the script also uploads the zip to a release of this
repository (`crave/upload-kiwi.sh`).

## Installing

Download the zip from this repository's
[Releases](https://github.com/Shaheddan/android_device_huawei_kiwi/releases)
and flash it from TWRP. Coming from another ROM or another Android version,
wipe data before flashing. If you use Magisk, flash it again after every ROM
update, since the update replaces the patched boot image.

## Kernel / vendor

- Kernel: [android_kernel_huawei_kiwi](https://github.com/Shaheddan/android_kernel_huawei_kiwi)
  (branch `lineage-21`): 3.10.108 with eBPF, PSI, cgroup v2/kernfs,
  FunctionFS AIO (adbd) and `IFA_FLAGS` netlink support backported, plus for
  Android 14: `renameat2` (with `RENAME_NOREPLACE`), per-file SELinux labels
  on bpffs, and a FunctionFS AIO cancel fix that makes `adb root` reconnect.
- Vendor: [proprietary_vendor_huawei_kiwi](https://github.com/Shaheddan/proprietary_vendor_huawei_kiwi)
  (branch `lineage-21`).
- SELinux: [android_device_qcom_sepolicy-legacy](https://github.com/Shaheddan/android_device_qcom_sepolicy-legacy)
  (branch `lineage-21`).

## Credits

- LineageOS team (kiwi and msm8916 platform work)
- acroreiser, whose LeEco Le 2 (s2) 3.10 kernel and LineageOS 21 device tree
  were the main reference for this kernel's backports and for Android 14
- The HTC One A9 (hiae) tree, used as a boot reference
- TipzTeam, for the msm8916 sepolicy-legacy tree this port's SELinux policy
  is built on
- niclimcy's wt88047x trees and the cyanogen/Motorola msm8916-common trees,
  used as references in earlier versions of this port
