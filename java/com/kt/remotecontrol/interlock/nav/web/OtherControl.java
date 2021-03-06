package com.kt.remotecontrol.interlock.nav.web;

import com.alticast.hds.HDSPinUpdatedEvent;
import com.alticast.navigator.qts.QTSController;
import com.alticast.rop.navigator.HDSPinUpdateInfImpl;
import com.alticast.rop.navigator.RExternalCollectionImpl;
import com.alticast.rop.navigator.RVoiceSearchInfImpl;
import com.alticast.tvcore.navigator.HDSPinUpdateListener;
import com.alticast.tvcore.navigator.PRControlManager;
import com.alticast.tvcore.navigator.STBInfoManager;
import com.kt.navsuite.core.kt.KTUserPreference;
import com.kt.navsuite.ui.navigator.RestrictionWatchTimeController;
import com.kt.remotecontrol.interlock.nav.OtherHandler;
import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.ErrorCode;
import com.kt.remotecontrol.util.KeyConstant;
import com.kt.remotecontrol.util.Log;
import com.kt.util.sysinfo.STBInformation;

import org.dvb.user.GeneralPreference;
import org.dvb.user.UserPreferenceManager;

import java.util.Properties;

public class OtherControl implements OtherHandler, HDSPinUpdateListener {

    private static final Log LOG = new Log("OtherControl");

    private RExternalCollectionImpl externalCollection;
    private PRControlManager prControlManager;
    private RVoiceSearchInfImpl voiceSearch;
    private STBInfoManager stbInfoManager;
    private RestrictionWatchTimeController restrictionWatchTimeController;
    private STBInformation stbInformation;
    private KTUserPreference ktUserPreference;
    private HDSPinUpdateInfImpl hdsPinUpdateInfImpl;

    private boolean pinUpdatedResult = false;
    private String SAID = null;
    private String responseCTL1031;
    private Object WAIT_FOR_RESPONSE = new Object();

    public OtherControl() {
        externalCollection = RExternalCollectionImpl.getInstance();
        prControlManager = PRControlManager.getInstance();
        voiceSearch = RVoiceSearchInfImpl.getInstance();
        stbInfoManager = STBInfoManager.getInstance();
        restrictionWatchTimeController = RestrictionWatchTimeController.getInstance();
        stbInformation = STBInformation.getInstance();
        ktUserPreference = KTUserPreference.getInstance();
        hdsPinUpdateInfImpl = HDSPinUpdateInfImpl.getInstance();
    }

    public boolean isOTS() {
        return QTSController.OTS;
    }

    public Properties getUSBProperties(String filename) {
        return QTSController.getUSBProperties(filename);
    }

    public String getUseKidsMode() {
        return stbInfoManager.getUseKidsMode();
    }

    public String getInfo(String key) {
        return stbInfoManager.getInfo(key);
    }

    public synchronized String launchBrowser(String url, long timeout) throws InterruptedException {
        this.notifyAll();
        responseCTL1031 = null;

        externalCollection.launchBrowser("kidscare", url);

        this.wait(timeout);

        return responseCTL1031;
    }

    public synchronized void notifyLaunchBrowser(int result) {
        switch (result) {
            case 0:
                responseCTL1031 = ErrorCode.SUCCESS;
                break;
            case 417:
                responseCTL1031 =  ErrorCode.C417;
                break;
            case 418:
                responseCTL1031 = ErrorCode.C418;
                break;
        }

        this.notifyAll();
    }

    public boolean isFullBrowserState() {
        byte osdState = externalCollection.getOSDState();

        return osdState == RExternalCollectionImpl.FULL_BROWS;
    }

    public int getParentalRating() {
        int parentalRating = prControlManager.getParentalRating(); // KTKIDSCARE-56

        LOG.message("getParentalRating, parentalRating=" + parentalRating);

        return parentalRating;
    }

    public boolean setParentalRating(int rate, String passwd) {
        return prControlManager.changeParentalRating(rate, passwd);
    }

    public boolean checkAdultPIN(String pin) {
        LOG.message("checkAdultPIN");

        return prControlManager.verifyPIN(pin);
    }

    /**
     * HDS ?????????????????? PIN ?????? ????????? ????????????.
     * ??????????????? ????????? ?????? SamrtCard??? ?????? ????????????.
     * ??????, SmartCard ????????? ???????????? HDS ?????????????????? ????????? PIN ????????? ????????????.
     *
     * @param oldPin String
     * @param newPin String
     * @return boolean true(?????? ??????) or false(?????? ??????)
     */
    public boolean changeAdultPIN(String oldPin, String newPin) {
        LOG.message("[OtherControl] changeAdultPIN, passwd=[" + oldPin + "->" + newPin + "]");

        boolean result = changeHDSPin(oldPin, newPin); // HDS ?????? ?????? ??????
        LOG.message("[OtherControl] changeAdultPIN, change 'HDS' result=" + result);

        if (result) {
            result = prControlManager.changeAdultPIN(oldPin, newPin); // HDS ???????????? ???????????? ????????????????????? ????????????.
            LOG.message("[OtherControl] changeAdultPIN, change 'PRControlManager' result=" + result);

            if (!result) { // ??????????????? ???????????? ???????????? HDS ???????????? ???????????? ???. oldPin??? newPin??? ????????? hdsPinUpdate ?????????.
                result = changeHDSPin(newPin, oldPin);

                if (result) {
                    LOG.message("[OtherControl] changeAdultPIN, PIN rollback succeeded");
                } else {
                    LOG.error("[OtherControl] changeAdultPIN, PIN rollback failed");
                }
            }
        }

        LOG.message("changeAdultPIN, result=" + result);

        return result;
    }

    public void sendKeywordToDialog(String keyword) {
        LOG.message("sendKeywordToDialog, keyword=" + keyword);

        voiceSearch.sendKeywordByAISpeaker(keyword);
    }

    public void setVersion(String version) {
        stbInfoManager.setRemoteAgentVer(version);
    }

    /**
     * ???????????? ?????? ????????? ??????..
     * @return true or false
     */
    public boolean isDisplayAdultMenu() {
        LOG.message("[OtherControl] isAdultMenuDisplay");
        boolean result = false;
        try {
            String value = STBInfoManager.getInstance().getInfo(KeyConstant.UP_Adult_Menu_Display);
            result = Boolean.parseBoolean(value); // ????????? display ?????? (0:?????????,1:??????)
        } catch(Exception e) {
            e.printStackTrace();
        }

        LOG.message("[OtherControl] isAdultMenuDisplay, result=" + result);

        return result;
    }

    /**
     * ???????????? ?????? ??????
     * @param display    true or false
     */
    public void setDisplayAdultMenu(boolean display) {
        LOG.message("[OtherControl] setDisplayAdultMenu, display=" + display);

        try {
            String value = String.valueOf(display);
            stbInfoManager.setInfo(KeyConstant.UP_Adult_Menu_Display, value);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isLimitedWatchingTime() {
        return restrictionWatchTimeController.isStart();
    }

    public void cancelLimitedWatchingTime() {
        restrictionWatchTimeController.cancel();
    }

    public String getSAID() {
        if (SAID == null) {
            SAID = stbInformation.getSAID();
        }
        return SAID;
    }

    public boolean isValidSAID(String said) {
        if (said == null) {
            return false;
        }

        return said.equalsIgnoreCase(getSAID());
    }

    public String getIP() {
        return stbInformation.getIP();
    }

    public String getBouquetID() {
        return stbInformation.getBouquetID();
    }

    // CAS
    /**
     * ??????????????? ????????????. ???????????? ?????? ???????????? ;
     * <p>
     * 2010.11.26 : KT CAS ??????????????? KT_CAS ???????????? ????????????.
     */
    public String getProductCode() {
        String productCode = stbInformation.getProductCode(); // "257 258 262 488"
        LOG.message("getProductCode, productCode=" + productCode);

        String code = "KT_CAS" + CharConstant.CHAR_SPACE + productCode;
        return code.replace(' ', CharConstant.CHAR_SEMICOLON.charAt(0));
    }

    public String getMacAddress() {
        return stbInformation.getMac();
    }

    public boolean isSubscriber() {
        String productCode = getProductCode();
        boolean isSubscriber = productCode.indexOf(Constants.PRODUCT_KIDSCARE_CODE_BY_KT) > 0;

        LOG.message("isSubscriber, result=" + isSubscriber + ", productCode=" + productCode);

        return isSubscriber;
    }

    public void setClipboardContents(String argument) {
        if (argument == null) {
            return ;
        }

        stbInfoManager.setClipboardContents(argument);
    }

    /**
     * ?????? ?????? ?????? ????????? ??????.
     * ???????????? ????????? ???????????? ??????...
     * @return "N|20080411|18:04|18:10|1|Y"
     */
    public String getLimitedWatchingTime() {
        // "N|20080411|18:04|18:10|1|Y"
        String value = ktUserPreference.getRestrictionTime();
        LOG.message("[OtherControl] getRestrictionWatchTime, value=" + value);

        if (value == null) {
            value = "N|00000000|00:00|00:00|0|N";
        }
        return value;
    }

    /**
     * ?????? ?????? ?????? ??????
     * @param   value    "N|20080411|18:04|18:10|1|Y"
     */
    public void setLimitedWatchingTime(String value) {
        LOG.message("[OtherControl] setRestrictionWatchTime, value:" + value);

        try {
            GeneralPreference preference = new GeneralPreference(KeyConstant.UP_Time_Limit);
            preference.removeAll();
            preference.add(value);
            UserPreferenceManager.getInstance().write(preference);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public boolean changeHDSPin(String oldPin, String newPin) {
        addPinUpdatedListener();

        synchronized (WAIT_FOR_RESPONSE) {
            hdsPinUpdateInfImpl.updateHDSPin(oldPin, newPin);

            try {
                WAIT_FOR_RESPONSE.wait();
            } catch (InterruptedException e) {
                LOG.message(e.toString());
            }
        }

        return pinUpdatedResult;
    }

    /* (non-Javadoc)
     * @see com.alticast.tvcore.navigator.HDSPinUpdateListener#receivePinUpdated(com.alticast.hds.HDSPinUpdatedEvent)
     * HDSPinUpdatedEvent member
     *   event.success: ?????? ??????
     *   event.description: HDS?????? ?????? ????????? ?????? ?????? ?????????
     */
    public void receivePinUpdated(HDSPinUpdatedEvent event) {
        boolean success = event.success;

        LOG.message("[OtherControl] receivePinUpdated, success=" + success + ", desc="
                + event.description);

        pinUpdatedResult = success;

        synchronized (WAIT_FOR_RESPONSE) {
            WAIT_FOR_RESPONSE.notify();
        }

        removePinUpdatedListener();
    }

    private void addPinUpdatedListener() {
        hdsPinUpdateInfImpl.addPinUpdatedListener(this);
    }

    private void removePinUpdatedListener() {
        hdsPinUpdateInfImpl.removePinUpdatedListener(this);
    }
}
