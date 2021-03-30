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
package com.kt.remotecontrol.service.request;

import com.kt.remotecontrol.http.HttpRequest;
import com.kt.remotecontrol.manager.StatusManager;
import com.kt.remotecontrol.model.WatchRecorder;
import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.ErrorCode;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.interlock.ProxyManager;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

public class RequestService {

    private static final Log LOG = new Log("RequestService");

    private String url, service;
    private boolean mustSync;

    public RequestService(String url, String service, boolean mustSync) {
        this.url = url;
        this.service = service;
        this.mustSync = mustSync;
    }

    /**
     * Remote Control 서버와 Synchronized 시킨다.
     * 부팅되거나 스탠바이에서 돌아올 때 불린다.
     */
    public void startServiceSync() {

        // 서비스 가입 확인과 제어서버와 동기화는 시간이 오래 걸려서 쓰레드로 뺀다.
        // STB state change에서 해야 하므로
        new Thread("startServiceSync") {
            public void run() {
                LOG.message("startServiceSync, Thread start~~~");

                serviceSync();

                LOG.message("startServiceSync, Thread end~~~");
            }
        }.start();
    }

    private void serviceSync() {

        int retryCount = 0;

        while (StatusManager.getInstance().isNotSyncOK()
                && retryCount < Constants.SYNC_RETRY_CNT) {

            boolean isSuccess = syncToIF();

            if (!mustSync) {
                LOG.message("startServiceSync, ignore Sync result for test.");

                isSuccess = true; // 테스트 : 동기화 실패해도 어플 동작하도록 하기 위해.
            }

            if (isSuccess) {
                LOG.message("startServiceSync, Success Synchronized with Remote Constrol Server");

                StatusManager.getInstance().completedSync();
            } else {
                LOG.message("startServiceSync, Fail Synchronized with Remote Control Server");

                try {
                    Thread.sleep(Constants.SYNC_RETRY_GAP);
                    retryCount++;
                    LOG.message("startServiceSync, Synchronize RETRY ~~~");
                } catch (Exception e) {
                }
            }
        } // while
    }

    /**
     * 시청내역누적 (STBAgent -> STB I/F Server)
     */
    public boolean watchRecordToIF(WatchRecorder watchRecorder) {
        int type = watchRecorder.getType();
        String id = watchRecorder.getID();
        String contentType = watchRecorder.getContentType();
        String name = watchRecorder.getName();
        String startTime = watchRecorder.getRecordStartTime();
        String endTime = watchRecorder.getRecordTime();
        boolean isSkylifeChannel = watchRecorder.isSkylifeChannel();

        String stringRunMode = Integer.toString(type);

        LOG.message("requestWatchRecordToIF, run_mode:" + stringRunMode
                    + ", id:" + id + ", name:" + name + ", startTime:" + startTime + ", endTime:" + endTime
                    + ", skylifeChannel=" + isSkylifeChannel);

        Hashtable data = new Hashtable();
        data.put(Constants.CMD, Constants.ETC0304);
        data.put(Constants.SAID, com.kt.remotecontrol.interlock.ProxyManager.otherHandler().getSAID());
        data.put(Constants.CONTENTS_TYPE, contentType);
        data.put(Constants.CONTENTS_ID, id);
        data.put(Constants.ST_TIME, startTime);
        data.put(Constants.END_TIME, endTime);
        data.put(Constants.NAME, name == null ? "" : name);

        if (Constants.RUN_MODE_CHANNEL.equals(stringRunMode)) {
            data.put(Constants.CH_TYPE, isSkylifeChannel ? Constants.CH_MODE_SKYLIFE : Constants.CH_MODE_KT);
        }

        return requestHttpPost(data, Constants.SERVER_PORT_WATCH_HISTORY);
    }

    /**
     * 최초 상태 알림 (STBAgent -> STB I/F Server) : ETC0104 키즈케어
     */
    public boolean initialStatusToIF(int runMode, String channelNumber, String channelName, String programName) {

        boolean success = initialStatus(Constants.ETC0104, runMode, channelName, channelNumber, programName);

        LOG.message("initialStatusToIF, result=" + success);

        return success;
    }

    /**
     * 최초 상태 알림 (STBAgent -> STB I/F Server) : ETC0105 리모트 어플용
     **/
    public boolean initialStatusForRemoteToIF(int runMode, String channelNumber, String channelName,
                                              String programName) {

        LOG.message("initialStatusForRemoteToIF, runMode:" + runMode
                    + ", id:" + channelNumber + ", chName:" + channelName + ", programName:" + programName);

        boolean success = initialStatus(Constants.ETC0105, runMode, channelName, channelNumber, programName);

        LOG.message("initialStatusForRemoteToIF(" + Constants.ETC0105
                    + "), result=" + success);

        return success;
    }

    public boolean requestHttpPost(Hashtable hashtable) {
        return requestHttpPost(hashtable, Constants.SERVER_PORT);
    }

    /**
     * 메가 TV 동기화 요청 (STBAgent -> STB I/F Server)
     */
    private boolean syncToIF() {

        LOG.message("syncToIF");

        Hashtable data = new Hashtable();
        data.put(Constants.CMD, Constants.ETC0301);
        data.put(Constants.SAID, com.kt.remotecontrol.interlock.ProxyManager.otherHandler().getSAID());
        data.put(Constants.STB_IP, com.kt.remotecontrol.interlock.ProxyManager.otherHandler().getIP());
        data.put(Constants.STB_TYPE, Constants.STB_TYPE_WEB); // KTKIDSCARE-14
        data.put(Constants.PRD_CD, com.kt.remotecontrol.interlock.ProxyManager.otherHandler().getProductCode());
        data.put(Constants.BQ_CD, com.kt.remotecontrol.interlock.ProxyManager.otherHandler().getBouquetID());

        return requestHttpPost(data);
    }

    private boolean initialStatus(String command, int runMode, String channelName, String channelNumber, String programName) {
        String runModeValue = Integer.toString(runMode);

        Hashtable data = new Hashtable();
        data.put(Constants.CMD, command);
        data.put(Constants.SAID, ProxyManager.otherHandler().getSAID());
        data.put(Constants.RUN_MODE, runModeValue);
        data.put(Constants.ID, channelNumber);

        String channelInfo = getChannelInfo(runModeValue, channelName, programName);
        if (channelInfo != null) {
            data.put(Constants.NAME, channelInfo);
        }

        return requestHttpPost(data);
    }

    private String getChannelInfo(String runMode, String channelName, String programName) {

        if (channelName == null) {
            return null;
        }

        String channelInfo = null;

        if (Constants.RUN_MODE_CHANNEL.equals(runMode)) { // 채널 (채널명 있는 경우만 보냄)
            if (programName == null) {
                programName = CharConstant.CHAR_SPACE; // 제어서버 요청사항
            }
            channelInfo = channelName + CharConstant.CHAR_VERTICAL + programName;
        } else if (Constants.RUN_MODE_DATA_SERVICE.equals(runMode)) { // 양방향
            channelInfo = channelName; // 양방향 서비스 이름
        }

        return channelInfo;
    }

    private boolean requestHttpPost(Hashtable hashtable, int port) {

        String command = (String) hashtable.get(Constants.CMD);
        String spec = url + CharConstant.CHAR_COLON + port + service;
        String data = convertPostParameter(hashtable);

        ArrayList result = new HttpRequest().post(spec, data);

        return isSuccess(result, command);
    }

    private String convertPostParameter(Hashtable hashtable) {
        StringBuffer sb = new StringBuffer();
        Enumeration en = hashtable.keys();
        Object key;

        while (en.hasMoreElements()) {
            key = en.nextElement();

            sb.append(key.toString()).append(CharConstant.CHAR_EQUAL);
            sb.append(hashtable.get(key).toString());

            if (en.hasMoreElements()) {
                sb.append(CharConstant.CHAR_AMPERSAND);
            }
        }

        String data = sb.toString();

        LOG.message("Post Data=[" + data + "]");

        return data;
    }

    /**
     * I/F 서버로 요청에 대한 응답을 분석한다.
     */
    private boolean isSuccess(ArrayList resultList, String command) {

        LOG.message("isSuccess");

        if (resultList == null || resultList.size() <= 0) {
            LOG.message("isSuccess(Fail), resultList is null, or resultList size = 0");
            return false;
        }

        String response = (String) resultList.get(0);
        StringTokenizer st = new StringTokenizer(response, CharConstant.CHAR_CARET);

        if (st.countTokens() != 2) {
            LOG.message("isSuccess(Fail), Token is not 2, response="
                        + response);
            return false;
        }

        String cmd = st.nextToken();
        String code = st.nextToken();
        boolean isSuccess = cmd.equalsIgnoreCase(command) && code.equalsIgnoreCase(ErrorCode.C000);

        LOG.message("isSuccess(" + (isSuccess ? "Success" : "Fail")
                    + "), CMD=" + cmd + ", ErrorCode=" + code + ", Message=" + ErrorCode.getErrorMessage(code));

        return isSuccess;
    }
}
