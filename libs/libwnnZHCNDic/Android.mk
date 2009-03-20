LOCAL_PATH:= $(call my-dir)

#----------------------------------------------------------------------
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE := libWnnZHCNDic

# All of the source files that we will compile.
LOCAL_SRC_FILES := \
	WnnZHCNDic.c

# No shared libraries.
LOCAL_SHARED_LIBRARIES := 

# No static libraries.
LOCAL_STATIC_LIBRARIES :=

# Also need the JNI headers.
# No special include flags.
LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/../libwnnDictionary/include

# No special compiler flags.
LOCAL_CFLAGS += \
	-O

# Don't prelink this library.  For more efficient code, you may want
# to add this library to the prelink map and set this to true. However,
# it's difficult to do this for applications that are not supplied as
# part of a system image.

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
