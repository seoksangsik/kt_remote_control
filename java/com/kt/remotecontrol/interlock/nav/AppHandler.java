package com.kt.remotecontrol.interlock.nav;

import com.alticast.remotecontrol.RemoteControlEvent;

import java.util.HashMap;
import java.util.Properties;

public interface AppHandler {
    public final String ID_OBSERVER = "3000";
    public final String ID_HOME_PORTAL = "3001";
    public final String ID_QRATOR = "3009";
    public final String ID_KIDSCARE = "3013";

    public void setRemoteControlEvent(RemoteControlEvent event);

    public void talkToApp(HashMap params, String aid);
    public boolean sendKeyword(String keyword);
    public void notifyKeyword(boolean result);

    public String buyContents(Properties params);
    public void notifyRemotePurchase(String result);

    public HashMap execute(HashMap params);
    public void notifyExecuteResult(HashMap response);

    public int getHomeState();
    public void setHomeState(int state);
}
