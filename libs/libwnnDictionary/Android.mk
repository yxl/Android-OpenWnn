LOCAL_PATH:= $(call my-dir)

#----------------------------------------------------------------------
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := libwnndict

LOCAL_SRC_FILES := \
	OpenWnnDictionaryImplJni.c \
	engine/ndapi.c \
	engine/neapi.c \
	engine/ndbdic.c \
	engine/ndfdic.c \
	engine/ndldic.c \
	engine/ndrdic.c \
	engine/necode.c \
	engine/ndcommon.c \
	engine/nj_str.c

ifeq ($(TARGET_OS)-$(TARGET_SIMULATOR),linux-true)
LOCAL_LDLIBS += -ldl
endif
ifneq ($(TARGET_SIMULATOR),true)
LOCAL_SHARED_LIBRARIES += libdl
endif

LOCAL_STATIC_LIBRARIES := 

LOCAL_C_INCLUDES += \
	$(JNI_H_INCLUDE) \
	$(LOCAL_PATH)/include $(LOCAL_PATH)

LOCAL_CFLAGS += \
	 -O

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
