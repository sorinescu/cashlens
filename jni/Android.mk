LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := CashLens

LOCAL_SRC_FILES := CashLens.c

# only export CashLens.c symbols
LOCAL_CFLAGS := -fvisibility=hidden -DAPIENTRY='__attribute__((visibility("default")))'

### libyuv

# for the armeabi-v7a ABI, enable NEON SIMD support
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_CFLAGS += -D__ARM_NEON__
    LOCAL_ARM_NEON := true
endif

LOCAL_CFLAGS += -I$(LOCAL_PATH)/libyuv
LOCAL_SRC_FILES += libyuv/cpu_id.cpp libyuv/planar_functions.cpp libyuv/rotate_neon.cpp libyuv/row_common.cpp libyuv/row_neon.cpp libyuv/scale.cpp

include $(BUILD_SHARED_LIBRARY)
