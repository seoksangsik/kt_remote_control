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

import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.util.TimeConstant;
import com.kt.remotecontrol.interlock.ProxyManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

/**
 *
 */
public class ReserveManager implements TVTimerWentOffListener {

    private static final Log LOG = new Log("ReserveManager");

    private static ReserveManager instance = null;

    // 끄기 예약에서 사용
    private boolean isScheduled = false;
    private TVTimerSpec timerSpec = null;

    private String startTime, endTime;
    private String message, cellPhone, displayTime;
    private boolean changeRestrictWatchTime = true;
    private boolean turnOffStarted = false;

    private ReserveManager() {
        timerSpec = new TVTimerSpec();
        timerSpec.setAbsolute(true);
        timerSpec.setRepeat(false);
    }

    public static ReserveManager getInstance() {
        if (instance == null) {
            instance = new ReserveManager();
        }

        return instance;
    }

    // 끄기 예약
    public void reserveSTBTurnOff(String startTime, String endTime, String[] messages, boolean changeRestrictWatchTime) {

        Integer.parseInt(startTime);
        Integer.parseInt(endTime);

        this.startTime = startTime;
        this.endTime = endTime;
        setContent(messages);
        this.changeRestrictWatchTime = changeRestrictWatchTime;

        schedule(startTime);
    }

    public boolean isTurnOffStarted() {
        return turnOffStarted;
    }

    public void setTurnOffStarted() {
        turnOffStarted = true;
    }

    // 끄기 예약 해제
    public void cancelReserveSTBTurnOff() {
        LOG.message("cancelReserveSTBTurnOff");

        deschedule();
        turnOffStarted = false;
    }

    public String getTurnOffResult() {
        StringBuffer result = new StringBuffer();

        if (isScheduled) {
            result.append(Constants.STATE_SET);
        } else {
            result.append(Constants.STATE_CLEAR);
        }
        result.append(CharConstant.CHAR_CARET);
        result.append(getTurnOffStartTime());

        return result.toString();
    }

    private void schedule(String startTime) {
        LOG.message("schedule, startTime:" + startTime + ", isSchedulde:" + isScheduled);

        deschedule();

        if (isScheduled) {
            return;
        }

        try {
            timerSpec.setTime(getTimeToTodayDate(startTime));
            timerSpec.addTVTimerWentOffListener(this);
            TVTimer.getTimer().scheduleTimerSpec(timerSpec);
            isScheduled = true;
        } catch (TVTimerScheduleFailedException ex) {
            ex.printStackTrace();
        }
    }

    private void deschedule() {
        LOG.message("deschedule, isSchedulde:" + isScheduled);

        if (!isScheduled) {
            return;
        }

        TVTimer.getTimer().deschedule(timerSpec);
        timerSpec.removeTVTimerWentOffListener(this);
        isScheduled = false;
    }

    // 끄기 예약 시간이 됐을 때 호출됨
    public void timerWentOff(TVTimerWentOffEvent e) {
        LOG.message("timerWentOff, startTime=" + startTime);

        deschedule();

        new Thread() {
            public void run() {
                if (startTime == null) {
                    ProxyManager.stateHandler().changeStateToStandby();
                    return ;
                }

                if (message != null) {
                    PopupManager.getInstance().showPopup(message, cellPhone, displayTime);

                    try {
                        Thread.sleep(TimeConstant.SEVEN_SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }

                ProxyManager.stateHandler().changeStateToStandby();

                if (!changeRestrictWatchTime) {
                    return;
                }

                ProxyManager.navigator().setLimitedWatchingTime(true, startTime, endTime, false);
            }
        }.start();
    }

    private void setContent(String[] contents) {
        if (contents == null) {
            return;
        }

//        title = contents[2];
        message = contents[0];
        cellPhone = contents[1];
        displayTime = contents[3];
    }

    private String getTurnOffStartTime() {
        if (isScheduled && startTime != null) {
            return startTime;
        }

        return "0000";
    }

    private long getTimeToTodayDate(String time) {
        String today = getStringToday();
        Date date = stringToDate(today, time);
        return date.getTime();
    }

    private Date stringToDate(String strDate, String strTime) {
        String pattern = "yyyyMMdd HHmm";
        LOG.message("stringToDate : strDate=" + strDate + ", strTime=" + strTime);

        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        Date date = null;
        try {
            date = sdf.parse(strDate + " " + strTime);
        } catch (ParseException pe) {
            pe.printStackTrace();
            return date;
        }

        LOG.message("stringToDate : date = " + date);

        return date;
    }

    private String getStringToday() {
        String pattern = "yyyyMMdd";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(new Date());
    }
}
