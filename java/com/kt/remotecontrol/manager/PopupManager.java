package com.kt.remotecontrol.manager;

import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.KeyConstant;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.util.MethodConstant;
import com.kt.remotecontrol.interlock.ProxyManager;

import java.util.HashMap;
import java.util.Properties;

public class PopupManager {
    private static final Log LOG = new Log("PopupManager");

    private static PopupManager instance = new PopupManager();

    private final int POPUP_INDEX_MESSAGE = 0;
    private final int POPUP_INDEX_PHONE_NUM = 1;
    private final int POPUP_INDEX_TITLE = 2;
    private final int POPUP_INDEX_SHOW_TIME = 3;

    private PopupManager() {
    }

    public static PopupManager getInstance() {
        return instance;
    }

    public void showMessageByThread(Properties params) {
        String[] messages = getMessageAfterCheck(params);
        showMessageByThread(messages);
    }

    public boolean invalidMessageType(String type) {
        return !Constants.MSG_YN_EMPTY_MSG.equals(type)
                && !Constants.MSG_YN_SHOW_MSG.equals(type)
                && !Constants.MSG_YN_NO_MSG.equals(type);
    }

    public String[] getMessageAfterCheck(Properties params) {
        String hasMessage = params.getProperty(Constants.MSG_YN);

        if (Constants.MSG_YN_SHOW_MSG.equals(hasMessage)) {
            return getMessage(params);
        }

        return null;
    }

    public void showMessageByThread(final String[] messages) {
        if (messages == null) {
            return ;
        }

        String message = messages[POPUP_INDEX_MESSAGE];
        String cellPhone = messages[POPUP_INDEX_PHONE_NUM];
        String displayTime = messages[POPUP_INDEX_SHOW_TIME];
        final HashMap ropParam = createParameter(message, cellPhone, displayTime);

        new Thread("showMessageByThread") {
            public void run() {
                ProxyManager.appHandler().execute(ropParam);
            }
        }.start();
   }

    public void showPopup(String message, String cellPhone, String displayTime) {
        LOG.message("showPopup");

        HashMap ropParam = createParameter(message, cellPhone, displayTime);
        ProxyManager.appHandler().execute(ropParam);
    }

    private HashMap createParameter(String message, String cellPhone, String displayTime) {
        HashMap ropParam = new HashMap();
        ropParam.put(KeyConstant.METHOD, MethodConstant.showPopup);
        ropParam.put(KeyConstant.POPUP_TYPE, Constants.POPUP_TYPE_NOTICE);
        ropParam.put(KeyConstant.MESSAGE, message);
        ropParam.put(KeyConstant.CELLPHONE, cellPhone);
        ropParam.put(KeyConstant.DISPLAY_TIME, displayTime);

        return ropParam;
    }

    private String[] getMessage(Properties params) {
        return new String[] { params.getProperty(Constants.MSG),
                params.getProperty(Constants.HP_NO),
                params.getProperty(Constants.MSG_TITLE),
                params.getProperty(Constants.MSG_TIME) };
    }
}
