package com.kt.remotecontrol.model;

import com.kt.remotecontrol.util.CharConstant;

public class TVStatus {
    private String uiStatus;
    private String tvType;
    private String stbType = "1"; // 1: WEB
    private String kidsMode;
    private String appIDByDialog;

    public TVStatus(String uiStatus, boolean isOTS, boolean isKidsMode, String appIDByDialog) {
        setUIStatus(uiStatus);
        setTVType(isOTS);
        setKidsMode(isKidsMode);
        this.appIDByDialog = appIDByDialog;
    }

    /**
     * 키즈모드 설정 true(0:설정), false(1:미설정)
     * @param isKidsMode true or false
     */
    public void setKidsMode(boolean isKidsMode) {
        kidsMode = isKidsMode ? "0" : "1";
    }

    public void setUIStatus(String uiStatus) {
        this.uiStatus = uiStatus;
    }

    public String toString() {
        return uiStatus + CharConstant.CHAR_CARET + tvType + CharConstant.CHAR_CARET + stbType + CharConstant.CHAR_CARET
                + kidsMode + CharConstant.CHAR_CARET + appIDByDialog;
    }

    /**
     * true(1: OTS), false(0: OTV)
     * @param isOTS true or false
     */
    private void setTVType(boolean isOTS) {
        tvType = isOTS ? "1" : "0";
    }
}
