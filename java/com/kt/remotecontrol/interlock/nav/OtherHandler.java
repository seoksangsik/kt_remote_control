package com.kt.remotecontrol.interlock.nav;

import java.util.Properties;

public interface OtherHandler {
    public boolean isOTS();
    public Properties getUSBProperties(String filename);

    public String getInfo(String key);
    public String getUseKidsMode();

    public String launchBrowser(String url, long timeout) throws InterruptedException;
    public void notifyLaunchBrowser(int result);

    public boolean isFullBrowserState();

    public int getParentalRating();
    public boolean setParentalRating(int rate, String passwd);
    public boolean checkAdultPIN(String pin);
    public boolean changeAdultPIN(String oldPin, String newPin);

    public void sendKeywordToDialog(String keyword);

    public void setVersion(String version);

    public boolean isLimitedWatchingTime();
    public void cancelLimitedWatchingTime();

    public boolean isDisplayAdultMenu();
    public void setDisplayAdultMenu(boolean display);

    public String getSAID();
    public boolean isValidSAID(String said);
    public String getIP();
    public String getBouquetID();
    public String getProductCode();
    public String getMacAddress();
    public boolean isSubscriber();

    public void setClipboardContents(String argument);

    public String getLimitedWatchingTime();
    public void setLimitedWatchingTime(String value);

    public boolean changeHDSPin(String oldPin, String newPin);
}
