package com.kt.remotecontrol.interlock.nav.web;

import com.alticast.navsuite.android.AndroidBridge;
import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.interlock.nav.KeyHandler;

import org.havi.ui.event.HRcEvent;

public class AndroidControl implements KeyHandler {
    AndroidBridge androidBridge;

    public AndroidControl() {
        androidBridge = AndroidBridge.getInstance();
    }

    public boolean isMute() {
        return androidBridge.isMute();
    }

    public boolean volumeUp() {
        androidBridge.controlVolume(AndroidBridge.VOLUME_UP);
        return true;
    }

    public boolean volumeDown() {
        androidBridge.controlVolume(AndroidBridge.VOLUME_DOWN);
        return true;
    }

    public boolean mute() {
        androidBridge.controlVolume(AndroidBridge.VOLUME_MUTE);
        return true;
    }

    public boolean sendKey(int keyCode) {
        boolean success = false;

        switch (keyCode) {
            case HRcEvent.VK_VOLUME_DOWN:
                volumeDown();
                success = true;
                break;
            case HRcEvent.VK_VOLUME_UP:
                volumeUp();
                success = true;
                break;
            case HRcEvent.VK_MUTE:
                mute();
                success = true;
                break;
            default:
                success = ProxyManager.eventGenerator().sendKey(keyCode);
                break;
        }

        return success;
    }

    public boolean findRCU() {
        return androidBridge.findRcu(); // KTCOMM-449
    }
}
