package com.kt.remotecontrol.service.command;

import com.kt.remotecontrol.interlock.server.hds.HDS;
import com.kt.remotecontrol.manager.PopupManager;
import com.kt.remotecontrol.manager.StatusManager;
import com.kt.remotecontrol.service.Service;
import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.ErrorCode;
import com.kt.remotecontrol.util.KeyConstant;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.interlock.app.Kidscare;

import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;

public class ConfigService extends CommandService implements Service {

    private static final Log LOG = new Log("ConfigService");

    public ConfigService() {
        super();

        publishCommand.put("CFG1001", "changeConfig"); // TV 설정 변경, STBInfo.SetCareCfg
        publishCommand.put("CFG1002", "setLimitChannel"); // TV 시청 채널 제한 설정, STBInfo.SetLimitChannel
        publishCommand.put("CFG1003", "setMyChannel"); // 마이 채널 설정 저장, STBInfo.SetMyChannel
        publishCommand.put("CFG1004", "setHiddenChannel"); // 숨김 채널 설정 저장, STBInfo.SetHiddenChannel
        publishCommand.put("CFG1005", "setMenuInfo"); // 메뉴 표시 설정 저장, STBInfo.SetMenuInfo
        publishCommand.put("CFG1006", "setTurnOnAlarmStatus"); // 켜짐 알림 설정 저장, STBInfo.SetNotifyOn
        publishCommand.put("CFG3001", "changePIN"); // TV 비밀번호 변경(구매인증 비밀번호), STBInfo.SetPassword
    }

    public String execute(Properties params) {
        String cmd = params.getProperty(Constants.CMD);

        if (!publishCommand.containsKey(cmd)) {
            return ErrorCode.INVALID_COMMAND;
        }

        String methodName = (String) publishCommand.get(cmd);
        return execute(this, methodName, params);
    }

    /**
     * olleh tv 설정 변경(CFG1001, STBInfo.SetCareCfg)
     * @param params
     */
    protected String changeConfig(Properties params) {
        LOG.message("changeConfig(CFG1001)");

        String messageType = params.getProperty(Constants.MSG_YN);
        if (messageType == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        if (PopupManager.getInstance().invalidMessageType(messageType)) {
            LOG.message("changeConfig, invalid MSG_YN = " + messageType);
            return ErrorCode.INVALID_ATTRIBUTE;
        }

        String resultCode = changeViewAge(params); // 시청 연령 변경
        if (resultCode != null) {
            return resultCode;
        }

        resultCode = changeLimitedWatchingTime(params); // 시청 시간 제한 변경
        if (resultCode != null) {
            return resultCode;
        }

        resultCode = changeDisplayAdultMenu(params); // 성인 메뉴 표시
        if (resultCode != null) {
            return resultCode;
        }

        return ErrorCode.SUCCESS;
    }

    /**
     * 시청채널 제한 설정(CFG1002, STBInfo.SetLimitChannel)
     * @param params
     */
    protected String setLimitChannel(Properties params) {
        LOG.message("setLimitChannel");

        String errorCode = checkChannelParam(params);
        if (errorCode != null) {
            return errorCode;
        }

        String allOff = params.getProperty(Constants.ALL_OFF);

        if (Constants.ALL_OFF_ALL_CHANNEL.equals(allOff)) { // 전체 제한 채널 해제.
            ProxyManager.navigator().clearLimitedChannel();
            selectCurrentChannelWhenValidState();
        } else { // 채널 별 설정.
            String ch_list = params.getProperty(Constants.CH_LIST);
            if (ch_list == null || ch_list.length() == 0) { // 옵션이니까 에러처리 안함.
                LOG.message("setLimitChannel, channel list is empty, ch_list="
                            + (ch_list == null ? "null" : ch_list.length() + ""));
            } else {
                errorCode = ProxyManager.navigator().updateLimitedChannel(ch_list);

                if (errorCode != null) {
                    return errorCode;
                }
            }
        }

        PopupManager.getInstance().showMessageByThread(params);

        return ErrorCode.SUCCESS;
    }

    private void selectCurrentChannelWhenValidState() {
        if (StatusManager.getInstance().isChannelOrIDLEState()) {
            ProxyManager.channelHandler().selectCurrentChannel();
        }
    }

    /**
     * 마이 채널 설정 저장(CFG1003, STBInfo.SetMyChannel)
     * @param params
     */
    protected String setMyChannel(Properties params) {
        LOG.message("setMyChannel");

        String errorCode = checkChannelParam(params);
        if (errorCode != null) {
            return errorCode;
        }

        String allOff = params.getProperty(Constants.ALL_OFF);

        if (Constants.ALL_OFF_ALL_CHANNEL.equals(allOff)) { // 전체 마이채널 삭제
            ProxyManager.navigator().clearFavoriteChannel();
        } else { // 채널 별 설정.
            String channelList = params.getProperty(Constants.CH_LIST);
            if (channelList == null || channelList.length() == 0) { // 옵션이니까 에러처리 안함.
                LOG.message("setMyChannel, channel list is empty, ch_list="
                            + (channelList == null ? "null" : channelList.length() + ""));
            } else {
                errorCode = ProxyManager.navigator().updateFavoriteChannel(channelList);

                if (errorCode != null) {
                    return errorCode;
                }
            }
        }

        PopupManager.getInstance().showMessageByThread(params);

        return ErrorCode.SUCCESS;
    }

    /**
     * 숨김 채널 설정 저장(CFG1004, STBInfo.SetHiddenChannel)
     * @param params
     */
    protected String setHiddenChannel(Properties params) {
        LOG.message("setHiddenChannel");

        String errorCode = checkChannelParam(params);
        if (errorCode != null) {
            return errorCode;
        }

        String allOff = params.getProperty(Constants.ALL_OFF);

        if (Constants.ALL_OFF_ALL_CHANNEL.equals(allOff)) { // 전체 숨김 채널
            ProxyManager.navigator().clearHiddenChannel();
        } else { // 채널 별 설정.
            String ch_list = params.getProperty(Constants.CH_LIST);
            if (ch_list == null || ch_list.length() == 0) { // 옵션이니까 에러처리 안함.
                LOG.message("setHiddenChannel, channel list is empty, ch_list="
                        + (ch_list == null ? "null" : ch_list.length() + ""));
            } else {
                errorCode = ProxyManager.navigator().updateHiddenChannel(ch_list);

                if (errorCode != null) {
                    return errorCode;
                }
            }
        }

        PopupManager.getInstance().showMessageByThread(params);

        return ErrorCode.SUCCESS;
    }

    /**
     * 메뉴 표시 설정 저장(CFG1005, STBInfo.SetMenuInfo)
     * TODO : 구현해야 함
     * @param params
     */
    protected String setMenuInfo(Properties params) {
        LOG.message("setMenuInfo");

        return ErrorCode.SUCCESS;
    }

    /**
     * 켜짐 알림 설정 저장(CFG1006) // 2차
     * @param params
     */
    protected String setTurnOnAlarmStatus(Properties params) {
        LOG.message("setTurnOnAlarmStatus");

        String notiCfg = params.getProperty(Constants.NOTI_CFG);
        String notiStart = params.getProperty(Constants.NOTI_START);
        String notiEnd = params.getProperty(Constants.NOTI_END);

        if (notiCfg == null || notiStart == null || notiEnd == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        boolean notify = Constants.STATE_SET.equals(notiCfg);

        try {
            StatusManager.getInstance().setNotify(notify, notiStart, notiEnd);
            return ErrorCode.SUCCESS;
        } catch (RuntimeException e) {
            return ErrorCode.INVALID_NUMBER_VALUE;
        }
    }

    /**
     * olleh tv 비밀번호 변경(CFG3001, STBInfo.SetPassword)
     *
     *@param params CMD, SVC_CD, SAID, PASSWD, NEW_PASSWORD
     *
     * [에러코드]
     * 000 - 성공
     * 499 - 정의되지 않은 STB 오류 - STB 관련 기타 오류
     * 508 - 존재하지 않는 SAID 로 요청
     * 502 - 비밀번호 오류
     * 601 - HDS 연동 오류
     */
    protected String changePIN(Properties params) {
        LOG.message("changePIN(CFG3001)");

        String password = params.getProperty(Constants.PASSWD);
        String new_password = params.getProperty(Constants.NEW_PASSWORD);

        if (password == null || new_password == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        boolean isBuyPIN = Constants.PW_TYPE_BUY.equals(params.getProperty(Constants.PW_TYPE)); // KTKIDSCARE-3
        String resultCode;

        if (isBuyPIN) {
            resultCode = new HDS().changeBuyPin(password, new_password);
        } else if (ProxyManager.otherHandler().checkAdultPIN(password)) {
            if (ProxyManager.otherHandler().changeAdultPIN(password, new_password)) {
                LOG.message("success to change pin");
                resultCode = ErrorCode.SUCCESS;
            } else {
                LOG.message("fail to change pin");
                resultCode = ErrorCode.C601;
            }
        } else {
            LOG.message("fail to match valid check pin");
            resultCode = ErrorCode.C502;
        }

        return resultCode;
    }

    protected String changeBuyPin(String pin, String newPIN) {

        HashMap responseMap = Kidscare.changeBuyPin(pin, newPIN);
        String resultCode = null;

        if (responseMap.isEmpty()) {
            LOG.message("changeBuyPin, empty in the Result Map");
            resultCode = ErrorCode.C601;
        } else if (responseMap.containsKey(KeyConstant.RESULT_CODE)) {
            resultCode = (String) responseMap.get(KeyConstant.RESULT_CODE);
        }

        LOG.message("changeBuyPin, resultCode=" + resultCode);

        return resultCode;
    }

    private String changeViewAge(Properties params) {
        String age = params.getProperty(Constants.CFG_AG);
        if (age == null || "".equals(age)) {
            LOG.message("changeConfig, invalid age(CFG_AG)!");
            return null;
        }

        String pin = params.getProperty(Constants.PIN_NO);
        if (pin == null || !ProxyManager.otherHandler().checkAdultPIN(pin)) {
            LOG.message("changeConfig, PIN ERROR!!");
            return ErrorCode.C502;
        }

        int ageIntValue = 0;
        try {
            ageIntValue = Integer.parseInt(age);
        } catch (NumberFormatException e) {
            return ErrorCode.C107;
        }

        switch (ageIntValue) {
            case 0: // 제한없음
            case 7: // 7세이상제한
            case 12: // 12세이상제한
            case 15: // 15세이상제한
            case 19: // 19세이상제한
                boolean result = ProxyManager.otherHandler().setParentalRating(ageIntValue, pin);

                if (!result) { // 변경 실패시
                    return ErrorCode.STB_ETC;
                }
                break;
            default:
                LOG.message("changeConfig, invalid CFG_AG(parentalRate)=" + age);
                return ErrorCode.INVALID_ATTRIBUTE;
        }

        PopupManager.getInstance().showMessageByThread(params);

        LOG.message("changeConfig, set age : " + age);

        return null;
    }

    private String changeLimitedWatchingTime(Properties params) {
        String tm = params.getProperty(Constants.CFG_TM);

        if (tm == null || "".equals(tm)) {
            LOG.message("changeConfig, invalid time(CFG_TM)!");
            return null;
        }

        StringTokenizer st = new StringTokenizer(tm, CharConstant.CHAR_VERTICAL);

        try {
            String set = st.nextToken();

            if (Constants.STATE_CLEAR.equals(set)) { // 해제
                releaseLimitedWatchingTime(params);
            } else if (Constants.STATE_SET.equals(set)) { // 설정
                String startTime = st.nextToken();
                String endTime = st.nextToken();

                if (startTime == null || endTime == null) {
                    return null;
                }

                String repeat = st.nextToken();

                lockLimitedWatchingTime(startTime, endTime, repeat, params);
            } else {
                throw new IllegalArgumentException("invalid CFG_TM, expected value is 0(CLEAR) or 1(SET)");
            }
        } catch (Exception e) {
            LOG.message("changeConfig, invalid CFG_TM='" + tm + "', "
                        + e.getMessage());

            return ErrorCode.INVALID_ATTRIBUTE;
        }

        return null;
    }

    private void releaseLimitedWatchingTime(Properties params) {
        ProxyManager.navigator().cancelAndClearLimitedWatchingTime();

        PopupManager.getInstance().showMessageByThread(params);

        LOG.message("changeConfig, set restict time free ");
    }

    private void lockLimitedWatchingTime(String startTime, String endTime, String repeat, Properties params) {

        // HH:mm 이므로.
        String startHour = startTime.substring(0, 2);
        String startMin = startTime.substring(2, 4);
        String endHour = endTime.substring(0, 2);
        String endMin = endTime.substring(2, 4);

        LOG.message("changeConfig, setting reserve restriction time = ["
                    + startHour + ":" + startMin + "]~[" + endHour + ":" + endMin + "]");

        boolean isRepeat = false;

        if (repeat != null) {
            if (Constants.STATE_ONCE.equals(repeat)) {
                LOG.message("changeConfig, only today once");
            } else if (Constants.STATE_REPEAT.equals(repeat)) {
                LOG.message("changeConfig, reserve every day ");
                isRepeat = true;
            } else {
                LOG.message("changeConfig, invalid, but set only today once");
            }
        }

        ProxyManager.navigator().setLimitedWatchingTime(startHour, startMin, endHour, endMin, isRepeat);
        PopupManager.getInstance().showMessageByThread(params);
    }

    private String changeDisplayAdultMenu(Properties params) {
        String ad = params.getProperty(Constants.CFG_AD);

        if (ad == null || "".equals(ad)) {
            LOG.message("changeConfig, invalid adult menu(CFG_AD)!");
            return null;
        }

        if (Constants.STATE_CLEAR.equals(ad)) { // 숨김
            ProxyManager.otherHandler().setDisplayAdultMenu(false);
            LOG.message("changeConfig, hide adult menu");
        } else if (Constants.STATE_SET.equals(ad)) { // 보임
            ProxyManager.otherHandler().setDisplayAdultMenu(true);
            LOG.message("changeConfig, show adult menu");
        } else {
            LOG.message("changeConfig, invalid CFG_AD = " + ad);
            return ErrorCode.INVALID_ATTRIBUTE;
        }

        PopupManager.getInstance().showMessageByThread(params);

        return null;
    }

    private String checkChannelParam(Properties params) {
        String messageType = params.getProperty(Constants.MSG_YN);
        if (messageType == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        if (PopupManager.getInstance().invalidMessageType(messageType)) {
            return ErrorCode.INVALID_ATTRIBUTE;
        }

        String allOff = params.getProperty(Constants.ALL_OFF);
        if (allOff == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        return null;
    }
}
