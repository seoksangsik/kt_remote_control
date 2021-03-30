#------------------------------------------------------------------------------
#	Makefile for KT RemoteControl
#
BUILD_ROOT = ../..

include $(BUILD_ROOT)/etc/common.mk
include $(MAKEFILE_ROOT)/module.mk

PACKAGE = remotecontrol
INCLUDE_DIRS	+= . 

TARGET_DIST := ./remotecontrol
TARGET_JS_DIR := apps/nav_webapp/apps/remotecontrol

target : export_header classes all

include $(MAKEFILE_ROOT)/java.mk
include $(MAKEFILE_ROOT)/rule.mk

web_ui : all

all :  $(TARGET_LIB_DIR) $(TARGET_OBJ_DIR) $(TARGET_OBJS) $(TARGET_LIB)

clean : clean_classes clean_resources
	@ $(RM) $(TARGET_OBJ_DIR) $(TARGET_LIB)
	@ $(RM) $(TARGET_JS_DIR)

