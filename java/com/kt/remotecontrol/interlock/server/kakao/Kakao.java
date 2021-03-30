package com.kt.remotecontrol.interlock.server.kakao;

import com.kt.navsuite.util.Log;
import com.kt.remotecontrol.http.HttpRequest;
import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Constants;

import java.util.ArrayList;

public class Kakao {
    public static final String BEGIN_TAG_ICON = "<icon>";
    public static final String END_TAG_ICON = "</icon>";
    public static final String BEGIN_TAG_URL = "<url>";
    public static final String END_TAG_URL = "</url>";
    public static final String BEGIN_TAG_DESC = "<desc>";
    public static final String END_TAG_DESC = "</desc>";

    public static final String GREATER_THAN = "&gt;";
    public static final String LESS_THAN = "&lt;";
    public static final String AMPERSAND = "&amp;";
    public static final String CITATION_MARK = "&quot;";
    public static final String SPACE = "&nbsp;";

    public ArrayList loadEmoticon() {
        String url = Constants.KAKAO_LIVE_URI + Constants.KAKAO_SERVICE_EMOTICON;
        ArrayList results = HttpRequest.get(url);
        return parseEmoticon(results);
    }

    public ArrayList parseEmoticon(ArrayList result) {
        if (Log.INCLUDE) {
            Log.message("getEmoticon, message=[" + result + "]");
        }
        ArrayList emoticonList = new ArrayList();
        String value;
        com.kt.remotecontrol.interlock.server.kakao.Emoticon anEmoticon = null;

        for (int i = 0; i < result.size(); i++) {
            value = ((String) result.get(i)).trim();

            if (value.startsWith(BEGIN_TAG_ICON)) {
                anEmoticon = new com.kt.remotecontrol.interlock.server.kakao.Emoticon();
            } else if (value.startsWith(BEGIN_TAG_URL)) {
                setImageUrl(value, anEmoticon);
            } else if (value.startsWith(BEGIN_TAG_DESC)) {
                setDesc(value, anEmoticon);
            }

            if (value.startsWith(END_TAG_ICON) && anEmoticon.isValid()) {
                if (Log.INCLUDE) {
                    Log.message("getEmoticon, add : " + anEmoticon);
                }
                emoticonList.add(anEmoticon);
            }
        }

        return emoticonList;
    }

    private void setImageUrl(String value, com.kt.remotecontrol.interlock.server.kakao.Emoticon emoticon) {
        String imageUrl = value.substring(BEGIN_TAG_URL.length(), value.indexOf(END_TAG_URL));
        emoticon.setImageUrl(imageUrl);
    }

    private void setDesc(String value, Emoticon emoticon) {
        String desc = value.substring(BEGIN_TAG_DESC.length(), value.indexOf(END_TAG_DESC));
        String name = convertTag(desc);
        emoticon.setName(name);
    }

    private String convertTag(String value) {
        String result = replaceHtmlTag(value, GREATER_THAN, CharConstant.CHAR_GREATER_THAN);
        result = replaceHtmlTag(result, LESS_THAN, CharConstant.CHAR_LESS_THAN);
        result = replaceHtmlTag(result, AMPERSAND, CharConstant.CHAR_AMPERSAND);
        result = replaceHtmlTag(result, CITATION_MARK, CharConstant.CHAR_CITATION_MARK);
        return replaceHtmlTag(result, SPACE, CharConstant.CHAR_SPACE);
    }

    private String replaceHtmlTag(String value, String htmlTag, String character) {
        int index = value.indexOf(htmlTag);
        if (index == -1) {
            return value;
        }

        String result = value.substring(0, index) + character + value.substring(index + htmlTag.length());
        return replaceHtmlTag(result, htmlTag, character);
    }
}
