/*
 *  Util.java
 *
 *  Copyright (c) 2004 Alticast Corp.
 *  All rights reserved. http://www.alticast.com/
 *
 *  This software is the confidential and proprietary information of
 *  Alticast Corp. ("Confidential Information"). You shall not
 *  disclose such Confidential Information and shall use it only in
 *  accordance with the terms of the license agreement you entered into
 *  with Alticast.
 */
package com.kt.remotecontrol.util;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;

/**
 *
 */
public class Util {

    private static final Log LOG = new Log("Util");

    public static String encodeUTF8(String value) {
        String result;
        try {
            result = URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            result = value;
        }
        return result;
    }

    public static String decodeUTF8(String value) {
        String result;
        try {
            result = URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            result = value;
        }
        return result;
    }

    /**
     * 박스가 켜지거나 시청제한을 풀 경우 채널체인지가 일어나는데
     * 그 때 VOD 이어보기와 같은 팝업이 뜨면 팝업이 사라지는 문제가 있다.
     * 이를 피해가기 위해서 sleep 을 준다.
     * @param callMethod
     * @return
     */
    public static boolean hasErrorForSleep(String callMethod) {
        LOG.message("[Util] " + callMethod + ", need delay to start VOD");

        try {
            Thread.sleep(TimeConstant.SEVEN_SECONDS);
        } catch (InterruptedException e) {
            LOG.error("[Util] " + callMethod + ", InterruptedException.");
            return true;
        }

        return false;
    }

    public static String convertIpAddress(String url) {
        String address = "";

        try {
            byte[] byteAddresses = InetAddress.getByName(url).getAddress();
            int[] addresses = new int[byteAddresses.length];
            for (int i = 0; i < byteAddresses.length; i++) {
                addresses[i] = byteAddresses[i] & 0xFF;
            }

            address = addresses[0] + CharConstant.CHAR_PERIOD + addresses[1]
                    + CharConstant.CHAR_PERIOD + addresses[2] + CharConstant.CHAR_PERIOD
                    + addresses[3];
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

        return address;
    }

    public static String convertDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        String year = String.valueOf(calendar.get(Calendar.YEAR));
        String month = formatTwoDigit(calendar, Calendar.MONDAY, 1);
        String day = formatTwoDigit(calendar, Calendar.DATE);
        String hour = formatTwoDigit(calendar, Calendar.HOUR_OF_DAY);
        String min = formatTwoDigit(calendar, Calendar.MINUTE);
        String sec = formatTwoDigit(calendar, Calendar.SECOND);

        return year + CharConstant.CHAR_HYPHEN + month + CharConstant.CHAR_HYPHEN
                + day + CharConstant.CHAR_SPACE + hour + CharConstant.CHAR_COLON
                + min + CharConstant.CHAR_COLON + sec;
    }

    private static String formatTwoDigit(Calendar calendar, int type) {
        return formatTwoDigit(calendar, type, 0);
    }

    private static String formatTwoDigit(Calendar calendar, int type, int adjust) {
        String value = String.valueOf(calendar.get(type) + adjust);

        if (value.length() == 1) {
            return  "0" + value;
        }

        return value;
    }
}
