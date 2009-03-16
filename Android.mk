LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# LOCAL_MODULE_TAGS = optional

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := OpenWnn

LOCAL_JNI_SHARED_LIBRARIES := \
	 libWnnEngDic libWnnJpnDic libwnndict
#	 libWnnEngDic libWnnJpnDic libWnnZHCNDic libwnndict

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
