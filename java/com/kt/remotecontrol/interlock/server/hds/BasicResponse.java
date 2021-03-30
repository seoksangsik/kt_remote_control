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
 * <code>BasicResponse</code>
 * 
 * @author seoksangsik
*/
public class BasicResponse implements HDSResponse {
    final String XML_DEFINITION = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";

    public final String returnVal = "returnVal";
    public final String returnVal_True = "True";
    public final String returnDESC = "returnDESC";
    public final String returnCode = "returnCode";

    protected boolean result = false;
    protected String message = null, code = null;

    public BasicResponse() {
    }

    public BasicResponse(String response) {
        setResult(response);
    }

    public BasicResponse(ArrayList response) {
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

    public boolean has(String value) {
        return getResultMessage().indexOf(value) != -1;
    }

    // 인증에 실패하였습니다. 핀번호 확인후 다시 시도하시기 바랍니다.[HDSE019]
    public boolean isPinError() {
        return has("HDSE019");
    }

    protected void setResult(String message) {
        if (message != null) {
            int index = message.indexOf(com.kt.remotecontrol.util.CharConstant.CHAR_VERTICAL);
            if (index != -1) { // exist Char '|'
                this.message = message.substring(index + 1);
                message = message.substring(0, index);
            }
            index = message.indexOf(com.kt.remotecontrol.util.CharConstant.CHAR_CARET);
            if (returnVal.equalsIgnoreCase(message.substring(0, index))) {
                result = returnVal_True.equalsIgnoreCase(message.substring(index + 1));
            }
        }
    }

    public static void main(String[] args) {
        ArrayList response = new ArrayList();
        response.add("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        response.add("<string xmlns=\"http://tempuri.org/\">returnVal^True|returnDESC^SUCCESS</string>");
        response.add("<string xmlns=\"http://tempuri.org/\">returnVal^False|returnCode^HDSE099|returnDESC^HDS �ý��� ����</string>");

        BasicResponse res = new BasicResponse(response);
        System.out.println(res.isResult());
        System.out.println(res.getResultMessage());
    }

    protected void setResult(final ArrayList response) {
        if (response == null) {
            return ;
        }

        ArrayList values = substractTag(response);
        parseResult(values);
    }

    protected void parseResult(ArrayList values) {
        String aLine;
        int count = values.size();
        StringTokenizer firstToken = null;

        for (int i = 0; i < count; i++) {
            aLine = (String) values.get(i);
            if (aLine == null) {
                continue;
            }

            firstToken = new StringTokenizer(aLine, com.kt.remotecontrol.util.CharConstant.CHAR_VERTICAL);
            while (firstToken.hasMoreTokens()) {
                parseKeyValue(firstToken.nextToken());
            } // end outer-while statement
        } // end for statement
    }

    protected ArrayList substractTag(ArrayList response) {
        ArrayList values;
        String firstLine = (String) response.get(0);
        final String XML_DEFINITION = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";

        if (XML_DEFINITION.equalsIgnoreCase(firstLine)) {
            values = parseExceptDefinition(response);
        } else if (firstLine.startsWith(XML_DEFINITION)) {
            response.set(0, firstLine.substring(XML_DEFINITION.length()));
            values = parseFirst(response);
        } else {
            values = new ArrayList(response);
        }

        return values;
    }

    protected void parseKeyValue(String token) {
        StringTokenizer secondToken = new StringTokenizer(token, com.kt.remotecontrol.util.CharConstant.CHAR_CARET);
        String key, value;

        while (secondToken.hasMoreTokens()) {
            if (secondToken.countTokens() != 2) {
                continue;
            }

            key = secondToken.nextToken();
            value = secondToken.nextToken();

            if (returnVal.equalsIgnoreCase(key)) {
                result = returnVal_True.equalsIgnoreCase(value);
            } else if (returnDESC.equalsIgnoreCase(key)) {
                message = value;
            } else if (returnCode.equalsIgnoreCase(key)) {
                code = value;
            }
        } // end inner-while statement
    }

    private ArrayList parseExceptDefinition(ArrayList response) {
        boolean closeEndTag = true, addValue = false;
        int beginIndex = -1, endIndex = -1;
        String key = null, value = "";
        String aLine;
        ArrayList values = new ArrayList();
        int count = response.size();

        for (int i = 1; i < count; i++) {
            aLine = (String) response.get(i);
//            System.out.println("aLine=" + aLine);
            beginIndex = aLine.indexOf(com.kt.remotecontrol.util.CharConstant.CHAR_LESS_THAN);
            endIndex = aLine.indexOf(com.kt.remotecontrol.util.CharConstant.CHAR_GREATER_THAN);

            if (beginIndex != -1 && endIndex != -1) {
                if (key != null) {
                    beginIndex = 0;
                    endIndex = aLine.indexOf(com.kt.remotecontrol.util.CharConstant.CHAR_LESS_THAN + com.kt.remotecontrol.util.CharConstant.CHAR_SLASH + key);
                    addValue = endIndex != -1;  // find 'key' close element
                }

                if (!addValue) {
                    if (closeEndTag) {
                        key = aLine.substring(beginIndex + 1, endIndex);
                        beginIndex = key.indexOf(com.kt.remotecontrol.util.CharConstant.CHAR_SPACE);
                        if (beginIndex != -1) {
                            key = key.substring(0, beginIndex);
                        }

                        beginIndex = endIndex + 1; // 1 = '>' size
                        endIndex = aLine.indexOf(com.kt.remotecontrol.util.CharConstant.CHAR_LESS_THAN + com.kt.remotecontrol.util.CharConstant.CHAR_SLASH + key);
                        if (endIndex == -1) {
                            value = aLine.substring(beginIndex) + com.kt.remotecontrol.util.CharConstant.CHAR_NEW_LINE;
                            closeEndTag = false;
                        } else {
                            addValue = true;
                        }
                    } else {
                        endIndex = aLine.indexOf(com.kt.remotecontrol.util.CharConstant.CHAR_LESS_THAN + com.kt.remotecontrol.util.CharConstant.CHAR_SLASH + key);
                        if (endIndex == -1) {
                            value += aLine + com.kt.remotecontrol.util.CharConstant.CHAR_NEW_LINE;
                        } else {
                            beginIndex = 0;
                            addValue = true;
                        }
                    }
                }
            } else { // invalid start and end index
                value += aLine + com.kt.remotecontrol.util.CharConstant.CHAR_NEW_LINE;
            }

            if (addValue) {
                if (endIndex == 0) {
                    value = value.substring(0, value.lastIndexOf(com.kt.remotecontrol.util.CharConstant.CHAR_NEW_LINE));
                }
                values.add(value + aLine.substring(beginIndex, endIndex));
                key = null;
                value = "";
                closeEndTag = true;
                addValue = false;
            }
        }

        return values;
    }

    private ArrayList parseFirst(ArrayList response) {
        boolean closeEndTag = true, addValue = false;
        int beginIndex = -1, endIndex = -1;
        String key = null, value = "";
        String aLine;
        ArrayList values = new ArrayList();
        int count = response.size();

        for (int i = 0; i < count; i++) {
            aLine = (String) response.get(i);
//            System.out.println("aLine=" + aLine);
            beginIndex = aLine.indexOf(com.kt.remotecontrol.util.CharConstant.CHAR_LESS_THAN);
            endIndex = aLine.indexOf(com.kt.remotecontrol.util.CharConstant.CHAR_GREATER_THAN);

            if (beginIndex == -1 || endIndex == -1) { // invalid start and end index
                value += aLine + com.kt.remotecontrol.util.CharConstant.CHAR_NEW_LINE;
            } else {
                if (key != null) {
                    beginIndex = 0;
                    endIndex = aLine.indexOf(com.kt.remotecontrol.util.CharConstant.CHAR_LESS_THAN + com.kt.remotecontrol.util.CharConstant.CHAR_SLASH + key);
                    addValue = endIndex != -1;  // find 'key' close element
                }

                if (!addValue) {
                    if (closeEndTag) {
                        key = aLine.substring(beginIndex + 1, endIndex);
                        beginIndex = key.indexOf(com.kt.remotecontrol.util.CharConstant.CHAR_SPACE);
                        if (beginIndex != -1) {
                            key = key.substring(0, beginIndex);
                        }

                        beginIndex = endIndex + 1; // 1 = '>' size
                        endIndex = aLine.indexOf(com.kt.remotecontrol.util.CharConstant.CHAR_LESS_THAN + com.kt.remotecontrol.util.CharConstant.CHAR_SLASH + key);
                        if (endIndex == -1) {
                            value = aLine.substring(beginIndex) + com.kt.remotecontrol.util.CharConstant.CHAR_NEW_LINE;
                            closeEndTag = false;
                        } else {
                            addValue = true;
                        }
                    } else {
                        endIndex = aLine.indexOf(com.kt.remotecontrol.util.CharConstant.CHAR_LESS_THAN + com.kt.remotecontrol.util.CharConstant.CHAR_SLASH + key);
                        if (endIndex == -1) {
                            value += aLine + com.kt.remotecontrol.util.CharConstant.CHAR_NEW_LINE;
                        } else {
                            beginIndex = 0;
                            addValue = true;
                        }
                    }
                }
            }

            if (addValue) {
                if (endIndex == 0) {
                    value = value.substring(0, value.lastIndexOf(CharConstant.CHAR_NEW_LINE));
                }
                values.add(value + aLine.substring(beginIndex, endIndex));
                key = null;
                value = "";
                closeEndTag = true;
                addValue = false;
            }
        }

        return values;
    }
}
