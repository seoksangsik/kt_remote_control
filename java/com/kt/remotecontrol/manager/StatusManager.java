/*
 *  Copyright (c) 2004 Alticast Corp.
 *  All rights reserved. http://www.alticast.com/
 *
 *  This software is the confidential and proprietary information of
 *  Alticast Corp. ("Confidential Information"). You shall not
 *  disclose such Confidential Information and shall use it only in
 *  accordance with the terms of the license agreement you entered into
 *  with Alticast.
 */
package com.kt.remotecontrol.manager;

import com.kt.navsuite.core.Channel;
import com.kt.remotecontrol.model.NotifySetup;
import com.kt.remotecontrol.model.WatchRecorder;
import com.kt.remotecontrol.model.WatchingStatus;
import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.WorkingConfig;
import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.interlock.nav.Navigator;
import com.kt.remotecontrol.service.request.RequestService;

import org.dvb.si.SIService;

import java.util.HashMap;
import java.util.Hashtable;

public class StatusManager {

    private static final Log LOG = new Log("StatusManager");

    public static final int WS_IDLE = 0;
    public static final int WS_CHANNEL = 1;
    public static final int WS_VOD = 2;
    public static final int WS_DATASERVICE = 3;
    public static final int WS_FULL_BROWSER = 4;

    /**
     * I/F Server 와 싱크 되었는가
     */
    private final int SYNC_NO = 0;
    private final int SYNC_TRYING = 1;
    private final int SYNC_OK = 2;

    private static StatusManager instance = new StatusManager();

    private int watchingStatus = WS_IDLE; // 0:IDLE, 1:채널, 2:VOD, 3:양방향서비스
    private int syncStatus = SYNC_NO; // IF server와 sync 상태.
    private boolean notifyInitialStatus = false; // 최초 상태 알림 플래그. (처음 부팅시 1번 알려주기 위한 플래그)
    private boolean skipNotifyInitialStatus = false;  // 최초 상태 알림 무시 flag

    private RequestService request;
    private WatchRecorder watchRecorder;
    private NotifySetup notifySetup;

    private StatusManager() {

        String url = "http://" + WorkingConfig.REQUEST_SERVER;
        boolean mustSync = WorkingConfig.MUST_SYNC_SUCCESS;

        request = new RequestService(url, Constants.REQUEST_SERVICE, mustSync);
        watchRecorder = new WatchRecorder();
        notifySetup = new NotifySetup();
    }

    public static StatusManager getInstance() {
        return instance;
    }

    public void setNotify(boolean notify, String startNotifyTime, String endNotifyTime) {

        LOG.message("setNotify, notify=" + notify + ", start="
                    + startNotifyTime + ", end=" + endNotifyTime);

        notifySetup.setValue(notify, startNotifyTime, endNotifyTime);
    }

    public String getNotifySetupResult() {
        return notifySetup.getResult();
    }

    /**
     * 현재 시청 상태 얻기
     * @return value
     */
    public String getWatchingStatus() {
        setWatchingStateWhenFullBrowser();

        WatchingStatus aWatchingStatus;
        Channel channel;

        switch (watchingStatus) {
            case WS_IDLE : // IDLE
                aWatchingStatus = new WatchingStatus(watchingStatus);
                break;
            case WS_CHANNEL : // 채널
                channel = ProxyManager.channelHandler().getCurrentChannel();
                String name = channel.getName();
                String programName = null;

                if (name != null) { // 채널명 없으면 프로그램명도 가져올 필요없다.
                    programName = getCurrentProgramName(channel, "WatchingState");
                }

               aWatchingStatus = new WatchingStatus(watchingStatus, channel.getNumber(), name, programName, channel.isSkylife());
                break;
            case WS_DATASERVICE : // 양방향 서비스
                channel = ProxyManager.channelHandler().getCurrentChannel();
                aWatchingStatus = new WatchingStatus(watchingStatus, channel.getNumber(), channel.getName());
                break;
            case WS_VOD : // VOD
                aWatchingStatus = new WatchingStatus(watchingStatus, watchRecorder.getID(), watchRecorder.getName());
                break;
            case WS_FULL_BROWSER : // Full Browser
                aWatchingStatus = new WatchingStatus(watchingStatus, "Browser", "Full Browser");
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + watchingStatus);
        }

        LOG.message("getWatchingStates, watchingStatus:" + aWatchingStatus.toString());

        return aWatchingStatus.getResult();
    }

    public void setWatchingStatus(int status) {

        LOG.message("setWatchingStatus:" + status);

        if (status >= WS_IDLE && status <= WS_FULL_BROWSER) {
            watchingStatus = status;
        }
    }

    public void toStandby() {
        notifyAndClearWatchRecord();
        resetNotifyInitialStatus();
    }

    public void toRunning() {

        if (!isNotSyncNO()) {
            return ;
        }

        if (!ProxyManager.otherHandler().isSubscriber()) { // 키즈케어 서비스 가입자 체크.
            LOG.message("startServiceSynchronize, doesn't service subscriber");
            return ;
        }

        setSyncStatus(SYNC_TRYING);

        request.startServiceSync();
    }

    public void requestHttpPost(HashMap hashMap) {
        Hashtable hashtable = new Hashtable(hashMap);
        request.requestHttpPost(hashtable);
    }

    public void completedSync() {
        setSyncStatus(SYNC_OK);
    }

    public void clearSync() {
        setSyncStatus(SYNC_NO);
    }

    public boolean isNotSyncOK() {
        return !isSyncOK();
    }

    public void skipNotifyInitialStatus() {
        setSkipNotifyInitialStatus(true);
    }

    public boolean isChannelOrIDLEState() {
        return watchingStatus == WS_CHANNEL || watchingStatus == WS_IDLE;
    }

    public String getVODPlayResult() {
        if (watchingStatus != WS_VOD) {
            return "-1";
        }

        return watchRecorder.getVodPlayRate();
    }

    public void setVODPlayRate(float rate) {
        watchRecorder.setVodPlayRate(rate);
    }

    public synchronized void changeVODStatus(boolean isPlay, String id, String vodName) {

        LOG.message("changeVODStatus isPlay:" + isPlay + ", id:" + id
                    + ", vodName:" + vodName);

        try {
            notifyWatchRecordWhenValid();

            if (isPlay) {
                processVODStart(id, vodName);
            } else {
                processVODStop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setSyncStatus(int syncStatus) {
        LOG.message("setSyncStatus, syncStatus=" + syncStatus);

        this.syncStatus = syncStatus;
    }

    private void setWatchingStateWhenFullBrowser() {
        if (!ProxyManager.otherHandler().isFullBrowserState()) {
            return ;
        }

        setWatchingStatus(WS_FULL_BROWSER);
    }

    private void resetNotifyInitialStatus() {

        LOG.message("resetNotifyInitialStatus");

        notifyInitialStatus = false;
    }

    private void processVODStart(String id, String name) {

        setWatchingStatus(WS_VOD);

        if (isNotNotifyAndSubscriber()) { // 최초 상태 알림.
            requestInitialStatusToIF(WS_VOD, id, null, null, false);
        }

        watchRecorder.startWatchRecordByVOD(WS_VOD, id, name);

        requestInitialStatusForRemoteWhenNotNotifyAtNotifyTime(WS_VOD, id);
    }

    private void processVODStop() {
        setWatchingStatus(WS_IDLE);

        watchRecorder.stopVOD();

        if (watchRecorder.getType() == WS_VOD) { // VOD 시청누적중이면 clear
            watchRecorder.clearWatchRecord();
        }
    }

    /**
     * 상태(VOD)가 바뀔 때 현재 시청내역 누적중이라면 누적 내용을 제어서버로 보낸다.
     */
    private void notifyWatchRecordWhenValid() {

        if (!isNotifyAndSubscriber()) {
            return ;
        }

        if (checkRequestWatchRecord()) {
            request.watchRecordToIF(watchRecorder);
        }
    }

    /**
     * (시청내역을 누적 중 이었을때만) 현재까지의 시청내역누적을 보낸다
    */
    private boolean checkRequestWatchRecord() {
        if (isNotSyncOK()) {
            return false;
        }

        String id = watchRecorder.getID();
        if (ProxyManager.navigator().isPromoChannel(id)) {
            LOG.message("requestWatchRecordToIF this request is Promo Record!!  Don't send request!!");
            return false;
        }

        return watchRecorder.enableNotifyWatchRecord();
    }

    /**
     * 채널 체인지 이벤트 받아서 시청누적 보내고.. 일련의 작업을 하는데,
     * 그 시간이 오래 걸려서 다음 채널 이벤트가 무시되는 현상이 있음.
     * 쓰레드로 빼서 처리하도록 수정.
     *
     * @param eventType
     * @param channel
     */
    public void processChannelEvent(final int eventType, final Channel channel) {

        if (eventType == Navigator.REQUESTED || eventType == Navigator.USER_BLOCKED) {
            ServiceChangeManager.getInstance().notifyChannelEvent();
        }

        if (eventType == Navigator.SUCCEEDED) {
            processSucceededEvent(channel);
        } else if (eventType == Navigator.USER_BLOCKED) {
            processUserBlockedEvent(channel);
        } else if (eventType == Navigator.FAILED) {
            processFailedEvent();
        }
    }

    private void processSucceededEvent(Channel channel) {

        LOG.message("ChannelSelectionEvent.SUCCEEDED");

        notifyWatchRecordWhenValid();

        if (channel == null) {
            LOG.message("Channel into the event is null!");
            return;
        }

        try {
            if (channel.isDataService()) { // 양방향
                processDataChannel(channel);
            } else { // 채널
                processChannel(channel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processDataChannel(Channel channel) {

        int number = channel.getNumber();
        String name = channel.getName();

        LOG.message("notifyChannelEvent(DataChannel), number:" + number + ", name:" + name);

        setWatchingStatus(WS_DATASERVICE);

        requestInitialStatusToIFWhenNotNotifyOnSubscriber(WS_DATASERVICE, channel);

        int sid = channel.getSIService().getServiceID();
        watchRecorder.setWatchRecordByDataChannel(WS_DATASERVICE, sid, name);

        requestInitialStatusForRemoteWhenNotNotifyAtNotifyTime(WS_DATASERVICE, channel, null);
    }

    private void processChannel(Channel channel) {

        int number = channel.getNumber();
        String name = channel.getName();

        LOG.message("notifyChannelEvent(Channel), number:" + number + ", name:" + name);

        setWatchingStatus(WS_CHANNEL);

        SIService siService = channel.getSIService();

        if (siService == null) { // 아날로그 채널. (채널번호만 사용, 시청누적 없음)
            watchRecorder.clearWatchRecord();
            return ;
        }

        int sid = siService.getServiceID();
        String value = number + CharConstant.CHAR_VERTICAL + name;

        watchRecorder.setWatchRecordByChannel(WS_CHANNEL, sid, value, channel.isSkylife());

        String programName = requestInitialStatusToIFWhenNotNotifyOnSubscriber(WS_CHANNEL, channel);

        requestInitialStatusForRemoteWhenNotNotifyAtNotifyTime(WS_CHANNEL, channel, programName);
    }

    private void processUserBlockedEvent(Channel channel) {

        LOG.message("ChannelSelectionEvent.USER_BLOCKED");

        notifyAndClearWatchRecord();

        if (channel == null) {
            LOG.message("Channel into the event is null!");
            return;
        }

        setWatchingStatus(channel.isDataService() ? WS_DATASERVICE : WS_CHANNEL);
    }

    private void processFailedEvent() {

        LOG.message("ChannelSelectionEvent.FAILED");

        notifyAndClearWatchRecord();
        setWatchingStatus(WS_IDLE); // 채널 튜닝 실패하면 IDLE 처리.
    }

    private boolean isSyncOK() {
        return syncStatus == SYNC_OK;
    }

    private boolean isNotSyncNO() {
        return syncStatus != SYNC_NO;
    }

    private boolean isNotNotifyAndSubscriber() {
        return !notifyInitialStatus && ProxyManager.otherHandler().isSubscriber();
    }

    private boolean isNotifyAndSubscriber() {
        return notifyInitialStatus && ProxyManager.otherHandler().isSubscriber();
    }

    private void requestInitialStatusForRemoteWhenNotNotifyAtNotifyTime(int type, Channel channel, String programName) {
        if (!isNotNotifyAndNotifyTime()) {
            return ;
        }

        LOG.message("also request to remote appl server");

        String number = String.valueOf(channel.getNumber());
        String name = channel.getName();
        boolean isPromo = false;

        if (type == WS_CHANNEL) {
            isPromo = channel.equals(ProxyManager.channelHandler().getPromoChannel());

            if (programName == null) {
                programName = getCurrentProgramName(channel, "NotifyTime");
            }
        }

        requestInitialStatusForRemoteToIF(type, number, name, programName, isPromo);
    }

    private String getCurrentProgramName(Channel channel, String caller) {

        LOG.message("getCurrentProgramName(" + caller + ")");

        String programName = ProxyManager.getCurrentProgramName(channel);

        LOG.message("getCurrentProgramName(" + caller + ") name="
                    + programName);

        return programName;
    }

    private void requestInitialStatusForRemoteWhenNotNotifyAtNotifyTime(int type, String id) {
        if (!isNotNotifyAndNotifyTime()) { // XXX : notifyFirstStatus 처리를 한번 더 호출해야 하나?
            return;
        }

        LOG.message("changeVODStatus, also request to remote appl server");

        requestInitialStatusForRemoteToIF(type, id, null, null, false);
    }

    private boolean isNotNotifyAndNotifyTime() {
        return !notifyInitialStatus && notifySetup.isCurrentInNotifyTime();
    }

    private void notifyAndClearWatchRecord() {
        notifyWatchRecordWhenValid();
        watchRecorder.clearWatchRecord();
    }

    private String requestInitialStatusToIFWhenNotNotifyOnSubscriber(int type, Channel channel) {
        if (!isNotNotifyAndSubscriber()) { // 최초 상태 알림.
            return null;
        }

        String number = String.valueOf(channel.getNumber());
        String name = channel.getName();
        boolean isPromo = false;
        String programName = null;

        if (type == WS_CHANNEL) {
            isPromo = channel.equals(ProxyManager.channelHandler().getPromoChannel());
            programName = getCurrentProgramName(channel, "First");
        }

        requestInitialStatusToIF(type, number, name, programName, isPromo);

        return programName;
    }

    private void requestInitialStatusToIF(int mode, String id, String name, String programName, boolean isPromo) {

        if (resetInitialStatusWhenIgnoreStatus(isPromo)) {
            return ;
        }

        boolean result = request.initialStatusToIF(mode, id, name, programName);
        setNotifyInitialStatus(result);
    }

    private void requestInitialStatusForRemoteToIF(int mode, String id, String name, String programName,
                                                boolean isPromo) {

        if (resetInitialStatusWhenIgnoreStatus(isPromo)) {
            return ;
        }

        boolean result = request.initialStatusForRemoteToIF(mode, id, name, programName);
        setNotifyInitialStatus(result);
    }

    private boolean resetInitialStatusWhenIgnoreStatus(boolean isPromo) {
        if (ignoreInitialStatus(isPromo)) {
            setNotifyInitialStatus(false);
            return true;
        }

        return false;
    }

    private boolean ignoreInitialStatus(boolean isPromo) {
        if (isNotSyncOK()) {
            return true;
        }

        if (skipNotifyInitialStatus) {
            setSkipNotifyInitialStatus(false);

            LOG.message("requestInformFirstStateToIF - SKIP : skipNotifyFirstStatus");

            return true; // 최초 상태 알림 무시
        }

        if (isPromo) {
            LOG.message("requestInformFirstStateToIF - SKIP : promo channel");

            return true; // 가이드채널은 무시
        }

        return false;
    }

    private void setSkipNotifyInitialStatus(boolean skipNotifyInitialStatus) {
        LOG.message("setSkipNotifyInitialStatus, status=" + skipNotifyInitialStatus);

        this.skipNotifyInitialStatus = skipNotifyInitialStatus;
    }

    private void setNotifyInitialStatus(boolean notifyInitialStatus) {
        this.notifyInitialStatus = notifyInitialStatus;
    }
}
