package com.kt.remotecontrol.service.command;

import com.kt.navsuite.core.Channel;
import com.kt.remotecontrol.manager.ReserveManager;
import com.kt.remotecontrol.manager.ServiceChangeManager;
import com.kt.remotecontrol.manager.StatusManager;
import com.kt.remotecontrol.service.Service;
import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.ErrorCode;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.WorkingConfig;
import com.kt.remotecontrol.interlock.ProxyManager;

import java.util.Properties;
import java.util.StringTokenizer;

public class QueryService extends CommandService implements Service {

    private static final Log LOG = new Log("QueryService");

    private final String CHANNEL_TYPE_SI = "2";
    private final String CHANNEL_TYPE_SPECIFIC = "3";
    private final String CHANNEL_TYPE_COUNT = "4";

    public QueryService() {
        super();

        publishCommand.put("QRY1001", "getCurrentState"); // 현재 시청 상태 조회, STBState.GetCurrentState
        publishCommand.put("QRY1002", "getOffState"); // 현재 tv 끄기 상태 조회, STBInfo.GetOffState
        publishCommand.put("QRY1003", "getConfig"); // TV 설정 상태 조회, STBInfo.GetCareCfg
        publishCommand.put("QRY1004", "getLimitChannel"); // 채널제한 상태 조회, STBInfo.GetLimitChannel
        publishCommand.put("QRY1005", "getCurrentDisplayStatus"); // 현재 화면 Display 상태 조회, STBState.GetDisplayState
        publishCommand.put("QRY1007", "getFavoriteChannel"); // 마이 채널 설정 상태 조회, STBInfo.GetMyChannel
        publishCommand.put("QRY1008", "getHiddenChannel"); // 숨김 채널 설정 상태 조회, STBInfo.GetHiddenChannel
        publishCommand.put("QRY1009", "getMenuDisplay"); // 메뉴 표시 설정 상태 조회, STBInfo.GetMenuInfo
        publishCommand.put("QRY1010", "getVODPlayStatus"); // 현재 VOD 재생 상태 조회, STBState.GetPlayState
        publishCommand.put("QRY1011", "getMuteStatus"); // 현재 음소거 상태 조회, STBInfo.GetMuteState
        publishCommand.put("QRY1012", "getTurnOnNoticeStatus"); // 켜짐 알림 설정 조회, STBInfo.GetNotifyOn
        publishCommand.put("QRY1013", "getChangeState"); // olleh tv 켜기/전환, STBInfo.getOnState
        publishCommand.put("QRY1015", "getAutoOnOff"); // STB 자동 전원온오프 설정 상태 조회, STBInfo.getAutoOnOff

        publishCommand.put("QRY3000", "getVersion"); // 버전 조회
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
     * 현재 상태 조회 요청 (QRY1001, STBState.GetCurrentState)
     * (STB I/F Server->STB Agent)
     *
     *  CMD^RESULT^STB_STATE^RUN_MODE^ID^NAME
     *  QRY1001^000^1
     *  QRY1001^000^2^1^160^투니버스|괴도루팡
     *  QRY1001^000^2^3^220^날씨
     */
    protected String getCurrentState(Properties params) {

        LOG.message("getCurrentState(QRY1001)");

        StringBuffer data = getSUCCESS();
        data.append(getCurrentStateValue()).append(CharConstant.CHAR_CARET);
        data.append(Constants.STB_WEB_TYPE); // KTKIDSCARE-16

        return data.toString();
    }

    /**
     * 현재 TV 끄기 상태 조회(QRY1002, STBInfo.getOffState)
     */
    protected String getOffState(Properties params) {

        LOG.message("getOffState(QRY1002)");

        String limitedWatchTimeState = Constants.STATE_CLEAR;
        if (ProxyManager.otherHandler().isLimitedWatchingTime()) {
            limitedWatchTimeState = Constants.STATE_SET;
        }

        StringBuffer data = getSUCCESS();
        // OFF_STATE(STB 끄기 잠금 상태값 - 0:해제, 1:설정)
        data.append(limitedWatchTimeState).append(CharConstant.CHAR_CARET);
        // RSV_STATE(끄기 예약 상태값 - 0:해제, 1:설정) ^ RSV_TIME(끄기 예약 시간 - HHmm)
        data.append(ReserveManager.getInstance().getTurnOffResult());

        return data.toString();
    }

    /**
     * olleh tv 설정 상태 조회(QRY1003, STBInfo.GetCareCfg)
     */
    protected String getConfig(Properties params) {

        LOG.message("getConfig(QRY1003)");

        int parentalRating = ProxyManager.otherHandler().getParentalRating();
        String limitedWatchTime = ProxyManager.navigator().getLimitedWatchingTimeResult();

        // 숨김 설정의 경우라면 0 : 예, 1 : 아니오
        String displayAdultMenu = "0"; // 숨김
        if (ProxyManager.otherHandler().isDisplayAdultMenu()) {
            displayAdultMenu = "1"; // 보임
        }

        Channel[] channels = ProxyManager.navigator().getLimitedChannels();
        String channelNumbers = getChannelNumbers(channels);

        StringBuffer data = getSUCCESS();
        data.append(parentalRating).append(CharConstant.CHAR_CARET); // CFG_AG
        data.append(limitedWatchTime).append(CharConstant.CHAR_CARET); // CFG_TM
        data.append(displayAdultMenu).append(CharConstant.CHAR_CARET); // CFG_AD
        // 비밀번호 인증여부 설정은 EP4에서 없어졌음. ==> 무조건 인증하는 것으로...
        data.append("1").append(CharConstant.CHAR_VERTICAL); // 성인메뉴
        data.append("1").append(CharConstant.CHAR_CARET); // CFG_PC, 구매여부
        data.append(channelNumbers);

        return data.toString();
    }

    /**
     * olleh tv 시청 채널 제한 상태 조회(QRY1004, STBInfo.GetLimitChannel)
     * @param params
     */
    protected String getLimitChannel(Properties params) {

        LOG.message("getLimitChannel(QRY1004)");

        String type = params.getProperty(Constants.SCH_TYPE);
        String errorCode = checkChannelType(type);
        if (errorCode != null) {
            return errorCode;
        }

        String channelSID = params.getProperty(Constants.CH_LIST);
        Channel[] channels = ProxyManager.navigator().getLimitedChannels();

        return getChannelSettingStatus(type, channelSID, channels);
    }

    // [신규] 현재 화면 Display 상태 조회 (QRY1005)
    protected String getCurrentDisplayStatus(Properties params) {

        LOG.message("getCurrentDisplayStatus");

        StringBuffer data = getSUCCESS();
        data.append("ID_HOME|").append(ProxyManager.appHandler().getHomeState()).append(CharConstant.CHAR_SEMICOLON);
        data.append("ID_TCOM|").append(-1).append(CharConstant.CHAR_SEMICOLON);
        data.append("ID_QRATOR|").append(-1).append(CharConstant.CHAR_SEMICOLON);
        data.append("ID_RTIS|").append(-1).append(CharConstant.CHAR_SEMICOLON);
        data.append("ID_TV_PORTAL|").append(-1);

        return data.toString();
    }

    /**
     * [신규] 마이채널 설정 상태 조회(QRY1007)
     *
     * @param params
     */
    protected String getFavoriteChannel(Properties params) {

        LOG.message("getFavoriteChannel(QRY1007)");

        String type = params.getProperty(Constants.SCH_TYPE);
        String errorCode = checkChannelType(type);
        if (errorCode != null) {
            return errorCode;
        }

        String channelSID = params.getProperty(Constants.CH_LIST);
        Channel[] channels = ProxyManager.navigator().getFavoriteChannels();

        return getChannelSettingStatus(type, channelSID, channels);
    }

    /**
     * [신규] 숨김채널 설정 상태 조회(QRY1008)
     *
     * @param params
     */
    protected String getHiddenChannel(Properties params) {

        LOG.message("getHiddenChannel(QRY1008)");

        String type = params.getProperty(Constants.SCH_TYPE);
        String errorCode = checkChannelType(type);
        if (errorCode != null) {
            return errorCode;
        }

        String channelSID = params.getProperty(Constants.CH_LIST);
        Channel[] channels = ProxyManager.navigator().getHiddenChannels();

        return getChannelSettingStatus(type, channelSID, channels);
    }

    /**
     * [신규] 메뉴 표시 설정 상태 조회(QRY1009)
     * TODO: 구현해야 함
     * @param params
     */
    protected String getMenuDisplay(Properties params) {

        LOG.message("getMenuDisplay");

        return ErrorCode.SUCCESS;
    }

    // [신규] 현재 VOD 재생 상태 조회(QRY1010)
    protected String getVODPlayStatus(Properties params) {

        LOG.message("getVODPlayStatus");

        // play: 0, pause : 1, VOD 상태 아닐 때 -1
        String result = StatusManager.getInstance().getVODPlayResult();

        return getSUCCESS(result).toString();
    }

    // [신규] 현재 음소거 상태 조회 (QRY1011)
    protected String getMuteStatus(Properties params) {

        LOG.message("getMuteStatus");

        boolean mute = ProxyManager.keyHandler().isMute();

        return getSUCCESS(mute ? "1" : "0").toString();
    }

    // [신규] 켜짐 알림 설정 조회 (QRY1012)
    protected String getTurnOnNoticeStatus(Properties params) {

        LOG.message("getTurnOnNoticeStatus");

        String result = StatusManager.getInstance().getNotifySetupResult();

        return getSUCCESS(result).toString();
    }

    /**
     * 현재 olleh tv 켜기/전환 상태 조회(QRY1013, STBInfo.GetOnState)
     * @param params
     */
    protected String getChangeState(Properties params) {

        LOG.message("getChangeState(QRY1013)");

        String result = ServiceChangeManager.getInstance().getReservationResult();
        return getSUCCESS(result).toString();
    }

    /**
     * STB 자동 전원온오프 설정 상태 조회(QRY1015, STBInfo.GetAutoOnOff)
     * TODO: UP로 변경 필요
     * @param params
     */
    protected String getAutoOnOff(Properties params) {

        LOG.message("getAutoOnOff(QRY1015)");

        String resultCode = ErrorCode.C541;
        String responseMessage = null;

        /*
        if (response.isEmpty()) {

                LOG.message("getAutoOnOff, empty in the Result Map");
            }
            resultCode = ErrorCode.C699;
        } else {
            resultCode = (String) response.get(Constants.KEY_RESULT_CODE);
            if (ErrorCode.SUCCESS.equals(resultCode)) {
                responseMessage =  (String) response.get(Constants.KEY_RESPONSE_MESSAGE);
            }
        }
        */


        LOG.message("getAutoOnOff, resultCode=" + resultCode);

        if (responseMessage != null) {
            StringBuffer data = new StringBuffer();
            data.append(resultCode).append(CharConstant.CHAR_CARET);
            data.append(responseMessage);

            return data.toString();
        }

        return resultCode;
    }

    /**
     * 버전 조회(QRY3000)
     */
    protected String getVersion(Properties params) {

        LOG.message("getVersion");

        return getSUCCESS(WorkingConfig.VERSION).toString();
    }

    private String getCurrentStateValue() {
        String value = "";

        if (ProxyManager.navigator().isStandbyBySTB()) {
            LOG.message("getCurrentState, current STANDBY mode");

            value = Constants.STB_STATE_STANDBY; // 대기모드
        } else if (ProxyManager.navigator().isRunningBySTB()) {
            LOG.message("getCurrentState, current RUNNING status");

            value = Constants.STB_STATE_RUNNING + CharConstant.CHAR_CARET
                    + StatusManager.getInstance().getWatchingStatus();
        } else {
            LOG.message("getCurrentState, current OFF status");

            value = Constants.STB_STATE_OFF; // 꺼져 있는 경우, 이런 경우는 없다.
        }

        return value;
    }

    private String getChannelNumbers(Channel[] channels) {
        int count = channels.length;
        int last = count - 1;
        int channelNumber;
        StringBuffer data = new StringBuffer();

        for (int i = 0; i < count; i++) {
            channelNumber = channels[i].getNumber();

            LOG.error("restrict channel : " + channelNumber);

            data.append(channelNumber);

            if (i != last) {
                data.append(CharConstant.CHAR_SEMICOLON);
            }
        }

        return data.toString();
    }

    private String checkChannelType(String type) {
        if (type == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        if (!CHANNEL_TYPE_SI.equals(type) && !CHANNEL_TYPE_SPECIFIC.equals(type)
                && !CHANNEL_TYPE_COUNT.equals(type)) {
            LOG.message("invalid SCH_TYPE = " + type);
            return ErrorCode.INVALID_ATTRIBUTE;
        }

        return null;
    }

    private String getChannelSettingStatus(String type, String channelSID, Channel[] channels) {
        String count = "";
        String value = "";

        if (CHANNEL_TYPE_SI.equals(type)) { // 채널별 설정 조회
            value = getMatchedChannels(channelSID, channels);
        } else if (CHANNEL_TYPE_SPECIFIC.equals(type)) { // 채널 목록 조회
            count = String.valueOf(channels.length);
            value = getMatchedChannels(channels);
        } else if (CHANNEL_TYPE_COUNT.equals(type)) { // 채널 개수 조회
            count = String.valueOf(channels.length);
        }

        return getChannelSettingStatus(count, value);
    }

    private String getMatchedChannels(String serviceIDs, Channel[] channels) {
        StringTokenizer st = new StringTokenizer(serviceIDs, CharConstant.CHAR_SEMICOLON); // 23;24;25
        int count = channels.length;
        StringBuffer data = new StringBuffer();

        while (st.hasMoreTokens()) {
            int serviceID = Integer.parseInt(st.nextToken());
            Channel channel;
            Channel matchedChannel = null;

            for (int i = 0; i < count; i++) {
                channel = channels[i];

                if (channel.getSIService().getServiceID() == serviceID) {
                    matchedChannel = channel;
                    break;
                }
            }

            data.append(getChannelData(matchedChannel, serviceID));

            if (st.hasMoreTokens()) {
                data.append(CharConstant.CHAR_SEMICOLON);
            }
        }

        return data.toString();
    }

    private String getMatchedChannels(Channel[] channels) {
        int count = channels.length;
        int last = count - 1;
        StringBuffer data = new StringBuffer();

        for (int i = 0; i < count; i++) {
            data.append(getChannelDataBySetting(channels[i]));

            if (i != last) {
                data.append(CharConstant.CHAR_SEMICOLON);
            }
        }

        return data.toString();
    }

    private String getChannelData(Channel matchedChannel, int serviceID) {

        if (matchedChannel == null) {
            int channelNumber = ProxyManager.navigator().getChannelNumber(serviceID);
            return getChannelData(serviceID, channelNumber) + Constants.CHANNEL_UNSET;
        }

        return getChannelDataBySetting(matchedChannel);
    }

    private String getChannelDataBySetting(Channel channel) {
        int sid = channel.getSIService().getServiceID();

        return getChannelData(sid, channel.getNumber()) + Constants.CHANNEL_SET;
    }

    private String getChannelData(int sid, int channelNumber) {
        StringBuffer data = new StringBuffer();
        data.append(sid).append(CharConstant.CHAR_VERTICAL);
        data.append(channelNumber).append(CharConstant.CHAR_VERTICAL);

        return data.toString();
    }

    private String getChannelSettingStatus(String count, String value) {
        StringBuffer data = getSUCCESS();
        data.append(count).append(CharConstant.CHAR_CARET);
        data.append(value);

        return data.toString();
    }
}
