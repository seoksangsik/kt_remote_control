package com.kt.remotecontrol.interlock;

import com.kt.navsuite.core.Channel;
import com.kt.remotecontrol.http.URLConnectionProxy;
import com.kt.remotecontrol.interlock.spec.SIRetrieval;
import com.kt.remotecontrol.interlock.mw.EventGenerator;
import com.kt.remotecontrol.interlock.nav.AppHandler;
import com.kt.remotecontrol.interlock.nav.ChannelHandler;
import com.kt.remotecontrol.interlock.nav.KeyHandler;
import com.kt.remotecontrol.interlock.nav.Navigator;
import com.kt.remotecontrol.interlock.nav.OtherHandler;
import com.kt.remotecontrol.interlock.nav.StateHandler;

public class ProxyManager {
    private static ProxyManager instance = new ProxyManager();
    private static Navigator navigator;
    private static EventGenerator eventGenerator;
    private static URLConnectionProxy urlConnectionProxy;
    private static SIRetrieval siRetrieval;

    private ProxyManager() {
    }

    public static ProxyManager getInstance() {
        return instance;
    }

    public static Navigator navigator() {
        return navigator;
    }

    public static void setNavigator(Navigator nav) {
        navigator = nav;
    }

    public static EventGenerator eventGenerator() {
        return eventGenerator;
    }

    public static void setEventGenerator(EventGenerator evtGenerator) {
        eventGenerator = evtGenerator;
    }

    public static String getCurrentProgramName(Channel channel) {
        return siRetrieval.getCurrentProgramName(channel);
    }

    public static void setSIRetrieval(SIRetrieval aSIRetrieval) {
        siRetrieval = aSIRetrieval;
    }

    public static void setURLConnection(URLConnectionProxy urlConnection) {
        urlConnectionProxy = urlConnection;
    }

    public static URLConnectionProxy urlConnection() {
        return urlConnectionProxy;
    }

    public static KeyHandler keyHandler() {
        return navigator.keyHandler();
    }

    public static ChannelHandler channelHandler() {
        return navigator.channelHandler();
    }

    public static StateHandler stateHandler() {
        return navigator.stateHandler();
    }

    public static AppHandler appHandler() {
        return navigator.appHandler();
    }

    public static OtherHandler otherHandler() {
        return navigator.otherHandler();
    }
}
