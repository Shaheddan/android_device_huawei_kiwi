LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE           := libril-wrapper
LOCAL_MULTILIB         := 64
LOCAL_VENDOR_MODULE    := true
LOCAL_SRC_FILES        := ril-wrapper.c
LOCAL_SHARED_LIBRARIES := libdl liblog libril libcutils
LOCAL_CFLAGS           := -Wall -Werror
# kiwi: Android 14 builds every vendor module VNDK-style, without the old global
# include dirs (libhardware, media plugin/OMX, EGL); request them explicitly.
LOCAL_HEADER_LIBRARIES += libhardware_headers media_plugin_headers gl_headers
include $(BUILD_SHARED_LIBRARY)
