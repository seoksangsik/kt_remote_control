package com.kt.remotecontrol.interlock.nav;

public interface NavHandlers {
    public ChannelHandler channelHandler();
    public KeyHandler keyHandler();
    public StateHandler stateHandler();
    public AppHandler appHandler();
    public OtherHandler otherHandler();
}
