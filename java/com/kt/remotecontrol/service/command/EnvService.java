package com.kt.remotecontrol.service.command;

import com.kt.navsuite.core.Channel;
import com.kt.navsuite.core.STBStateManager;
import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.service.Service;
import com.kt.remotecontrol.util.*;

import java.util.Properties;

public class EnvService extends CommandService implements Service {

    private static final Log LOG = new Log("EnvcService");

    public EnvService() {
        super();

        publishCommand.put("ENV1001", "setupSTBEnv");
    }

    public String execute(Properties params) {
        String cmd = params.getProperty(Constants.CMD);

        if (!publishCommand.containsKey(cmd)) {
            return ErrorCode.INVALID_COMMAND;
        }

        String methodName = (String) publishCommand.get(cmd);
        return execute(this, methodName, params);
    }

    protected String setupSTBEnv(Properties params) {

        LOG.message("setupSTBEnv(ENV1001)");

        setLimitedTime(params);

        setSTBState(params);

        setCurrentChannel(params);

        setLimitedChannel(params);
        setFavoriteChannel(params);
        setHiddenChannel(params);

        return "000";
    }

    private boolean setLimitedTime(Properties params) {
        String limitedTime = params.getProperty("LIMITED_TIME");
        ProxyManager.otherHandler().setLimitedWatchingTime(limitedTime);
        return true;
    }

    private boolean setSTBState(Properties params) {
        String stbState = params.getProperty("STB_STATE");

        if ("RUNNING".equalsIgnoreCase(stbState)) {
            STBStateManager.getInstance().setState(768);
            return true;
        } else if ("STANDBY".equalsIgnoreCase(stbState)) {
            STBStateManager.getInstance().setState(512);
            return true;
        }

        return false;
    }

    private boolean setCurrentChannel(Properties params) {
        String value = params.getProperty("CH_CURRENT");

        if (hasNoValue(value)) {
            return false;
        }

        Channel channel = ProxyManager.channelHandler().getChannel(value);
        if (channel == null) {
            return false;
        }

        ProxyManager.channelHandler().changeChannel(channel);
        return  true;
    }

    private void setLimitedChannel(Properties params) {
        String value = params.getProperty("CH_LIMIT");
    }

    private void setFavoriteChannel(Properties params) {
        String price = params.getProperty("CH_FAVORITE");
    }

    private void setHiddenChannel(Properties params) {
        String pinNo = params.getProperty("CH_HIDDEN");
    }

    private boolean hasNoValue(String value) {
        return value == null;
    }
}
