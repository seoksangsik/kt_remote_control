package com.kt.remotecontrol.model;

import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Log;

import java.util.Calendar;

public class NotifySetup {

    private static final Log LOG = new Log("NotifySetup");

    private boolean isNotifyOn;
    private String startTime, endTime;

    public void setValue(boolean isNotifyOn, String startTime, String endTime) {
        LOG.message("setValue, notify=" + isNotifyOn + ", start=" + startTime + ", end=" + endTime);

        this.isNotifyOn = isNotifyOn;

        if (!isNotifyOn) {
            return;
        }

        try {
            this.startTime = startTime;
            this.endTime = endTime;

            Integer.parseInt(startTime);
            Integer.parseInt(endTime);
        } catch (NumberFormatException e) {
            LOG.message("setValue, number format exception, e=" + e);

            this.isNotifyOn = false;
            throw new RuntimeException("NumberFormatException occurred");
        }
    }

    public String getResult() {
        StringBuffer result = new StringBuffer();
        String startTime = "0000";
        String endTime = "0000";

        if (isNotifyOn) {
            result.append("1");
            startTime = this.startTime;
            endTime = this.endTime;
        } else {
            result.append("0");
        }
        result.append(CharConstant.CHAR_CARET);
        result.append(startTime).append(CharConstant.CHAR_CARET);
        result.append(endTime);

        return result.toString();
    }

    /**
     * 현재가 Notify 할 수 있는 시간대인가?
     **/
    public boolean isCurrentInNotifyTime() {
        Calendar startCalendar = Calendar.getInstance();
        Calendar endCalendar = Calendar.getInstance();

        try {
            int startHour = Integer.parseInt(startTime.substring(0, 2));
            int startMinutes = Integer.parseInt(startTime.substring(2, 4));
            startCalendar.set(Calendar.HOUR_OF_DAY, startHour);
            startCalendar.set(Calendar.MINUTE, startMinutes);

            int endHour = Integer.parseInt(endTime.substring(0, 2));
            int endMinutes = Integer.parseInt(endTime.substring(2, 4));
            endCalendar.set(Calendar.HOUR_OF_DAY, endHour);
            endCalendar.set(Calendar.MINUTE, endMinutes);

        } catch (NumberFormatException e) {
            LOG.message("isCurrentInNotifyTime, format error, return false");

            return false;
        }

        if (startCalendar.after(endCalendar)) {
            endCalendar.add(Calendar.DATE, 1); // 다음날로 옮긴다
        }

        Calendar current = Calendar.getInstance();
        boolean result = current.after(startCalendar) && current.before(endCalendar);

        LOG.message("isCurrentInNotifyTime, result=" + result
                    + ", start=" + startCalendar + ", end=" + endCalendar + ", current=" + current);

        return result;
    }
}
