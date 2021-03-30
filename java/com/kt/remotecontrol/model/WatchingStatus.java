package com.kt.remotecontrol.model;

import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.manager.StatusManager;

public class WatchingStatus {
    private int watchingStatus;
    private String id, name, programName, channelType;
    private String FIRST_DEPTH = CharConstant.CHAR_CARET;
    private String SECOND_DEPTH = CharConstant.CHAR_VERTICAL;

    public WatchingStatus(int watchingStatus) {
        this.watchingStatus = watchingStatus;
    }

    public WatchingStatus(int watchingStatus, int id, String name) {
        this(watchingStatus, String.valueOf(id), name);
    }

    public WatchingStatus(int watchingStatus, String id, String name) {
        this(watchingStatus);
        this.id = id;
        this.name = name;
    }

    public WatchingStatus(int watchingStatus, int id, String name, String programName, boolean isSkylife) {
        this(watchingStatus, id, name);
        setProgramName(programName);
        setChannelType(isSkylife);
    }

    public String getResult() {
        String result = String.valueOf(watchingStatus);

        if (watchingStatus == StatusManager.WS_IDLE) {
            return result;
        }

        result += FIRST_DEPTH + id;
        String name = this.name; // KTKIDSCARE-57

        if (name == null) {
            return result;
        }

        result += FIRST_DEPTH + name;

        if (programName != null) {
            result += SECOND_DEPTH + programName;
            result += SECOND_DEPTH + channelType;
        }

        return result;
    }

    public String toString() {
        return "watchingState=" + watchingStatus + ", id=" + id + ", name=" + name + ", programName="
                + programName;
    }

    private void setProgramName(String programName) {
        if (programName == null) {
            this.programName = " "; // 프로그램명 없으면 공백문자 (제어서버요청)
        } else {
            this.programName = programName;
        }
    }

    private void setChannelType(boolean isSkylife) {
        channelType = isSkylife ? "OTS" : "OTV";
    }
}
