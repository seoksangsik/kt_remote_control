package com.kt.remotecontrol.interlock.nav.web;

import com.kt.navsuite.core.STBState;
import com.kt.navsuite.core.STBStateListener;
import com.kt.navsuite.core.STBStateManager;
import com.kt.navsuite.core.State;
import com.kt.navsuite.core.StateManager;
import com.kt.navsuite.ui.navigator.KidsCareControl;
import com.kt.remotecontrol.RemoteControlAgent;
import com.kt.remotecontrol.interlock.nav.StateHandler;
import com.kt.remotecontrol.manager.StatusManager;
import com.kt.remotecontrol.util.Log;

public class StateControl implements StateHandler, STBStateListener {

    private static final Log LOG = new Log("StateControl");

    private StateManager stateManager;
    private STBStateManager stbStateManager;
    private KidsCareControl kidsCareControl;

    public StateControl() {
        stateManager = StateManager.getInstance();
        stbStateManager = STBStateManager.getInstance();
    }

    public void addSTBStateListener() {
        stbStateManager.addListener(this);
    }

    public void removeSTBStateListener() {
        stbStateManager.removeListener(this);
    }

    public int getState() {
        return stateManager.getState();
    }

    public int getSTBState() {
        return stbStateManager.getState();
    }

    public boolean changeStateToAVWatching() {
        return changeState(StateHandler.AV_WATCHING);
    }

    public boolean changeStateToStandby() {
        return changeState(StateHandler.STANDBY);
    }

    private boolean changeState(int state) {
        int currentState = getState();

        if (currentState == state) {
            LOG.message("[StateControl] changeState : it's same State(" + currentState + ")");
            return false;
        }

        if (LOG.INCLUDE) {
            if (state == State.AV_WATCHING) {
                LOG.message("[StateControl] changeState : AV_WATCHING");
            } else if (state == State.STANDBY) {
                LOG.message("[StateControl] changeState : STANDBY");
            }
        }

        if (kidsCareControl == null) { // late instance
            kidsCareControl = KidsCareControl.getInstance();
        }

        kidsCareControl.changeState(state, -1);

        return true;
    }

    public void stateChanged(int newState) {
        LOG.message("[StateControl] stateChanged, newState=" + newState);

        if (newState == STBState.STANDBY) {
            LOG.message("[StateControl] stateChanged, STBState.STANDBY");

            StatusManager.getInstance().toStandby();
        } else if (newState == STBState.RUNNING) {
            LOG.message("[StateControl] stateChanged, STBState.RUNNING");

            toRunning();
        }
    }

    public void toRunning() {
        if (!RemoteControlAgent.getInstance().isStartedServer()) {
            return ;
        }

        StatusManager.getInstance().toRunning();
    }
}
