ifneq ($(QCPATH),)
ifneq ($(BUILD_TINY_ANDROID),true)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libloc_ds_api

LOCAL_MODULE_TAGS := optional

LOCAL_SHARED_LIBRARIES := \
    libutils \
    libcutils \
    libqmi_cci \
    libqmi_common_so \
    libgps.utils \
    libdsi_netctrl \
    libqmiservices

LOCAL_SRC_FILES += \
    ds_client.c

LOCAL_CFLAGS += \
    -fno-short-enums \
    -D_ANDROID_

LOCAL_COPY_HEADERS_TO:= libloc_ds_api/

LOCAL_COPY_HEADERS:= \
    ds_client.h

LOCAL_LDFLAGS += -Wl,--export-dynamic

## Includes
LOCAL_C_INCLUDES := \
    $(TARGET_OUT_HEADERS)/libloc_eng \
    $(TARGET_OUT_HEADERS)/qmi-framework/inc \
    $(TARGET_OUT_HEADERS)/qmi/inc \
    $(TARGET_OUT_HEADERS)/gps.utils \
    $(TARGET_OUT_HEADERS)/data/inc


LOCAL_PRELINK_MODULE := false

# kiwi: Android 14 builds every vendor module VNDK-style, without the old global
# include dirs (libhardware, media plugin/OMX, EGL); request them explicitly.
LOCAL_HEADER_LIBRARIES += libhardware_headers media_plugin_headers gl_headers
include $(BUILD_SHARED_LIBRARY)

endif # not BUILD_TINY_ANDROID
endif # QCPATH
