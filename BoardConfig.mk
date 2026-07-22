#
# Copyright (C) 2015 The CyanogenMod Project
# Copyright (C) 2017-2021 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

DEVICE_PATH := device/huawei/kiwi

TARGET_SPECIFIC_HEADER_PATH := $(DEVICE_PATH)/include

# Architecture
TARGET_ARCH := arm64
TARGET_ARCH_VARIANT := armv8-a
TARGET_CPU_ABI := arm64-v8a
TARGET_CPU_ABI2 :=
TARGET_CPU_VARIANT := cortex-a53

TARGET_2ND_ARCH := arm
TARGET_2ND_ARCH_VARIANT := armv8-a
TARGET_2ND_CPU_ABI := armeabi-v7a
TARGET_2ND_CPU_ABI2 := armeabi
TARGET_2ND_CPU_VARIANT := cortex-a53

TARGET_BOARD_SUFFIX := _64
TARGET_USES_64_BIT_BINDER := true
TARGET_BOARD_PLATFORM := msm8916

# Bootloader
TARGET_BOOTLOADER_BOARD_NAME := MSM8916
TARGET_NO_BOOTLOADER := true
TARGET_OTA_ASSERT_DEVICE := kiwi

# Kernel
BOARD_KERNEL_BASE := 0x80000000
BOARD_KERNEL_CMDLINE := androidboot.hardware=qcom ehci-hcd.park=3 androidboot.bootdevice=7824900.sdhci lpm_levels.sleep_disabled=1
BOARD_KERNEL_IMAGE_NAME := Image
BOARD_KERNEL_PAGESIZE := 2048
BOARD_KERNEL_SEPARATED_DT := true
BOARD_DTBTOOL_ARGS := -2
BOARD_KERNEL_TAGS_OFFSET := 0x01E00000
BOARD_RAMDISK_OFFSET     := 0x02000000
TARGET_KERNEL_SOURCE := kernel/huawei/kiwi
TARGET_KERNEL_ARCH := arm64
TARGET_KERNEL_HEADER_ARCH := arm64
TARGET_KERNEL_CONFIG := kiwi-64_defconfig
TARGET_KERNEL_CLANG_COMPILE := false

# Audio
AUDIO_FEATURE_ENABLED_FM_POWER_OPT := true
AUDIO_FEATURE_ENABLED_KPI_OPTIMIZE := true
AUDIO_FEATURE_ENABLED_MULTI_VOICE_SESSIONS := true
AUDIO_FEATURE_ENABLED_NEW_SAMPLE_RATE := true
AUDIO_FEATURE_ENABLED_SND_MONITOR := true
AUDIO_FEATURE_HUAWEI_SOUND_PARAM_PATH := true
BOARD_USES_ALSA_AUDIO := true
USE_CUSTOM_AUDIO_POLICY := 1
USE_XML_AUDIO_POLICY_CONF := 1

# Bluetooth
BOARD_HAVE_BLUETOOTH_QCOM := true
BOARD_BLUETOOTH_BDROID_BUILDCFG_INCLUDE_DIR := $(DEVICE_PATH)/bluetooth

# Bootanimation
TARGET_BOOTANIMATION_PRELOAD := true
TARGET_BOOTANIMATION_TEXTURE_CACHE := true

# Camera
TARGET_USES_MEDIA_EXTENSIONS := true
TARGET_NEEDS_LEGACY_CAMERA_HAL1_DYN_NATIVE_HANDLE := true
TARGET_PROCESS_SDK_VERSION_OVERRIDE := \
        /system/bin/mediaserver=23 \
        /system/vendor/bin/mm-qcamera-daemon=23

# Charger
BOARD_CHARGER_DISABLE_INIT_BLANK := true
BACKLIGHT_PATH := /sys/class/leds/lcd-backlight/brightness

# Display
OVERRIDE_RS_DRIVER := libRSDriver_adreno.so
TARGET_ADDITIONAL_GRALLOC_10_USAGE_BITS := 0x02000000
TARGET_CONTINUOUS_SPLASH_ENABLED := true
TARGET_DISABLE_POSTRENDER_CLEANUP := true
TARGET_SCREEN_DENSITY := 480
TARGET_USES_ION := true

# Extended Filesystem Support
TARGET_EXFAT_DRIVER := sdfat

# FM
BOARD_HAVE_QCOM_FM := true
TARGET_QCOM_NO_FM_FIRMWARE := true

# Fonts
EXCLUDE_SERIF_FONTS := true

# GPS
TARGET_GPS_HAL_PATH := $(DEVICE_PATH)/gps

# Hardware disk encryption (FDE)
TARGET_HW_DISK_ENCRYPTION := true
TARGET_LEGACY_HW_DISK_ENCRYPTION := true

# HIDL
DEVICE_MANIFEST_FILE := $(DEVICE_PATH)/manifest.xml


# Legacy memfd
TARGET_HAS_MEMFD_BACKPORT := true

# Lights
TARGET_PROVIDES_LIBLIGHT := true

# Malloc
MALLOC_SVELTE := true

# Partitions
TARGET_FS_CONFIG_GEN := $(DEVICE_PATH)/config.fs
TARGET_USERIMAGES_USE_EXT4 := true
TARGET_USERIMAGES_USE_F2FS := true
TARGET_USES_MKE2FS := true
BOARD_CACHEIMAGE_FILE_SYSTEM_TYPE := ext4
BOARD_PERSISTIMAGE_FILE_SYSTEM_TYPE := ext4
BOARD_ROOT_EXTRA_FOLDERS := firmware persist
# /proc/partitions * 2 * BLOCK_SIZE (512) = size in bytes
BOARD_BOOTIMAGE_PARTITION_SIZE := 67108864
BOARD_RECOVERYIMAGE_PARTITION_SIZE := 67108864
BOARD_SYSTEMIMAGE_PARTITION_SIZE := 2684354560
BOARD_USERDATAIMAGE_PARTITION_SIZE := 11618204672
BOARD_CACHEIMAGE_PARTITION_SIZE := 268435456
BOARD_PERSISTIMAGE_PARTITION_SIZE := 67108864
BOARD_FLASH_BLOCK_SIZE := 131072 # blockdev --getbsz /dev/block/mmcblk0p19

# Power
TARGET_USES_INTERACTION_BOOST := true
TARGET_TAP_TO_WAKE_NODE := /sys/touch_screen/tap_to_wake

# Properties
TARGET_SYSTEM_PROP += $(DEVICE_PATH)/system.prop

# Qualcomm support
BOARD_USES_QC_TIME_SERVICES := true
BOARD_USES_QCOM_HARDWARE := true

# Recovery
TARGET_RECOVERY_FSTAB := $(DEVICE_PATH)/rootdir/etc/fstab.qcom
TARGET_RECOVERY_PIXEL_FORMAT := ABGR_8888
TARGET_RECOVERY_DENSITY := xhdpi

# Release
TARGET_BOARD_INFO_FILE := $(DEVICE_PATH)/board-info.txt

# Release tools
TARGET_RECOVERY_UPDATER_LIBS := librecovery_updater_kiwi
TARGET_RELEASETOOLS_EXTENSIONS := $(DEVICE_PATH)/releasetools

# RIL
TARGET_RIL_VARIANT := caf
TARGET_USES_OLD_MNC_FORMAT := true

# SELinux
include device/qcom/sepolicy-legacy-um/SEPolicy.mk

# msm8916 predates the legacy-um platform filter; register the vendor policy
# dirs it would have added, using msm8937 as nearest-cousin platform dir
BOARD_VENDOR_SEPOLICY_DIRS += \
    device/qcom/sepolicy-legacy-um \
    device/qcom/sepolicy-legacy-um/legacy/vendor/common/sysmonapp \
    device/qcom/sepolicy-legacy-um/legacy/vendor/ssg \
    device/qcom/sepolicy-legacy-um/legacy/vendor/common \
    device/qcom/sepolicy-legacy-um/legacy/vendor/msm8937

# test dir holds eng/userdebug-only type definitions referenced by vendor common
ifneq (,$(filter userdebug eng, $(TARGET_BUILD_VARIANT)))
BOARD_VENDOR_SEPOLICY_DIRS += device/qcom/sepolicy-legacy-um/legacy/vendor/test
endif

BOARD_VENDOR_SEPOLICY_DIRS += \
    device/huawei/kiwi/sepolicy


# VNDK
TARGET_VNDK_USE_CORE_VARIANT := true

# Vendor Init
TARGET_INIT_VENDOR_LIB := libinit_kiwi

# Vold
TARGET_USE_CUSTOM_LUN_FILE_PATH := /sys/devices/platform/msm_hsusb/gadget/lun%d/file

# Wifi
BOARD_HAS_QCOM_WLAN := true
BOARD_HOSTAPD_DRIVER := NL80211
BOARD_HOSTAPD_PRIVATE_LIB := lib_driver_cmd_qcwcn
PRODUCT_VENDOR_MOVE_ENABLED := true
BOARD_WLAN_DEVICE := qcwcn
BOARD_WPA_SUPPLICANT_DRIVER := NL80211
BOARD_WPA_SUPPLICANT_PRIVATE_LIB := lib_driver_cmd_qcwcn
WIFI_DRIVER_FW_PATH_AP := "ap"
WIFI_DRIVER_FW_PATH_STA := "sta"
WIFI_HIDL_UNIFIED_SUPPLICANT_SERVICE_RC_ENTRY := true
WPA_SUPPLICANT_VERSION := VER_0_8_X
TARGET_DISABLE_WCNSS_CONFIG_COPY := true
TARGET_USES_QCOM_WCNSS_QMI := true
TARGET_USES_WCNSS_CTRL := true
TARGET_PROVIDES_WCNSS_QMI := true

# inherit from the proprietary version
-include vendor/huawei/kiwi/BoardConfigVendor.mk

# Build-system exceptions (each demanded by an explicit build error)
# libmmcamera_interface uses obsolete LOCAL_COPY_HEADERS (camera HAL, error 2026-07-11)
BUILD_BROKEN_USES_BUILD_COPY_HEADERS := true


# Shims (targeted; replaces the former global LD_PRELOAD of libshim_cutils in
# init.qcom.rc, which killed APEX services on 18.1 — consumers identified by
# scanning vendor blobs for UND Huawei log symbols, 2026-07-12)
TARGET_LD_SHIM_LIBS := \
    /system/vendor/bin/signinfolistener|libshim_signinfolistener.so:\
    /system/vendor/lib/libmmcamera_hdr_gb_lib.so|/system/vendor/lib/libmmqjpeg_codec.so:\
    /system/vendor/lib/libcalmodule_akm.so|libshim_cutils.so:\
    /system/vendor/lib/libchromatix_imx219_liteon_pad_zsl_preview.so|libshim_cutils.so:\
    /system/vendor/lib/libchromatix_imx219_liteon_zsl_preview.so|libshim_cutils.so:\
    /system/vendor/lib/libchromatix_imx219_ofilm_pad_zsl_preview.so|libshim_cutils.so:\
    /system/vendor/lib/libchromatix_imx219_ofilm_zsl_preview.so|libshim_cutils.so:\
    /system/vendor/lib/libchromatix_ov5648_foxconn_kivi_common.so|libshim_cutils.so:\
    /system/vendor/lib/libchromatix_ov5648_foxconn_kivi_default_video.so|libshim_cutils.so:\
    /system/vendor/lib/libchromatix_ov5648_foxconn_kivi_preview.so|libshim_cutils.so:\
    /system/vendor/lib/libchromatix_ov5648_ofilm_ohw5f03_kiw_common.so|libshim_cutils.so:\
    /system/vendor/lib/libchromatix_ov5648_ofilm_ohw5f03_kiw_default_video.so|libshim_cutils.so:\
    /system/vendor/lib/libchromatix_ov5648_ofilm_ohw5f03_kiw_preview.so|libshim_cutils.so:\
    /system/vendor/lib/libchromatix_s5k4e1_sunny_kivi_common.so|libshim_cutils.so:\
    /system/vendor/lib/libchromatix_s5k4e1_sunny_kivi_default_video.so|libshim_cutils.so:\
    /system/vendor/lib/libchromatix_s5k4e1_sunny_kivi_preview.so|libshim_cutils.so:\
    /system/vendor/lib/libchromatix_s5k4e1_sunny_kivi_snapshot.so|libshim_cutils.so:\
    /system/vendor/lib/libdmd.so|libshim_cutils.so:\
    /system/vendor/lib/libmmcamera_ov5648_foxconn.so|libshim_cutils.so:\
    /system/vendor/lib/libmmcamera_s5k4e1_sunny.so|libshim_cutils.so:\
    /system/vendor/lib64/hw/fingerprint.msm8916.so|libshim_cutils.so:\
    /system/vendor/lib64/libcalmodule_akm.so|libshim_cutils.so:\
    /system/vendor/lib64/libdmd.so|libshim_cutils.so:\
    /system/vendor/lib64/libsecure_boot_keybox.so|libshim_cutils.so:\
    /system/vendor/lib64/sensors.kiwi.so|libshim_cutils.so

# 19.1: vendor blobs shipped via PRODUCT_COPY_FILES trip new ELF checks (ref: Moto 19.1)
BUILD_BROKEN_ELF_PREBUILT_PRODUCT_COPY_FILES := true
