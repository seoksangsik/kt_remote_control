package com.kt.remotecontrol.manager;

import com.kt.remotecontrol.model.ChangeThread;
import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.util.Util;
import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.interlock.app.HomePortal;

public class ServiceChangeManager {

    private static final Log LOG = new Log("ServiceChangeManager");

    private static ServiceChangeManager instance = new ServiceChangeManager();

    private ChangeThread reservationThread = null;
    private boolean delayStartVOD;
    private String type, id;

    private ServiceChangeManager() {
    }

    public static ServiceChangeManager getInstance() {
        return instance;
    }

    /**
     * 즉시전환 (채널, VOD, 양방향)
     * @param type 1: 채널, 2: VOD, 3: 양방향 서비스
     * @param id serviceID, assetId, Locator(양방향)
     * @param changeRestrictWatchTime 시청시간제한 설정변경 여부
     */
    public void changeRightNow(String type, String id, boolean changeRestrictWatchTime) {
        new ChangeThread(type, id, changeRestrictWatchTime).start();
    }

    /**
     * 예약전환 (채널, VOD, 양방향)
     * @param type 1: 채널, 2: VOD, 3: 양방향 서비스
     * @param id serviceID, assetId, Locator(양방향)
     * @param time 예약전환 시간
     * @param changeRestrictWatchTime 시청시간제한 설정변경 여부
     */
    public void changeReservation(String type, String id, String time, boolean changeRestrictWatchTime) {
        cancelReservation();

        reservationThread = new ChangeThread(type, id, time, changeRestrictWatchTime);
        reservationThread.start();
    }

    /**
     * 예약취소
     *
     */
    public void cancelReservation() {
        LOG.message("cancelReservation");

        if (invalidChangeThread()) {
            return ;
        }

        LOG.message("cancelReservation, reservationThread exist and interrupt!");

        reservationThread.interrupt();
        reservationThread = null;
    }

    public String getReservationResult() {
        if (invalidChangeThread() || reservationThread.isInterrupted()) {
            return Constants.STATE_CLEAR; // 해제
        }

        String time = reservationThread.getTime();
        String type = reservationThread.getType();
        String id = reservationThread.getID();

        return Constants.STATE_SET + CharConstant.CHAR_CARET + time + CharConstant.CHAR_CARET + type
                + CharConstant.CHAR_CARET + id;
    }

    public void setChangeValue(String type, String id) {
        this.type = type;
        this.id = id;
    }

    public void setDelayStartVOD(boolean delayStartVOD) {
        this.delayStartVOD = delayStartVOD;
    }

    public void changeService(boolean needDelayStartVOD) {
        setDelayStartVOD(needDelayStartVOD);
        changeServiceByRunMode(type, id);
    }

    public void notifyChannelEvent() {
        if (invalidChangeValue()) {
            return;
        }

        changeServiceByRunMode(type, id);
    }

    private boolean invalidChangeThread() {
        return reservationThread == null || !reservationThread.isAlive();
    }

    private void changeServiceByRunMode(String runMode, String id) {

        clearChangeValue();

        LOG.message("changeService(RunMode=" + runMode + "), id=" + id);

        boolean result = false;

        if (Constants.RUN_MODE_CHANNEL.equals(runMode)) { // 채널
            result = ProxyManager.navigator().changeChannel(id);
        } else if (Constants.RUN_MODE_VOD.equals(runMode)) { // vod
            result = watchVOD(id, delayStartVOD);
        } else if (Constants.RUN_MODE_DATA_SERVICE.equals(runMode)) { // 양방향
            result = ProxyManager.navigator().launchApp(id);
        }

        LOG.message("changeService(RunMode=" + runMode + "), id=" + id + ", result=" + result);
    }

    private boolean invalidChangeValue() {
        return type == null || id == null;
    }

    private void clearChangeValue() {
        type = null;
        id = null;
        delayStartVOD = false;
    }

    private boolean watchVOD(String id, boolean delayStartVOD) {
        LOG.message("watchVOD, id=" + id + ", delayStartVOD=" + delayStartVOD);

        // 데이타 채널일 경우 VOD 바로 전환하면 문제 많아서 sleep : 전환 실패 or 이어보기 팝업 포커스 잃음 등등...
        if (ProxyManager.navigator().isDataService()) { // workaournd : 먼저 가이드 채널로 튜닝을 한 후 VOD 전환하도록 한다.
            delayStartVOD = ProxyManager.navigator().changePromoChannel();
        }

        if (delayStartVOD && Util.hasErrorForSleep("watchVOD")) {
            return false;
        }

        return HomePortal.hp_watchContent(id);
    }

}
