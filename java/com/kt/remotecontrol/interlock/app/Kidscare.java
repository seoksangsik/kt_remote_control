package com.kt.remotecontrol.interlock.app;

import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.ErrorCode;
import com.kt.remotecontrol.util.KeyConstant;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.util.MethodConstant;
import com.kt.remotecontrol.util.TimeConstant;
import com.kt.remotecontrol.interlock.nav.AppHandler;

import java.util.HashMap;

public class Kidscare {

    private static final Log LOG = new Log("Kidscare");

    public static void passMessage(String message, String toApp) {
        HashMap hashMap = new HashMap();
        hashMap.put(KeyConstant.METHOD, MethodConstant.kids_passMessage);
        hashMap.put(KeyConstant.FROM, AppHandler.ID_KIDSCARE);
        hashMap.put(KeyConstant.MSG, message);

        LOG.message("kids_passMessage, postMessage=" + hashMap.toString());

        talkToApp(hashMap, toApp);
    }

    public static HashMap changeBuyPin(String pin, String newPIN) {
        HashMap hashMap = new HashMap();
        hashMap.put(KeyConstant.METHOD, MethodConstant.changeBuyPin);
        hashMap.put(KeyConstant.SAID, ProxyManager.otherHandler().getSAID());
        hashMap.put(KeyConstant.PIN, pin);
        hashMap.put(KeyConstant.NEWPIN, newPIN);
        hashMap.put(KeyConstant.DEPENDS, MethodConstant.checkBuyPin);
        hashMap.put(KeyConstant.LINK, Constants.LINK_HDS);
        hashMap.put(KeyConstant.WAIT_TIMEOUT, Long.valueOf(TimeConstant.FIVE_SECONDS));

        return execute(hashMap);
    }

    public static HashMap passMessageCallback(String toApp, String message) {
        HashMap hashMap = new HashMap();
        hashMap.put(KeyConstant.METHOD, MethodConstant.kids_passMessage_callback);
        hashMap.put(KeyConstant.APPID, toApp);
        hashMap.put(KeyConstant.MSG, message);
        hashMap.put(KeyConstant.LINK, Constants.LINK_POST_MESSAGE);
        hashMap.put(KeyConstant.WAIT_TIMEOUT, Long.valueOf(TimeConstant.TEN_SECONDS));

        return execute(hashMap);
    }

    public static String joinKidscare(String said, String passwd, String cellPhone) {
        HashMap hashMap = controlKidscare(MethodConstant.joinKidscare, said, passwd);
        hashMap.put(KeyConstant.CELLPHONE, cellPhone);

        HashMap response = execute(hashMap);
        return getResultCode(response, "joinsKidscare");
    }

    public static String cancelKidscare(String said, String passwd) {
        HashMap hashMap = controlKidscare(MethodConstant.cancelKidscare, said, passwd);

        HashMap response = execute(hashMap);
        return getResultCode(response, "cancelKidscare");
    }

    private static HashMap controlKidscare(String method, String said, String passwd) {
        HashMap hashMap = new HashMap();
        hashMap.put(KeyConstant.METHOD, method);
        hashMap.put(KeyConstant.SAID, said);
        hashMap.put(KeyConstant.PIN, passwd);
        hashMap.put(KeyConstant.STBIP, ProxyManager.otherHandler().getIP());
        hashMap.put(KeyConstant.DEPENDS, MethodConstant.checkBuyPin);
        hashMap.put(KeyConstant.LINK, Constants.LINK_HDS);
        hashMap.put(KeyConstant.WAIT_TIMEOUT, Long.valueOf(TimeConstant.FIVE_SECONDS));

        return hashMap;
    }

    private static void talkToApp(HashMap hashMap, String toApp) {
        ProxyManager.appHandler().talkToApp(hashMap, toApp);
    }

    private static HashMap execute(HashMap hashMap) {
        return ProxyManager.appHandler().execute(hashMap);
    }

    private static String getResultCode(HashMap responseMap, String callMethod) {
        String resultCode;

        if (responseMap.isEmpty()) {
            LOG.message(callMethod + ", empty in the Result Map");
            resultCode = ErrorCode.C601;
        } else {
            resultCode = (String) responseMap.get(KeyConstant.RESULT_CODE);

            if (responseMap.containsKey(KeyConstant.ERROR_MESSAGE)) {
                resultCode += CharConstant.CHAR_CARET + responseMap.get(KeyConstant.ERROR_MESSAGE);
            }
        }

        LOG.message("[Kidscare] " + callMethod + ", resultCode=" + resultCode);

        return resultCode;
    }
}
