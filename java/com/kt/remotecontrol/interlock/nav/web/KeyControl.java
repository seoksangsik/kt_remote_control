package com.kt.remotecontrol.interlock.nav.web;

import com.alticast.ktip.BTRCUManager;
import com.kt.navsuite.core.VolumeController;
import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.interlock.nav.KeyHandler;

import org.havi.ui.event.HRcEvent;

public class KeyControl implements KeyHandler {
    private VolumeController instance;

    public KeyControl() {
        instance = VolumeController.getInstance();
    }

    public boolean isMute() {
        return instance.isMute();
    }

    public boolean volumeUp() {
        return sendKey(HRcEvent.VK_VOLUME_UP);
    }

    public boolean volumeDown() {
        return sendKey(HRcEvent.VK_VOLUME_DOWN);
    }

    public boolean mute() {
        return sendKey(HRcEvent.VK_MUTE);
    }

    public boolean sendKey(int keyCode) {
        return ProxyManager.eventGenerator().sendKey(keyCode);
    }

    public boolean findRCU() {
        return BTRCUManager.getInstance().findBTRCU(); // KTUHDII-476
    }
}
