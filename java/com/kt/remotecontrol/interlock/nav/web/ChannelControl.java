package com.kt.remotecontrol.interlock.nav.web;

import com.kt.navsuite.core.Channel;
import com.kt.navsuite.core.ChannelController;
import com.kt.navsuite.core.ChannelDatabase;
import com.kt.navsuite.core.ChannelEvent;
import com.kt.navsuite.core.ChannelEventListener;
import com.kt.navsuite.core.ChannelRing;
import com.kt.navsuite.core.kt.BrowserChannel;
import com.kt.navsuite.core.kt.KTChannelController;
import com.kt.remotecontrol.interlock.nav.ChannelHandler;
import com.kt.remotecontrol.manager.StatusManager;
import com.kt.remotecontrol.util.Log;

import org.davic.net.InvalidLocatorException;
import org.davic.net.dvb.DvbLocator;

public class ChannelControl implements ChannelHandler, ChannelEventListener {

    private static final Log LOG = new Log("ChannelControl");

    private ChannelDatabase channelDatabase;
    private ChannelController channelController;

    public ChannelControl() {
        channelDatabase = ChannelDatabase.getInstance();
        channelController = ChannelController.getInstance();
    }

    public ChannelRing getChannelRing(String ringName) {
        return channelDatabase.getChannelRing(ringName);
    }

    public Channel[] getChannels(String ringName) {
        ChannelRing channelRing = getChannelRing(ringName);
        return channelRing.getChannels();
    }

    public void setChannelRing(String ringName, Channel[] channels) {
        ChannelRing channelRing = new ChannelRing(ringName, channels);
        channelDatabase.setChannelRing(channelRing);
    }

    public void clearChannelRing(String ringName) {
        ChannelRing channelRing = new ChannelRing(ringName);
        channelDatabase.setChannelRing(channelRing);
    }

    public void selectCurrentChannel() {
        channelController.selectCurrentChannel();
    }

    public Channel getChannel(String locatorID) {
        Channel channel = null;

        try {
            channel = channelDatabase.getChannel(new DvbLocator(locatorID));
        } catch (InvalidLocatorException e) {
            e.printStackTrace();
        }
        return channel;
    }

    public Channel getCurrentChannel() {
        return channelController.getCurrentChannel();
    }

    public Channel getPromoChannel() {
        return ChannelController.getPromoChannel();
    }

    public int getServiceIDOfCurrentChannel() {
        int serviceID = -1;
        Channel channel = getCurrentChannel();

        if (channel != null) {
            serviceID = channel.getSIService().getServiceID();
        }

        return serviceID;
    }

    public boolean isSameCurrentChannel(String id) {
        Channel current = getCurrentChannel();

        try {
            return current != null && current.getSIService().getServiceID() == Integer.parseInt(id);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean isBrowserChannelByRealCurrent() {
        Channel channel = ((KTChannelController) channelController).getRealCurrentChannel();
        return channel != null && channel instanceof BrowserChannel;
    }

    public void changeChannel(Channel channel) {
        channelController.changeChannel(channel, true);
    }

    public void addChannelEventListener() {
        channelController.addListener(this);
    }

    public void removeChannelEventListener() {
        channelController.removeListener(this);
    }

    public void notifyChannelEvent(ChannelEvent event) {
        if (event == null) {
            return ;
        }

        final int eventType = event.getType();
        final Channel channel = event.getChannel();

        LOG.message("notifyChannelEvent event.getType " + eventType);

        new Thread("ChannelChangeThread") {
            public void run() {
                LOG.message("[ChannelChangeThread] start~~~ eventType=" + eventType);

                StatusManager.getInstance().processChannelEvent(eventType, channel);

                LOG.message("[ChannelChangeThread] finish~~~");
            }
        }.start();
    }
}
