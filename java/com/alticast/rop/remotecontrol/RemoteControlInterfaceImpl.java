package com.alticast.rop.remotecontrol;

import com.alticast.remotecontrol.RemoteControlInterface;
import com.alticast.remotecontrol.RemoteControlInterfaceSkel;
import com.alticast.remotecontrol.RemoteControlEvent;
import com.alticast.rop.Exportable;
import com.alticast.rop.Skeleton;
import com.alticast.rop.Stub;
import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.util.Log;

import java.util.HashMap;
import java.util.Map;

public class RemoteControlInterfaceImpl implements Exportable, RemoteControlInterface {

    private static final Log LOG = new Log("RemoteControlInterfaceImpl");

    private static RemoteControlInterfaceImpl instance;

    public static RemoteControlInterfaceImpl getInstance() {
        return instance;
    }

    public RemoteControlInterfaceImpl() {
        if (Log.INCLUDE) {
            LOG.message("constructor");
        }
        instance = this;
    }

    /* implementation of Exportable */
    public Skeleton createSkeleton() {
        return new RemoteControlInterfaceSkel(this);
    }

    // implementation RemoteControlInterface ------------------------------------------------
    public void setRemoteControlEvent(RemoteControlEvent event) {
        if (event instanceof Stub) {
            LOG.message("setRemoteControlEvent: id=" + ((Stub) event).remote.id);

            ProxyManager.appHandler().setRemoteControlEvent(event);
        }
    }

    public void notifyKeyword(boolean result) {
        LOG.message("notifyKeyword, result=" + result);

        ProxyManager.appHandler().notifyKeyword(result);
    }

    public void notifyRemotePurchase(String result) {
        LOG.message("notifyRemotePurchase, result=" + result);

        ProxyManager.appHandler().notifyRemotePurchase(result);
    }

    public void notifyExecuteResult(Map response) {
        LOG.message("notifyExecuteResult, response=" + response);

        ProxyManager.appHandler().notifyExecuteResult((HashMap) response);
    }
}
