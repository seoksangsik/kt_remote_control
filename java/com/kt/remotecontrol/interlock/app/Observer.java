package com.kt.remotecontrol.interlock.app;

import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.util.KeyConstant;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.util.MethodConstant;
import com.kt.remotecontrol.interlock.nav.AppHandler;

import java.util.HashMap;

public class Observer {

    private static final Log LOG = new Log("Observer");

    public static void obs_startUnboundApplication(String id) {
        HashMap hashMap = new HashMap();
        hashMap.put(KeyConstant.METHOD, MethodConstant.obs_startUnboundApplication);
        hashMap.put(KeyConstant.FROM, "1");
        hashMap.put(KeyConstant.TARGET, id);
        hashMap.put(KeyConstant.ARGS, "");

        LOG.message("obs_startUnboundApplication, postMessage=" + hashMap.toString());

        ProxyManager.appHandler().talkToApp(hashMap, AppHandler.ID_OBSERVER);
    }
}
