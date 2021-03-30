/*
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
package com.kt.remotecontrol;

import com.kt.remotecontrol.util.Log;

public class WorkingConfig {

    public static String VERSION;

    // 로그 on/off
    public static final boolean LOG_INCLUDE = Log.INCLUDE;

    // 제어서버로부터 온 요청이 아니면 거부 (최종 릴리즈시 true 가 되어야 함)
    public static boolean CHECK_IFSERVER_REQUEST = false;

    // 라이브 제어서버 연결 (최종 릴리즈시 true 가 되어야 함)
    public static boolean isLive = true;

    public static String CONTROL_SERVER = "";

    public static String REQUEST_SERVER = "";

    public static String LOCAL_SERVER = "221.147.113.5";

    public static String HDS_SERVER = "";

    // 제어서버와 싱크 실패하면 어플 실행 안되게 (최종 릴리즈시 true 가 되어야만 함)
    public static final boolean MUST_SYNC_SUCCESS = true;

    // 키즈케어 가입자 체크 할지 안할지 (최종 릴리즈시 true 가 되어야만 함)
    public static final boolean CHECK_SUBSCRIBER = true;
}
