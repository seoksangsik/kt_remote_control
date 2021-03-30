package com.kt.remotecontrol;

import com.kt.bridge.RemoteControl;
import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.interlock.nav.AppHandler;
import com.kt.remotecontrol.manager.StatusManager;
import com.kt.remotecontrol.util.Log;

// com.kt.bridge.RemoteServiceBridge(Navigator)->
public class EntryPoint implements RemoteControl {

    private static final Log LOG = new Log("EntryPoint");

    // HostAppManager(Navigator)->
    public void startService() {
        LOG.message("startService");

        RemoteControlAgent.getInstance().startService();
    }

    public void stopService() {
        LOG.message("stopService");

        RemoteControlAgent.getInstance().stopService();
    }

    // RNVODStateImpl(Navigator)->
    public void setVodState(boolean play, String vodID, String vodName) {
        LOG.message("setVodState");

        StatusManager.getInstance().changeVODStatus(play, vodID, vodName);
    }

    // RNVODStateImpl(Navigator)->
    public void setVodPlayRate(float vodPlayRate) {
        LOG.message("setVodPlayRate");

        StatusManager.getInstance().setVODPlayRate(vodPlayRate);
    }

    // RExternalCollectionImpl(Navigator)->
    public void resultLaunchBrowser(int result) {
        LOG.message("resultLaunchBrowser");

        ProxyManager.otherHandler().notifyLaunchBrowser(result);
    }

    // RChannelLegacyImpl(Navigator)->
    public void setUnboundAppState(String aid, int state) {
        LOG.message("setUnboundAppState, aid=" + aid);

        if (!AppHandler.ID_HOME_PORTAL.equals(aid)) {
            return ;
        }

        ProxyManager.appHandler().setHomeState(state);
    }
}
