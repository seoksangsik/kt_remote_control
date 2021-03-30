package com.kt.remotecontrol.interlock.mw;

import com.alticast.event.InputEventGenerator;
import com.alticast.mouse.MouseEventGenerator;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.util.TimeConstant;

public class EventGenerator {

    private static final Log LOG = new Log("EventGenerator");

    private InputEventGenerator inputEventGenerator;
    private MouseEventGenerator mouseEventGenerator;

    public EventGenerator() {

        inputEventGenerator = InputEventGenerator.getInstance();
        mouseEventGenerator = MouseEventGenerator.getInstance();
    }

    public boolean sendKey(int code) {
        LOG.message("sendKey, code=" + code);

        switch (code) {
            case 115 : // 이전키, ACAP에서 HRcEvent.VK_F4, 115
                code = 608; // VK_BACK
                break;
            case 8 : // 지우기
                code = 127; // VK_DELETE
                break;
        }

        return sendKeyPair(code);
    }

    public boolean sendKeyPair(int keyCode) {
        boolean success = pressedKey(keyCode);

        if (success) {
            try {
                Thread.sleep(TimeConstant.ZERO_POINT_ONE_SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            success = releasedKey(keyCode);
        }

        return success;
    }

    public boolean pressedKey(int keyCode) {
        boolean result = sendKeyEvent(keyCode, InputEventGenerator.KEY_EVENT_TYPE_PRESSED);
        LOG.message("pressedKey, keyCode=" + keyCode + ", result=" + result);

        return result;
    }

    public boolean releasedKey(int keyCode) {
        boolean result = sendKeyEvent(keyCode, InputEventGenerator.KEY_EVENT_TYPE_RELEASED);
        LOG.message("releasedKey, keyCode=" + keyCode + ", result=" + result);

        return result;
    }

    public boolean appendStringToIME(String keyword) {
        LOG.message("appendStringToIME, keyword=" + keyword);

        boolean result = inputEventGenerator.appendStringToIME(keyword);
        LOG.message("appendStringToIME, result=" + result);

        return result;
    }

    public boolean isSupportMouse() {
        boolean found = false;
        String className = "com.alticast.mouse.MouseEventGenerator";

        try {
            Class clazz = Class.forName(className);
            found = true;
        } catch (ClassNotFoundException e) {
            LOG.message("isSupportMouse, NOT support mouse, " + className
                        + " class is NOT found");
        }

        LOG.message("isSupportMouse, found=" + found);

        return found;
    }

    public void enableVirtualMouseDevice() {
        mouseEventGenerator.enableVirtualMouseDevice();
    }

    public void disableVirtualMouseDevice() {
        mouseEventGenerator.disableVirtualMouseDevice();
    }

    public boolean pressedMouse() {
        boolean result = sendMouseEvent(0, 0, MouseEventGenerator.MOUSE_EVENT_CODE_LEFT, MouseEventGenerator.MOUSE_EVENT_TYPE_PRESSED);
        LOG.message("pressedMouse, result=" + result);
        return result;
    }

    public boolean releasedMouse() {
        boolean result = sendMouseEvent(0, 0, MouseEventGenerator.MOUSE_EVENT_CODE_LEFT, MouseEventGenerator.MOUSE_EVENT_TYPE_RELEASED);
        LOG.message("releasedMouse, result=" + result);
        return result;
    }

    public boolean sendMouse(int x, int y) {
        boolean result = sendMouseEvent(x, y, MouseEventGenerator.MOUSE_EVENT_CODE_NONE, MouseEventGenerator.MOUSE_EVENT_TYPE_MOVED);
        LOG.message("sendMouse, result=" + result);
        return result;
    }

    private boolean sendKeyEvent(int keyCode, int keyType) {
        return inputEventGenerator.sendKeyEvent(keyCode, keyType, 0, 0, InputEventGenerator.EVENT_SOURCE_RC);
    }

    private boolean sendMouseEvent(int x, int y, int eventCode, int mouseType) {
        return mouseEventGenerator.sendMouseEvent(eventCode,
                mouseType, x, y, 0/*z*/, 0/*angle*/, 0/*forceX*/, 0/*forceY*/, 0/*forceZ*/, 0/*rotationX*/, 0/*rotationY*/);
    }
}
