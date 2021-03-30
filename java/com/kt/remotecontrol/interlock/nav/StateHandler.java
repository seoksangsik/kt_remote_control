package com.kt.remotecontrol.interlock.nav;

import com.kt.navsuite.core.STBState;
import com.kt.navsuite.core.State;

public interface StateHandler {
    public final int STANDBY = State.STANDBY;
    public final int AV_WATCHING = State.AV_WATCHING;
    public final int DATA_SERVICE = State.DATA_SERVICE;

    public final int STB_STANDBY = STBState.STANDBY;
    public final int STB_RUNNING = STBState.RUNNING;

    public void addSTBStateListener();
    public void removeSTBStateListener();

    public int getState();
    public int getSTBState();

    public boolean changeStateToAVWatching();
    public boolean changeStateToStandby();

    public void toRunning();
}
