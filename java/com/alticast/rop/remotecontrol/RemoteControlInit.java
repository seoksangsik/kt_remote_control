package com.alticast.rop.remotecontrol;

import com.kt.remotecontrol.util.Log;

import com.alticast.rop.Registry;

public class RemoteControlInit {
    private static final Log LOG = new Log("RemoteControlInit");

    public static boolean init() {
        LOG.message("init, called");

        Registry.export("remotecontrol.RemoteControlInterface", new RemoteControlInterfaceImpl());

        return true;
    }
}
