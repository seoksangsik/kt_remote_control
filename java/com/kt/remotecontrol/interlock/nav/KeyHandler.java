package com.kt.remotecontrol.interlock.nav;

public interface KeyHandler {
    public boolean isMute();
    public boolean volumeUp();
    public boolean volumeDown();
    public boolean mute();
    public boolean sendKey(int keyCode);
    public boolean findRCU();
}
