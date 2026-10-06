LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := sensors.$(TARGET_BOARD_PLATFORM)

LOCAL_MODULE_RELATIVE_PATH := hw
LOCAL_PROPRIETARY_MODULE := true

LOCAL_CFLAGS := -DLOG_TAG=\"MultiHal\"

LOCAL_SRC_FILES := \
    multihal.cpp \
    SensorEventQueue.cpp \

LOCAL_SHARED_LIBRARIES := \
    libcutils \
    libdl \
    liblog \
    libutils \

LOCAL_STRIP_MODULE := false

# kiwi: Android 14 builds every vendor module VNDK-style, without the old global
# include dirs (libhardware, media plugin/OMX, EGL); request them explicitly.
LOCAL_HEADER_LIBRARIES += libhardware_headers media_plugin_headers gl_headers
include $(BUILD_SHARED_LIBRARY)
