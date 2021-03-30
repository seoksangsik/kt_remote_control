package com.kt.remotecontrol.model;

import com.kt.remotecontrol.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 *
 */
public class WatchRecorder {

    private static final Log LOG = new Log("WatchRecorder");

    private SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");

    private final int IDLE_TYPE = 0;
    private final long WATCHING_TIME = 10000L;

    // 시청내역누적을 위한 변수들
    private boolean isSkylifeChannel = false;
    private int type = IDLE_TYPE; // 1:channel, 2:VOD, 3:data service
    private int contentType;
    private String recordStartTime = null, id = null, name = null;
    private float vodPlayRate = 0.0f;


    public int getType() {
        return type;
    }

    /**
     * @return 채널:0, VOD:1, 양방향:2
     */
    public String getContentType() {
        return String.valueOf(contentType);
    }

    public String getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRecordStartTime() {
        return recordStartTime;
    }

    public String getRecordTime() {
        return format.format(Calendar.getInstance().getTime());
    }

    public boolean isSkylifeChannel() {
        return isSkylifeChannel;
    }

    public boolean enableNotifyWatchRecord() {
        if (type > IDLE_TYPE && id != null && recordStartTime != null
                && isValidTime()) {
            return true;
        }

        return false;
    }

    public void setWatchRecordByChannel(int type, int sid, String value, boolean isSkylifeChannel) {
        startWatchRecord(type, String.valueOf(sid), value, isSkylifeChannel);
    }

    public void setWatchRecordByDataChannel(int type, int sid, String name) {
        startWatchRecord(type, String.valueOf(sid), name, false);
    }

    public void startWatchRecordByVOD(int type, String id, String vodName) {
        startWatchRecord(type, id, vodName, false);
        setVodPlayRate(1.0f); // Vod 시작되면 playRate 를 1.0f 로 설정
    }

    public void clearWatchRecord() {
        type = IDLE_TYPE;
        recordStartTime = null;
        id = null;
        name = null;
        isSkylifeChannel = false;
    }

    /**
     * 시청내역누적만 run_mode가 다르다.
     * DNP때 이미 잘못되어 연동규격서 수정이 불가능하기 때문에 달리 처리한다.
     * (채널:1, VOD:2, 양방향:3) -> (채널:0, VOD:1, 양방향:2)
     */
    private void startWatchRecord(int type, String id, String name, boolean isSkylifeChannel) {

        this.type = type;
        contentType = type - 1;
        this.id = id;
        this.name = name;
        this.isSkylifeChannel = isSkylifeChannel;

        resetStartTime();

        LOG.message("startWatchRecord type:" + type + ", id:" + id
                    + ", name:" + name + ", startTime:" + recordStartTime);
    }

    public String getVodPlayRate() {
        if (vodPlayRate == 0.0f) {
            return "0";
        }

        return String.valueOf(vodPlayRate);
    }

    public void setVodPlayRate(float vodPlayRate) {
        this.vodPlayRate = vodPlayRate;
    }

    public void stopVOD() {
        setVodPlayRate(0.0f);
    }

    private boolean isValidTime() {
        return getStartTime() + WATCHING_TIME <= Calendar.getInstance().getTimeInMillis();
    }

    private void resetStartTime() {
        recordStartTime = getRecordTime();
    }

    private long getStartTime() {
        long startTime = 0l;

        if (recordStartTime == null || "".equals(recordStartTime)) {
            return startTime;
        }

        try {
            startTime = format.parse(recordStartTime).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return startTime;
    }
}
