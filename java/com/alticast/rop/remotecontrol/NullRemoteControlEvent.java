package com.alticast.rop.remotecontrol;

import com.alticast.remotecontrol.RemoteControlEvent;
import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.util.Log;

import java.util.Map;

public class NullRemoteControlEvent implements RemoteControlEvent {

    private static final Log LOG = new Log("NullRemoteControlEvent");

    public NullRemoteControlEvent() {
        LOG.message("constructor");
    }

    public void postMessage(Map args, String aid) {
        LOG.message("postMessage");
    }

    public void sendKeyword(String keyword) {
        LOG.message("sendKeyword, keyword=" + keyword);
        ProxyManager.appHandler().notifyKeyword(false);
    }

    public void buyContents(Map params) {
        LOG.message("buyContents");
    }

    public void execute(Map request) {
        LOG.message("execute");
    }
}
