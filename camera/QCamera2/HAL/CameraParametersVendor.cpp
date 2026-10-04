/*
 * kiwi: vendor copy of android::CameraParameters for the HALv1 camera HAL.
 *
 * WHY: Android 14 enforces vendor link types for every vendor module, and
 * libcamera_client is platform-only, so camera.msm8916 may no longer link it.
 * The HAL only uses CameraParameters internally (HAL1 passes parameters as
 * flattened strings), so compiling the framework's own source in keeps the
 * behaviour identical and stays in sync with frameworks/av automatically.
 */
#include "../../../../../../frameworks/av/camera/CameraParameters.cpp"
