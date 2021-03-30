package com.kt.remotecontrol.service.mouse;

import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.interlock.ProxyManager;

public class OTGController {

    private static final Log LOG = new Log("OTGController");

    private final int MOUSE_PRESSED = 0;
    private final int MOUSE_RELEASED = 1;
    private final int MOUSE_MOVED = 2;

    private static OTGController instance;

    private boolean isSupportMouse;
    private boolean enabledMouseDevice;

    private OTGController() {
        isSupportMouse = ProxyManager.eventGenerator().isSupportMouse();
    }

    public static OTGController getInstance() {
        if (instance == null) {
            instance = new OTGController();
        }

        return instance;
    }

    public void enableMouseDevice() {
        if (isNotSupportMouse()) {
            LOG.message("enableMouseDevice, NOT mouse support, ignore");
            return;
        }

        LOG.message("enableMouseDevice");

        if (isDisabledMouseDevice()) {
            ProxyManager.eventGenerator().enableVirtualMouseDevice();
            enabledMouseDevice = true;
        }
    }

    public void disableMouseDevice() {
        if (isNotSupportMouse()) {
            LOG.message("disableMouseDevice, NOT mouse support, ignore");
            return;
        }

        if (isDisabledMouseDevice()) {
            return;
        }

        LOG.message("disableMouseDevice");

        ProxyManager.eventGenerator().disableVirtualMouseDevice();
        enabledMouseDevice = false;
    }

    public boolean sendPointMessage(String actionType, String positionX, String positionY) {

        if (isNotSupportMouse()) {
            LOG.message("sendPointMessage, NOT mouse support, skip");
            return false;
        }

        LOG.message("sendPointMessage, actionType=" + actionType
                    + ", position(x=" + positionX + ", y=" + positionY + ")");

        int action = -1;
        try {
            action = Integer.parseInt(actionType);
        } catch(NumberFormatException e) {
            e.printStackTrace();
        }

        int x = 0, y = 0;

        try {
            x = Integer.parseInt(positionX);
            y = Integer.parseInt(positionY);
        } catch(NumberFormatException e) {
            e.printStackTrace();
        }

        boolean result = false;

        switch(action) {
        case MOUSE_PRESSED :
            result = ProxyManager.eventGenerator().pressedMouse();
            break;
        case MOUSE_RELEASED :
            result = ProxyManager.eventGenerator().releasedMouse();
            break;
        case MOUSE_MOVED :
            int deltaX = 0, deltaY = 0;

            x += deltaX;
            y += deltaY;

            LOG.message("sendPointMessage, mouseMoved, x=" + x + ", y=" + y);

            result = ProxyManager.eventGenerator().sendMouse(x, y);
            break;
        default :
            break;
        }

        return result;
    }

    public boolean isNotSupportMouse() {
        return !isSupportMouse;
    }

    public boolean isDisabledMouseDevice() {
        return !enabledMouseDevice;
    }
}
