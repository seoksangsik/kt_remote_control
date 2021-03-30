package com.kt.remotecontrol.interlock.spec;

import com.kt.navsuite.core.Channel;
import com.kt.remotecontrol.util.Log;

public class NullableSIRetrieval implements SIRetrieval {
    private static final Log LOG = new Log("NullableSIRetrieval");

    public NullableSIRetrieval() {
        LOG.error("instance");
    }

    public String getCurrentProgramName(Channel channel) {
        return null;
    }
}
