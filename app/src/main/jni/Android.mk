LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LDLIBS    := -llog
LOCAL_MODULE    := planets_swiss
LOCAL_SRC_FILES := planets_swiss.c \
                   swiss/swedate.c \
                   swiss/sweph.c \
                   swiss/swephlib.c \
                   swiss/swejpl.c \
                   swiss/swemplan.c \
                   swiss/swemmoon.c \
                   swiss/swecl.c \
                   swiss/swehouse.c

include $(BUILD_SHARED_LIBRARY)
