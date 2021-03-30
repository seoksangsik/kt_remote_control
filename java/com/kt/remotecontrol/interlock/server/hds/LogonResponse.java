/*
 *  LogonResponse.java
 *
 *  Copyright (c) 2015 Alticast Corp.
 *  All rights reserved. http://www.alticast.com/
 *
 *  This software is the confidential and proprietary information of
 *  Alticast Corp. ("Confidential Information"). You shall not
 *  disclose such Confidential Information and shall use it only in
 *  accordance with the terms of the license agreement you entered into
 *  with Alticast.
 */
package com.kt.remotecontrol.interlock.server.hds;

import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Log;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * <code>LogonResponse</code>
 * 
 * @author seoksangsik
 * @since 2015. 11. 23.
 */
public class LogonResponse extends BasicResponse {
    private static final Log LOG = new Log("LogonResponse");

    private final String UserName = "UserName";
    private final String SvcPinInfo = "SvcPinInfo";
    private final String PinAuthValidate = "PinAuthValidate";
    private final String BuyPinAuthKey = "BuyPinAuthKey";
    private final String PinAuthKey = "PinAuthKey";
    private final String IPTVSAID = "IPTVSAID";

    private String userName = null;
    private String pinAuthValidate = null;
    private String buyPinAuthKey = null;
    private String pinAuthKey = null;
    private String iptvSAID = null;

    /**
     * 
     * @param response
     * i. returnVal^True|UserName^?�용�?SvcPinInfo^PinAuthValidate=72106a56dff8919d86135e7285c7ffacd7ed94a6bdb560acb7eed287fc5eb588
     * ii. returnVal^True|UserName^?�명??SvcPinInfo^PinAuthValidate=fc0a4f963136afab689d8daf5c138f9720c5674696a27e551c0592b048743bca_PinAuthKey=4d7908ad49b37a73_BuyPinAuthKey=d3b6c803efb38858514b11156a61402aa45d702a3807df53_IPTVSAID=90724e29a13c298cd49ddca44832973b
     */
    public LogonResponse(String response) {
        setResult(response);
    }

    public LogonResponse(ArrayList response) {
        setResult(response);
    }

    public String getUserName() {
        return userName;
    }

    public String getPinAuthValidate() {
        return pinAuthValidate;
    }

    public String getBuyPinAuthKey() {
        return buyPinAuthKey;
    }

    public String getPinAuthKey() {
        return pinAuthKey;
    }

    public String getIptvSAID() {
        return iptvSAID;
    }

    protected void setResult(String response) {
        LOG.message("setResult, response=" + response);

        if (response == null) {
            return ;
        }

        StringTokenizer token = new StringTokenizer(response, com.kt.remotecontrol.util.CharConstant.CHAR_VERTICAL);
        StringTokenizer secondToken = null;

        while (token.hasMoreTokens()) {
            secondToken = new StringTokenizer(token.nextToken(), com.kt.remotecontrol.util.CharConstant.CHAR_CARET);
            if (secondToken.countTokens() == 2) {
                setValue(secondToken.nextToken(), secondToken.nextToken());
            } else { // abnormal case
                parseAbnormal(secondToken);
            }
        }
    }

    protected void parseKeyValue(String token) {
        StringTokenizer tokenizer = new StringTokenizer(token, com.kt.remotecontrol.util.CharConstant.CHAR_CARET);
        if (tokenizer.countTokens() == 2) {
            setValue(tokenizer.nextToken(), tokenizer.nextToken());
        } else { // abnormal case
            parseAbnormal(tokenizer);
        }
    }
    /**
     * @param token
     */
    private void parseAbnormal(StringTokenizer token) {
        int tokenCount = token.countTokens();
        LOG.message("parseAbnormal, ^ token count=" + tokenCount);
        System.out.println("parseAbnormal, ^ token count=" + tokenCount);

        if (tokenCount != 3) {
            LOG.error("parseAbnormal, unknown token count=" + tokenCount);
            return;
        }

        String firstToken = token.nextToken();
        String secondToken = token.nextToken();
        int indexSvcPinInfo = secondToken.indexOf(SvcPinInfo);
        LOG.message("parseAbnormal, 1st=" + firstToken + ", 2nd=" + secondToken + ", index=" + indexSvcPinInfo);
        System.out.println("parseAbnormal, 1st=" + firstToken + ", 2nd=" + secondToken + ", index=" + indexSvcPinInfo);

        if (indexSvcPinInfo == -1) {
            setValue(firstToken, secondToken);
        } else {
            setValue(firstToken, secondToken.substring(0, indexSvcPinInfo));
        }

        parseSvcPinInfo(token.nextToken());
    }

    private void parseSvcPinInfo(String parseValue) {
        if (parseValue == null) {
            LOG.error("parseSvcPinInfo, parseValue is null");
            return ;
        }

        if (parseValue.indexOf(com.kt.remotecontrol.util.CharConstant.CHAR_UNDERSCORE) == -1) {
            parseKeyValueWithUnderScore(parseValue);
        } else {
            StringTokenizer token = new StringTokenizer(parseValue, com.kt.remotecontrol.util.CharConstant.CHAR_UNDERSCORE);
            while (token.hasMoreTokens()) {
                parseKeyValueWithUnderScore(token.nextToken());
            }
        }
    }

    protected void parseKeyValueWithUnderScore(String parseValue) {
        StringTokenizer token = new StringTokenizer(parseValue, CharConstant.CHAR_EQUAL);
        while (token.hasMoreTokens()) {
            if (token.countTokens() != 2) {
                LOG.error("parseKeyValue, parseValue='" + parseValue + "'");
                continue;
            }

            setValue(token.nextToken(), token.nextToken());
        }
    }

   private void setValue(String key, String value) {
        if (key == null) {
            LOG.error("setValue, key is null");
            return ;
        }

        LOG.message("setValue, key='" + key + "', value='" + value + "'");

        if (returnVal.equalsIgnoreCase(key)) {
            result = returnVal_True.equalsIgnoreCase(value);
        } else if (UserName.equalsIgnoreCase(key)) {
            userName = value;
        } else if (PinAuthValidate.equalsIgnoreCase(key)) {
            pinAuthValidate = value;
        } else if (BuyPinAuthKey.equalsIgnoreCase(key)) {
            buyPinAuthKey = value;
        } else if (PinAuthKey.equalsIgnoreCase(key)) {
            pinAuthKey = value;
        } else if (IPTVSAID.equalsIgnoreCase(key)) {
            iptvSAID = value;
        } else if (returnDESC.equalsIgnoreCase(key)) {
            message = value;
        } else if (SvcPinInfo.equalsIgnoreCase(key)) {
            parseSvcPinInfo(value);
        } else {
            LOG.message("setValue, unknown key='" + key + "', value='" + value + "'");
            System.err.println("[KIDSCARE] setValue, unknown key=" + key + ", value=" + value);
        }
    }



    /**
     * @param args
     */
    public static void main(String[] args) {
        String resp = "returnVal^True|UserName^?�용�?|SvcPinInfo^PinAuthValidate=72106a56dff8919d86135e7285c7ffacd7ed94a6bdb560acb7eed287fc5eb588";
        resp = "returnVal^True|UserName^?�용�?SvcPinInfo^PinAuthValidate=72106a56dff8919d86135e7285c7ffacd7ed94a6bdb560acb7eed287fc5eb588";
//        resp = "returnVal^True|UserName^?�명??|SvcPinInfo^PinAuthValidate=fc0a4f963136afab689d8daf5c138f9720c5674696a27e551c0592b048743bca_PinAuthKey=4d7908ad49b37a73_BuyPinAuthKey=d3b6c803efb38858514b11156a61402aa45d702a3807df53_IPTVSAID=90724e29a13c298cd49ddca44832973b";
//        resp = "returnVal^True|UserName^?�명??SvcPinInfo^PinAuthValidate=fc0a4f963136afab689d8daf5c138f9720c5674696a27e551c0592b048743bca_PinAuthKey=4d7908ad49b37a73_BuyPinAuthKey=d3b6c803efb38858514b11156a61402aa45d702a3807df53_IPTVSAID=90724e29a13c298cd49ddca44832973b";
        LogonResponse hdsResp = new LogonResponse(resp);
        System.out.println("result=" + hdsResp.isResult());
        System.out.println("UseName=" + hdsResp.getUserName());
        System.out.println("PinAuthValidate=" + hdsResp.getPinAuthValidate());
        System.out.println("BuyPinAuthKey=" + hdsResp.getBuyPinAuthKey());
    }
}
