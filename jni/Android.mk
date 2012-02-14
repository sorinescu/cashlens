LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := CashLens
### Add all source file names to be included in lib separated by a whitespace
LOCAL_SRC_FILES := CashLens.cpp yuv2rgb/yuv2bgr16tab.c yuv2rgb/nv21rgb565.s

include $(BUILD_SHARED_LIBRARY)
