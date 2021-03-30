package com.kt.remotecontrol.interlock.app;

import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.KeyConstant;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.util.MethodConstant;
import com.kt.remotecontrol.util.TimeConstant;
import com.kt.remotecontrol.interlock.nav.AppHandler;

import java.util.HashMap;
import java.util.Properties;

public class HomePortal {

    private static final Log LOG = new Log("HomePortal");

    public static boolean hp_watchContent(String id) {

        HashMap hashMap = new HashMap();
        hashMap.put(KeyConstant.METHOD, MethodConstant.hp_watchContent);
        hashMap.put(KeyConstant.CONST_ID, id);
        hashMap.put(KeyConstant.FROM, "1"); hashMap.put(KeyConstant.CAT_ID, "");

        LOG.message("hp_watchContent, postMessage=" + hashMap.toString());

        talkToHomePortal(hashMap);

        return false;
    }

    public static void hp_watchContent(Properties params) {

        HashMap hashMap = makeWatchContent(MethodConstant.hp_watchContent, params);

        LOG.message("hp_watchContent, postMessage=" + hashMap.toString());

        talkToHomePortal(hashMap);
    }

    public static void hp_watchContentForced(Properties params) {

        HashMap hashMap = makeWatchContent(MethodConstant.hp_watchContentForced, params);

        LOG.message("hp_watchContentForced, postMessage=" + hashMap.toString());

        talkToHomePortal(hashMap);
    }

    public static void hp_showCategory(String categoryID) {
        HashMap hashMap = new HashMap();
        hashMap.put(KeyConstant.METHOD, MethodConstant.hp_showCategory);
        hashMap.put(KeyConstant.CAT_ID, categoryID);

        LOG.message("hp_showCategory, postMessage=" + hashMap.toString());

        talkToHomePortal(hashMap);
    }

    public static HashMap hp_setAutoPower(String timeOn, String timeOff, String repeat) {

        boolean isRepeat = Constants.RPT_SET_REPEAT.equals(repeat);

        HashMap hashMap = new HashMap();
        hashMap.put(KeyConstant.METHOD, MethodConstant.hp_setAutoPower);
        hashMap.put(KeyConstant.ON_TIME, timeOn);
        hashMap.put(KeyConstant.OFF_TIME, timeOff);
        hashMap.put(KeyConstant.REPEAT, String.valueOf(isRepeat));
        hashMap.put(KeyConstant.LINK, Constants.LINK_POST_MESSAGE);
        hashMap.put(KeyConstant.DEPENDS, MethodConstant.hp_checkVersion);

        hashMap.put(KeyConstant.WAIT_TIMEOUT, Long.valueOf(TimeConstant.THREE_SECONDS));

        return ProxyManager.appHandler().execute(hashMap);
    }

    private static void talkToHomePortal(HashMap hashMap) {
        ProxyManager.appHandler().talkToApp(hashMap, AppHandler.ID_HOME_PORTAL);
    }

    private static HashMap makeWatchContent(String method, Properties params) {

        String contentID = params.getProperty(Constants.CON_ID); // VOD Asset ID
        String categoryID = checkNull(params.getProperty(Constants.CAT_ID));
        String isPrice = checkNull(params.getProperty(Constants.PRICE_YN));
        String seemlessTime = checkNull(params.getProperty(Constants.SEEMLEES_TIME));
        String otnSaid = checkNull(params.getProperty(Constants.OTN_SAID));

        HashMap hashMap = new HashMap();
        hashMap.put(KeyConstant.METHOD, method);
        hashMap.put(KeyConstant.CONST_ID, contentID);
        hashMap.put(KeyConstant.CAT_ID, categoryID);
        hashMap.put(KeyConstant.PRICE_YN, isPrice);
        hashMap.put(KeyConstant.SEEMLEES_TIME, seemlessTime);
        hashMap.put(KeyConstant.OTN_SAID, otnSaid);

        return hashMap;
    }

    private static String checkNull(String value) {
        return value == null ? "" : value;
    }
}
