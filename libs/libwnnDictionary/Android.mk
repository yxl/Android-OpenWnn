LOCAL_PATH:= $(call my-dir)

#----------------------------------------------------------------------
include $(CLEAR_VARS)

LOCAL_MODULE := libwnndict

LOCAL_MODULE_TAGS := optional

# All of the source files that we will compile.
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

LOCAL_LDLIBS := -ldl

ifeq ($(TARGET_SIMULATOR),true)
else
# for dynamic link library functions.
LOCAL_SHARED_LIBRARIES := \
	libdl
endif

# No static libraries.
LOCAL_STATIC_LIBRARIES := 

# Also need the JNI headers.
# No special include flags.
LOCAL_C_INCLUDES += \
	$(JNI_H_INCLUDE) \
	$(LOCAL_PATH)/include $(LOCAL_PATH)

# No special compiler flags.
LOCAL_CFLAGS += \
	 -O

# Don't prelink this library.  For more efficient code, you may want
# to add this library to the prelink map and set this to true. However,
# it's difficult to do this for applications that are not supplied as
# part of a system image.

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
