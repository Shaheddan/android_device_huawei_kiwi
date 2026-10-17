#
# Copyright (C) 2014 The CyanogenMod Project
# Copyright (C) 2019 The LineageOS Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Full dexpreopt on eng too. Android 14's runtime_libart.mk sets OVERRIDE_DISABLE_DEXOPT_ALL ?= true
# for eng, which pre-opts only the ART module's boot image: with no framework boot image
# (/system/framework/arm64/boot.art) zygote64 ran imageless, the 32-bit zygote compiled a boot
# image at every start, and system_server's first-boot dexopt never finished before its watchdog
# killed it. This file is read before the products it inherits, so the ?= there keeps our value.
OVERRIDE_DISABLE_DEXOPT_ALL := false

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/product_launched_with_l.mk)

# Inherit from kiwi device
$(call inherit-product, device/huawei/kiwi/device.mk)

# Inherit some common LineageOS stuff.
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

# Ship APEXes uncompressed. A compressed APEX is decompressed to /data and must be mounted
# through dm-verity with optional table arguments, which kiwi's 3.10 dm-verity rejects
# ("verity: Invalid argument count: 10-11 arguments required"); pre-installed APEXes on
# /system are loop-mounted without dm-verity. Set in this product makefile, not device.mk:
# its own value beats every inherited one (updatable_apex.mk sets true).
PRODUCT_COMPRESSED_APEX := false

# Device identifier. This must come after all inclusions
PRODUCT_BRAND := HONOR
PRODUCT_DEVICE := kiwi
PRODUCT_MANUFACTURER := HUAWEI
PRODUCT_MODEL := KIW-L24
PRODUCT_NAME := lineage_kiwi

PRODUCT_GMS_CLIENTID_BASE := android-huawei
