LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

OpenCV_INSTALL_MODULES:=on
OPENCV_CAMERA_MODULES:=off
OPENCV_LIB_TYPE:=STATIC
ifeq ("$(wildcard $(OPENCV_MK_PATH))","")
include E:\work\finish28\OpenCV-3.1.0-android-sdk\OpenCV-android-sdk\sdk\native\jni\OpenCV.mk
else
include $(OPENCV_MK_PATH)
endif

LOCAL_MODULE := ImgFun
LOCAL_SRC_FILES := ImgFun.cpp
LOCAL_LDLIBS += -lm -llog
include $(BUILD_SHARED_LIBRARY)