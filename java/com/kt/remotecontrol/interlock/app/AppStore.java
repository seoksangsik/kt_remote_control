package com.kt.remotecontrol.interlock.app;

import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.KeyConstant;
import com.kt.remotecontrol.interlock.nav.AppHandler;

import java.util.HashMap;
import java.util.StringTokenizer;

public class AppStore {
    /**
     * @param message umtMsg = as_runSASApp|3013|C|51160000000241|L|
     */
    public static void as_runSASApp(String message) {
        HashMap hashMap = new HashMap();
        hashMap.put(KeyConstant.FROM, AppHandler.ID_KIDSCARE);

        StringTokenizer tokenizer = new StringTokenizer(message, CharConstant.CHAR_VERTICAL);
        hashMap.put(KeyConstant.METHOD, tokenizer.nextToken()); // as_runSASApp
        tokenizer.nextToken();
        hashMap.put(KeyConstant.ID_FLAG, tokenizer.nextToken());
        hashMap.put(KeyConstant.REFER_ID, tokenizer.nextToken());
        hashMap.put(KeyConstant.DEPLOY_CD, tokenizer.nextToken());

        talkToAppStore(hashMap);
    }

    private static void talkToAppStore(HashMap hashMap) {
        ProxyManager.appHandler().talkToApp(hashMap, AppHandler.ID_QRATOR);
    }
}
