package com.kt.remotecontrol.model;

import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.manager.ServiceChangeManager;

import java.util.Calendar;

public class ChangeThread extends Thread {

    private static final Log LOG = new Log("ChangeThread");

    private int sleepTime = 0;
    private String type, id, time;
    private boolean changeRestrictWatchTime;

    public ChangeThread(String type, String id, boolean changeRestrictWatchTime) {
        this(type, id,null, changeRestrictWatchTime);
    }

    public ChangeThread(String type, String id, String time, boolean changeRestrictWatchTime) {
        this.type = type;
        this.id = id;
        this.time = time;
        this.sleepTime = getSleepTime(time);
        this.changeRestrictWatchTime = changeRestrictWatchTime;
    }

    public String getType() {
        return type;
    }

    public String getID() {
        return id;
    }

    public String getTime() {
        return time;
    }

    public void run() {

        LOG.message("[ChangeThread] run, sleep=" + sleepTime + ", type=" + type
                    + ", id=" + id);

        try {
            sleepDuringSleepTime();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return ;
        }

        boolean delayStartVOD = processLimitedWatchingTime();
        boolean isValidID = isValidID(id);
        if (isValidID) {
            ServiceChangeManager.getInstance().setChangeValue(type, id);
        }

        if (ProxyManager.navigator().isStandby()) { // 꺼져있으면 켜준다.
            LOG.message("[ChangeThread] run, STANDBY state!");

            delayStartVOD = ProxyManager.stateHandler().changeStateToAVWatching();
            ServiceChangeManager.getInstance().setDelayStartVOD(delayStartVOD);
        } else if (isValidID) {
            LOG.message("[ChangeThread] run, valid id(" + id + ") and not STANDBY state!");

            ServiceChangeManager.getInstance().changeService(delayStartVOD);
        }
    }

    private int getSleepTime(String time) {
        if (time == null || time.length() != 4) {
            return 0;
        }

        return getSleepSec(time);
    }

    private void sleepDuringSleepTime() throws InterruptedException {
        if (sleepTime <= 0) {
            return ;
        }

        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            LOG.message("[ChangeThread] run, InterruptedException.");

            throw e;
        }
    }

    private boolean processLimitedWatchingTime() {
        if (!changeRestrictWatchTime) {
            return false;
        }

        boolean isLimitedWatchingTime = ProxyManager.otherHandler().isLimitedWatchingTime();
        LOG.message("[ChangeThread] processLimitedWatchingTime, isLimitedWatchingTime="
                    + isLimitedWatchingTime + ", changeRestrictWatchTime=" + changeRestrictWatchTime);

        if (isLimitedWatchingTime) { // 잠겨있는 경우 풀어준다.
            return ProxyManager.navigator().cancelAndClearLimitedWatchingTime();
        }

        return false;
    }

    private boolean isValidID(String id) {
        return id != null && id.length() > 0;
    }

    private int getSleepSec(String time) {
        int targetHH = Integer.parseInt(time.substring(0, 2));
        int targetMM = Integer.parseInt(time.substring(2, 4));

        LOG.message("[Util] getSleepSec, targetHH:" + targetHH + ", targetMM:" + targetMM);

        Calendar currentCalendar = Calendar.getInstance();
        Calendar targetCalendar = Calendar.getInstance();

        LOG.message("[Util] getSleepSec, currentCal:" + currentCalendar.getTime());

        targetCalendar.set(Calendar.HOUR_OF_DAY, targetHH);
        targetCalendar.set(Calendar.MINUTE, targetMM);

        LOG.message("[Util] getSleepSec, targetCal:" + targetCalendar.getTime());

        long targetTimeMillis = targetCalendar.getTimeInMillis();
        long curTimeMillis = currentCalendar.getTimeInMillis();
        if (targetTimeMillis < curTimeMillis) {
            targetCalendar.add(Calendar.DAY_OF_MONTH, 1);
            targetTimeMillis = targetCalendar.getTimeInMillis();

            LOG.message("[Util] getSleepSec, targetCal:" + targetCalendar.getTime());
        }

        long diff = targetTimeMillis - curTimeMillis;
        LOG.message("[Util] getSleepSec, diff=" + diff);

        return (int) diff;
    }
}
