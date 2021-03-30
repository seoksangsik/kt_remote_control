package com.kt.remotecontrol.interlock.nav.web;

import com.kt.remotecontrol.interlock.nav.AppHandler;
import com.kt.remotecontrol.interlock.nav.ChannelHandler;
import com.kt.remotecontrol.interlock.nav.KeyHandler;
import com.kt.remotecontrol.interlock.nav.NavHandlers;
import com.kt.remotecontrol.interlock.nav.OtherHandler;
import com.kt.remotecontrol.interlock.nav.StateHandler;

public class WebControl implements NavHandlers {
    private KeyHandler keyHandler;
    private ChannelHandler channelHandler;
    private StateHandler stateHandler;
    private AppHandler appHandler;
    private OtherHandler otherHandler;

    public WebControl(KeyHandler keyHandler) {
        this.keyHandler = keyHandler;
        channelHandler = new ChannelControl();
        stateHandler = new StateControl();
        appHandler = new WebAppControl();
        otherHandler = new OtherControl();
    }

    public KeyHandler keyHandler() {
        return keyHandler;
    }

    public ChannelHandler channelHandler() {
        return channelHandler;
    }

    public StateHandler stateHandler() {
        return stateHandler;
    }

    public AppHandler appHandler() {
        return appHandler;
    }

    public OtherHandler otherHandler() {
        return otherHandler;
    }
}
