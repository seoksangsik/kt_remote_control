package com.kt.remotecontrol.interlock.nav;

import com.kt.navsuite.core.Channel;
import com.kt.navsuite.core.ChannelRing;

public interface ChannelHandler {

    public Channel getChannel(String locatorID);
    public Channel getCurrentChannel();
    public Channel getPromoChannel();
    public Channel[] getChannels(String ringName);

    public ChannelRing getChannelRing(String ringName);
    public void setChannelRing(String ringName, Channel[] channels);
    public void clearChannelRing(String ringName);

    public void selectCurrentChannel();
    public void changeChannel(Channel channel);

    public void addChannelEventListener();
    public void removeChannelEventListener();

    public int getServiceIDOfCurrentChannel();
    public boolean isSameCurrentChannel(String id);
    public boolean isBrowserChannelByRealCurrent();
}
