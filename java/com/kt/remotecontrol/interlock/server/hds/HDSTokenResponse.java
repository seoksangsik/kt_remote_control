/*
 *  BasicResponse.java
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

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * <code>HDSTokenResponse</code>
 * 
 * @author seoksangsik
*/
public class HDSTokenResponse extends BasicResponse {
    public final String HDSToken = "HDSToken";

    protected boolean result = false;
    protected String message = null, code = null;

    public HDSTokenResponse() {
    }

    public HDSTokenResponse(String response) {
        setResult(response);
    }

    public HDSTokenResponse(ArrayList response) {
        setResult(response);
    }

    /* (non-Javadoc)
     * @see com.kt.kidscare.hds.HDSResponse#isResult()
     */
    public boolean isResult() {
        return result;
    }

    /* (non-Javadoc)
     * @see com.kt.kidscare.hds.HDSResponse#getResultMessage()
     */
    public String getResultMessage() {
        return code == null ? message : "[" + code + "] " + message;
    }

    protected void setResult(final ArrayList response) {
        if (response == null) {
            return ;
        }

//        "<?xml version=\"1.0\" encoding=\"utf-8\"?><string xmlns=\"http://tempuri.org/\">returnVal^True|HDSToken^214be4e66db89dea4e19b157f4c835ef0b18941a4c5378e49d8aa5827e2b6fc2725db841cd4b7719</string>"
        ArrayList values = substractTag(response);
        parseResult(values);
    }

    protected void parseKeyValue(String token) {
        System.out.println("HDSToken, parseKeyValue");
        StringTokenizer tokenizer = new StringTokenizer(token, CharConstant.CHAR_CARET);
        String key, value;

        while (tokenizer.hasMoreTokens()) {
            if (tokenizer.countTokens() != 2) {
                continue;
            }

            key = tokenizer.nextToken();
            value = tokenizer.nextToken();

            if (returnVal.equalsIgnoreCase(key)) {
                result = returnVal_True.equalsIgnoreCase(value);
            } else if (returnDESC.equalsIgnoreCase(key)) {
                message = value;
            } else if (returnCode.equalsIgnoreCase(key)) {
                code = value;
            } else if (HDSToken.equalsIgnoreCase(key)) {
                message = value;
            }
        } // end inner-while statement
    }

    public static void main(String[] args) {
        ArrayList response = new ArrayList();
//        response.add("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
//        response.add("<string xmlns=\"http://tempuri.org/\">returnVal^True|returnDESC^SUCCESS</string>");
        response.add("<?xml version=\"1.0\" encoding=\"utf-8\"?><string xmlns=\"http://tempuri.org/\">returnVal^True|HDSToken^214be4e66db89dea4e19b157f4c835ef0b18941a4c5378e49d8aa5827e2b6fc2725db841cd4b7719</string>");

        HDSTokenResponse res = new HDSTokenResponse(response);
        System.out.println(res.isResult());
        System.out.println(res.getResultMessage());
    }
}
