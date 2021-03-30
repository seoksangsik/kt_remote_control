package com.kt.remotecontrol.service.command;

import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.interlock.app.AppStore;
import com.kt.remotecontrol.interlock.app.HomePortal;
import com.kt.remotecontrol.interlock.app.Kidscare;
import com.kt.remotecontrol.manager.PopupManager;
import com.kt.remotecontrol.manager.ReserveManager;
import com.kt.remotecontrol.manager.ServiceChangeManager;
import com.kt.remotecontrol.manager.StatusManager;
import com.kt.remotecontrol.service.Service;
import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.ErrorCode;
import com.kt.remotecontrol.util.KeyConstant;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.util.TimeConstant;
import com.kt.remotecontrol.util.Util;

import org.havi.ui.event.HRcEvent;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Properties;

public class ControlService extends CommandService implements Service {

    private static final Log LOG = new Log("ControlService");

    public ControlService() {
        super();

        publishCommand.put("CTL1001", "turnOff"); // tv 끄기, STBInfo.setOffState
        publishCommand.put("CTL1002", "changeState"); // tv 켜기/전환, STBInfo.getOnState
        publishCommand.put("CTL1003", "inputButton"); // 리모컨 버튼 입력, Remocon.InputButton
        publishCommand.put("CTL1004", "inputString"); // 문자열 입력, Remocon.InputString
        publishCommand.put("CTL1005", "changeChannel"); // 채널 변경, STBCommand.ChangeChannel
        publishCommand.put("CTL1006", "playVOD"); // VOD 재생, STBCommand.PlayVOD
        publishCommand.put("CTL1007", "powerButton"); // 전원 버튼, STBCommand.Power
        publishCommand.put("CTL1008", "okButton"); // 확인 버튼, STBCommand.Ok
        publishCommand.put("CTL1009", "previousCancelButton"); // 이전/취소 버튼, STBCommand.Previous
        publishCommand.put("CTL1010", "exitButton"); // 나가기 버튼, STBCommand.Exit
        publishCommand.put("CTL1011", "upButton"); // 상 버튼, STBCommand.Up
        publishCommand.put("CTL1012", "downButton"); // 하 버튼, STBCommand.Down
        publishCommand.put("CTL1013", "leftButton"); // 좌 버튼, STBCommand.Left
        publishCommand.put("CTL1014", "rightButton"); // 우 버튼, STBCommand.Right
        publishCommand.put("CTL1015", "redButton"); // 컬러키 Red 버튼, STBCommand.Red
        publishCommand.put("CTL1016", "greenButton"); // 컬러키 Green 버튼, STBCommand.Green
        publishCommand.put("CTL1017", "yellowButton"); // 컬러키 Yellow 버튼, STBCommand.Yellow
        publishCommand.put("CTL1018", "blueButton"); // 컬러키 Blue 버튼, STBCommand.Blue
        publishCommand.put("CTL1019", "channelUpButton"); // 채널 Up 버튼, STBCommand.ChannelUp
        publishCommand.put("CTL1020", "channelDownButton"); // 채널 Down 버튼, STBCommand.ChannelDown
        publishCommand.put("CTL1021", "volumeUpButton"); // 볼륨 Up 버튼, STBCommand.VolumeUp
        publishCommand.put("CTL1022", "volumeDownButton"); // 볼륨 Down 버튼, STBCommand.VolumeDown
        publishCommand.put("CTL1023", "muteButton"); //  음소거 버튼, STBCommand.Mute
        publishCommand.put("CTL1024", "playPauseButton"); // Play/Pause 버튼, STBCommand.PlayPause
        publishCommand.put("CTL1025", "stopButton"); // Stop 버튼, STBCommand.Stop
        publishCommand.put("CTL1026", "rewindButton"); // Rewind 버튼, STBCommand.Rewind
        publishCommand.put("CTL1027", "fastForwardButton"); // FastForward 버튼, STBCommand.FastForward
        publishCommand.put("CTL1028", "previousTrackButton"); // 건너뛰기(이전) 버튼, STBCommand.PreviousTrack
        publishCommand.put("CTL1029", "nextTrackButton"); // 건너뛰기(다음) 버튼, STBCommand.NextTrack
        publishCommand.put("CTL1030", "sendMessage"); // 스마트앱스토어 실행, STBCommand.ExecuteSas
        publishCommand.put("CTL1031", "sendURL"); // URL 미러링, STBCommand.SendURL
        publishCommand.put("CTL1032", "playVODBySeemless"); // VOD 재생(심리스 서비스만 사용), STBCommand.PlayVODS
        publishCommand.put("CTL1033", "showCategory"); // 홈메뉴 바로가기, STBCommand.ShowCategory
        publishCommand.put("CTL1034", "executeApp"); // 앱 실행하기, STBCommand.ExecuteApp
        publishCommand.put("CTL1035", "findRemocon"); // 리모컨 찾기 기능
        publishCommand.put("CTL1036", "executeAppWith"); // 앱 실행 후 응답 결과 전달하기, STBCommand.ExecuteAppWith
        publishCommand.put("CTL1037", "setAutoOnOff"); // STB 자동 전원온오프 설정, STBCommand.setAutoOnOff
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
     * olleh tv 끄기(CTL1001, STBInfo.setOffState)
     * @param params
     */
    protected String turnOff(final Properties params) {
        LOG.message("turnOff(CTL1001)");

        String messageType = params.getProperty(Constants.MSG_YN);
        String offType = params.getProperty(Constants.OFF_TYPE);

        if (messageType == null || offType == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        if (PopupManager.getInstance().invalidMessageType(messageType)) {
            LOG.message("turnOff, unknown msg_yn : " + messageType);
            return ErrorCode.INVALID_ATTRIBUTE;
        }

        String result;

        if (Constants.OFF_TYPE_NOW_TURN_OFF.equals(offType)) { // 즉시 끄기
            result = turnOffNow(params);
        } else if (Constants.OFF_TYPE_RESERVATION.equals(offType)) { // 끄기 예약
            result = turnOffBooking(params);
        } else if (Constants.OFF_TYPE_UNLOCK.equals(offType)) { // TV 잠금 해제
            result = turnOffUnlock(params);
        } else if (Constants.OFF_TYPE_RESERVATION_CANCLE.equals(offType)) { // 끄기 예약 해제
            result = turnOffUnbooking(params);
        } else { // UNKNOWN Command
            LOG.message("unknown offType : " + offType);
            result = ErrorCode.INVALID_ATTRIBUTE;
        }

        return result;
    }

    /**
     * olleh tv 켜기/전환(CTL1002, STBInfo.SetOnState)
     * @param params
     */
    protected String changeState(Properties params) {
        LOG.message("changeState(CTL1002)");

        String messageType = params.getProperty(Constants.MSG_YN); // 팝업 메세지 유무(0:없음, 1:있음)
        String type = params.getProperty(Constants.TYPE); // 전환 구분 (0: 즉시 전환, 1: 예약 전환, 2: 예약 전환 취소)
        String time = params.getProperty(Constants.TIME); // 전환 예약 시간 (HHmm)
        String runMode = params.getProperty(Constants.RUN_MODE); // 전환 종류  (0: 전환 없음, 1: 채널, 2: VOD, 3: 양방향 서비스)

        if (messageType == null || type == null || time == null || runMode == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        if (PopupManager.getInstance().invalidMessageType(messageType)) {
            LOG.message("changeState, unknown msg_yn : " + messageType);
            return ErrorCode.INVALID_ATTRIBUTE;
        }

        if (invalidChangeType(type) || invalidRunMode(runMode)) {
            LOG.message("changeState, unknown values(type: " + type
                        + ", runMode: " + runMode + ")");
            return ErrorCode.INVALID_ATTRIBUTE;
        }

        // 대기모드에서 채널이나 VOD 전환인 경우 최초상태 알림이 잘못 가는 문제가 있다.
        // 이 때 동작모드 전환 후 첫번째 채널 변경을 최초상태로 판단하면 안 된다.
        if (ProxyManager.navigator().isNotRunningBySTB() && isChannelOrVODRunMode(runMode)) {
            StatusManager.getInstance().skipNotifyInitialStatus();
        }

        String id = params.getProperty(Constants.ID); // ID
        boolean changeRestrictWatchTime = !Constants.SET_OFF.equals(params.getProperty(Constants.CFG_TM)); // KTKIDSCARE-17

        if (Constants.TYPE_NOW_CHANGE.equals(type)) { // 즉시전환
            ServiceChangeManager.getInstance().changeRightNow(runMode, id, changeRestrictWatchTime);
        } else if (Constants.TYPE_RESERVATION_CHANGE.equals(type)) { // 예약전환
            ServiceChangeManager.getInstance().changeReservation(runMode, id, time, changeRestrictWatchTime);
        } else { // 예약취소
            ServiceChangeManager.getInstance().cancelReservation();
        }

        if (Constants.MSG_YN_SHOW_MSG.equals(messageType)) {

            boolean sameChannel = Constants.RUN_MODE_CHANNEL.equals(runMode) // 채널전환이고
                    && ProxyManager.channelHandler().isSameCurrentChannel(id); // 현재 채널과 동일하니?

            if (!sameChannel) {
                PopupManager.getInstance().showMessageByThread(params);
            }
        }

        return ErrorCode.SUCCESS;
    }

    // [신규] 리모컨 버튼 입력(CTL1003)
    protected String inputButton(Properties params) {
        LOG.message("inputButton(CTL1003)");

        String key = params.getProperty(Constants.KEY_CD);
        if (key == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        String resultCode = null;

        try {
            int keyCode = Integer.parseInt(key);
            resultCode = sendKey(keyCode);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            resultCode = ErrorCode.INVALID_NUMBER_VALUE;
        }

        return resultCode;
    }

    /**
     * 문자열 입력(CTL1004, Remocon.InputString)
     * TODO : | 로 나누어진 입력 문자열 키 코드들이 들어있는데, 이걸로 처리해야 할 때는 언제일까?
     * @param params
     */
    protected synchronized String inputString(Properties params) {
        LOG.message("inputString(CTL1004)");

//        String codesArray = (String)parms.get(IN_STR_CD);
        String encodeValue = params.getProperty(Constants.IN_STR);
        if (encodeValue == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        boolean result = false;

        try {
            String decodeValue = URLDecoder.decode(encodeValue, "UTF-8");
            // Input Tag(HSingLineEntry)가 아닌 경우에는 들어가지 않는다.

            result = ProxyManager.eventGenerator().appendStringToIME(decodeValue);

            LOG.message("inputString, appendStringToIME result=" + result);

            if (result) {
                return ErrorCode.SUCCESS;
            }

            result = ProxyManager.appHandler().sendKeyword(decodeValue);

            LOG.message("inputString, sendKeyword result=" + result);
        } catch (Exception e) {
            e.printStackTrace();
            return ErrorCode.C199;
        }

        return getResultByKey(result);
    }

    /**
     * 채널 변경(CTL1005, STBCommand.ChangeChannel)
     * @param params
     */
    protected String changeChannel(Properties params) {
        LOG.message("changeChannel(CTL1005), params=" + params);

        if (ProxyManager.otherHandler().isLimitedWatchingTime()) {
            LOG.message("changeChannel(CTL1005), restrictionWatchTime");

            return ErrorCode.WONT_RUN_CAUSE_LIMITED_WATCHING_TIME; // KTKIDSCARE-82
        }

        if (ProxyManager.channelHandler().isBrowserChannelByRealCurrent()) {
            LOG.message("changeChannel(CTL1005), tuned by browser channel" );

            return ErrorCode.SUCCESS;
        }

        String channelID = params.getProperty(Constants.CH_ID);
        String channelMode = params.getProperty(Constants.CH_MODE);

        if (channelID == null || channelMode == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        String result = null;

        try {
            int channelNumber = Integer.parseInt(channelID);
            boolean isSkylifeChannel = Constants.CH_MODE_SKYLIFE.equals(channelMode);
            String argument = params.getProperty(Constants.ARGU_1); // KTKIDSCARE-5

            LOG.message("changeChannel, CH_ID=" + channelNumber
                        + ", CH_MODE=" + channelMode + ", argument=" + argument);

            boolean success = ProxyManager.navigator().changeChannel(channelNumber, isSkylifeChannel, argument);

            result = getResultByKey(success);
        } catch (NumberFormatException e) {
            result = ErrorCode.INVALID_NUMBER_VALUE;
            e.printStackTrace();
        }

        return result;
    }

    /**
     * VOD 재생(CTL1006, STBCommand.PlayVOD)
     * @param params
     */
    protected String playVOD(final Properties params) {
        LOG.message("playVOD(CTL1006)");

        if (ProxyManager.otherHandler().isLimitedWatchingTime()) {
            return ErrorCode.WONT_RUN_CAUSE_LIMITED_WATCHING_TIME; // KTKIDSCARE-82
        }

        if (params.getProperty(Constants.CON_ID) == null) { // VOD Asset ID
            LOG.message("playVOD, CON_ID is NULL.");

            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        new Thread("playVOD thread") {
            public void run() {
                LOG.message("playVOD, postMessage=["
                            + KeyConstant.METHOD + ": hp_watchContent, "
                            + KeyConstant.CONST_ID + ": " + params.getProperty(Constants.CON_ID) + "]");

                if (changeToVODPlayState("playVOD")) {
                    return ;
                }

                HomePortal.hp_watchContent(params);
            }
        }.start();

        return ErrorCode.SUCCESS;
    }

    /**
     * 전원 버튼(CTL1007, STBCommand.Power)
     */
    protected String powerButton(Properties params) {
        LOG.message("powerButton(CTL1007)");

        return sendKey(HRcEvent.VK_POWER);
    }

    /**
     * 확인 버튼(CTL1008, STBCommand.Ok)
     */
    protected String okButton(Properties params) {
        LOG.message("okBtn(CTL1008)");

        return sendKey(HRcEvent.VK_ENTER);
    }

    /**
     * 이전/취소 버튼(CTL1009, STBCommand.Previous)
     */
    protected String previousCancelButton(Properties params) {
        LOG.message("previousCancelBtn(CTL1009)");

        return sendKey(HRcEvent.VK_F4); // KTKIDSCARE-87
    }

    /**
     * 나가기 버튼(CTL1010, STBCommand.Exit)
     */
    protected String exitButton(Properties params) {
        LOG.message("exitBtn(CTL1010)");

        return sendKey(HRcEvent.VK_ESCAPE);
    }

    /**
     * 상 버튼(CTL1011, STBCommand.Up)
     */
    protected String upButton(Properties params) {
        LOG.message("upBtn(CTL1011)");

        return sendKey(HRcEvent.VK_UP);
    }

    /**
     * 하 버튼(CTL1012, STBCommand.Down)
     */
    protected String downButton(Properties params) {
        LOG.message("downBtn(CTL1012)");

        return sendKey(HRcEvent.VK_DOWN);
    }

    /**
     * 좌 버튼(CTL1013, STBCommand.Left)
     */
    protected String leftButton(Properties params) {
        LOG.message("leftBtn(CTL1013)");

        return sendKey(HRcEvent.VK_LEFT);
    }

    /**
     * 우 버튼(CTL1014, STBCommand.Right
     */
    protected String rightButton(Properties params) {
        LOG.message("rightBtn(CTL1014)");

        return sendKey(HRcEvent.VK_RIGHT);
    }

    /**
     * 컬러키 Red 버튼(CTL1015, STBCommand.Red)
     */
    protected String redButton(Properties params) {
        LOG.message("redBtn(CTL1015)");

        return sendKey(HRcEvent.VK_COLORED_KEY_0);
    }

    /**
     * 컬러키 Green 버튼(CTL1016, STBCommand.Green)
     */
    protected String greenButton(Properties params) {
        LOG.message("greenBtn(CTL1016)");

        return sendKey(HRcEvent.VK_COLORED_KEY_1);
    }

    /**
     * 컬러키 Yellow 버튼(CTL1017, STBCommand.Yellow)
     */
    protected String yellowButton(Properties params) {
        LOG.message("yellowBtn(CTL1017)");

        return sendKey(HRcEvent.VK_COLORED_KEY_2);
    }

    /**
     * 컬러키 Blue 버튼(CTL1018, STBCommand.Blue)
     */
    protected String blueButton(Properties params) {
        LOG.message("blueBtn(CTL1018)");

        return sendKey(HRcEvent.VK_COLORED_KEY_3);
    }

    /**
     * 채널 Up 버튼(CTL1019, STBCommand.ChannelUp)
     */
    protected String channelUpButton(Properties params) {
        LOG.message("channelUpBtn(CTL1019)");

        if (ProxyManager.otherHandler().isLimitedWatchingTime()) {
            return ErrorCode.WONT_RUN_CAUSE_LIMITED_WATCHING_TIME; // KTKIDSCARE-82
        }

        return sendKey(HRcEvent.VK_CHANNEL_UP);
    }

    /**
     * 채널 Down 버튼(CTL1020, STBCommand.ChannelDown)
     * @param params
     */
    protected String channelDownButton(Properties params) {
        LOG.message("channelDownBtn(CTL1020)");

        if (ProxyManager.otherHandler().isLimitedWatchingTime()) {
            return ErrorCode.WONT_RUN_CAUSE_LIMITED_WATCHING_TIME; // KTKIDSCARE-82
        }

        return sendKey(HRcEvent.VK_CHANNEL_DOWN);
    }

    /**
     * 볼륨 Up 버튼(CTL1021, STBCommand.VolumeUp)
     * @param params
     */
    protected String volumeUpButton(Properties params) {
        LOG.message("volumeUpBtn(CTL1021)");

        boolean success = ProxyManager.keyHandler().volumeUp();
        return getResultByKey(success);
    }

    /**
     * 볼륨 Down 버튼(CTL1022, STBCommand.VolumeDown)
     */
    protected String volumeDownButton(Properties params) {
        LOG.message("volumeDownBtn(CTL1022)");

        boolean success = ProxyManager.keyHandler().volumeDown();
        return getResultByKey(success);
    }

    /**
     * 음소거 버튼(CTL1023, STBCommand.Mute)
     */
    protected String muteButton(Properties params) {
        LOG.message("muteBtn(CTL1023)");

        boolean success = ProxyManager.keyHandler().mute();
        return getResultByKey(success);
    }

    /**
     * Play/Pause 버튼(CTL1024, STBCommand.PlayPause)
     */
    protected String playPauseButton(Properties params) {
        LOG.message("playPauseBtn(CTL1024)");

        return sendKey(HRcEvent.VK_PLAY);
    }

    /**
     * Stop 버튼(CTL1025, STBCommand.Stop)
     */
    protected String stopButton(Properties params) {
        LOG.message("stopBtn(CTL1025)");

        return sendKey(HRcEvent.VK_STOP);
    }

    /**
     * Rewind 버튼(CTL1026, STBCommand.Rewind)
     */
    protected String rewindButton(Properties params) {
        LOG.message("rewindBtn(CTL1026)");

        return sendKey(HRcEvent.VK_REWIND);
    }

    /**
     * FastForward 버튼(CTL1027, STBCommand.FastForward)
     */
    protected String fastForwardButton(Properties params) {
        LOG.message("fastForwardBtn(CTL1027)");

        return sendKey(HRcEvent.VK_FAST_FWD);
    }

    /**
     * 건너뛰기(이전) 버튼(CTL1028, STBCommand.PreviousTrack)
     */
    protected String previousTrackButton(Properties params) {
        LOG.message("previousTrackBtn(CTL1028)");

        return sendKey(HRcEvent.VK_LEFT);
    }

    /**
     * 건너뛰기(다음) 버튼(CTL1029, STBCommand.NextTrack)
     */
    protected String nextTrackButton(Properties params) {
        LOG.message("nextTrackBtn(CTL1029)");

        return sendKey(HRcEvent.VK_RIGHT);
    }

    /**
     * 스마트앱스토어 실행(CTL1030, STBCommand.ExecuteSas)
     * @param params
     */
    protected String sendMessage(Properties params) {
        LOG.message("sendMessage(CTL1030)");

        String toApp = params.getProperty(Constants.TO_APP);
        String umtMessage = params.getProperty(Constants.UMT_MSG);

        if (toApp == null || umtMessage == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

//        String fromApp = params.getProperty(FROM_APP); // Optional
        String result;

        try {
            int toAid = Integer.parseInt(toApp);
            LOG.message("sendMessage, app's aid : " + toApp + ", toAid=" + toAid);

            if (toAid == 0x3009) { // 0x3009=12297
                AppStore.as_runSASApp(umtMessage);
            } else {
                LOG.message("sendMessage, app's aid : " + toAid);

                ProxyManager.navigator().talkToApp(umtMessage, toApp);
            }

            result = ErrorCode.SUCCESS;
        } catch (NumberFormatException e) {
            result = ErrorCode.INVALID_NUMBER_VALUE;
        } catch (Exception e) {
            LOG.message("sendMessage, message=" + umtMessage);
            e.printStackTrace();
            result = ErrorCode.STB_ETC;
        }

        return result;
    }

    /**
     * URL 미러링 (CTL1031, STBCommand.SendURL)
     * @param params
     */
    protected String sendURL(Properties params) {
        LOG.message("sendURL(CTL1031)");

        String url = params.getProperty(Constants.URL);
        if (url == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        String result;

        try {
            result = ProxyManager.otherHandler().launchBrowser(url, TimeConstant.FOUR_SECONDS);

            if (result == null) {
                result = ErrorCode.C499;
            }
        } catch (Exception e) {
            LOG.message(e.toString());
            result = ErrorCode.STB_ETC;
        }

        return result;
    }

    /**
     * VOD 재생 - 심리스 서비스만 사용(CTL1032, STBCommand.PlayVODS)
     * @param params
     */
    protected String playVODBySeemless(final Properties params) {
        LOG.message("playVODBySeemless(CTL1032)");

        if (ProxyManager.otherHandler().isLimitedWatchingTime()) {
            return ErrorCode.WONT_RUN_CAUSE_LIMITED_WATCHING_TIME; // KTKIDSCARE-82
        }

        if (params.getProperty(Constants.CON_ID) == null) { // VOD Asset ID
            LOG.message("playVODBySeemless, CON_ID is NULL.");
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        new Thread("playVODBySeemless thread") {
            public void run() {

                LOG.message("playVODBySeemless, postMessage=["
                            + KeyConstant.METHOD + ": hp_watchContentForced, "
                            + KeyConstant.CONST_ID + ": " + params.getProperty(Constants.CON_ID) + "]");

                if (changeToVODPlayState("playVODBySeemless")) {
                    return ;
                }

                HomePortal.hp_watchContentForced(params);
            }
        }.start();

        return ErrorCode.SUCCESS;
    }

    /**
     * 시청시간제한 상태이면 시청시간제한 상태를 해제해야한다.
     * 그러나, 시나리오가 시청시간제한 상태이면 오류코드를 전달한다.
     * 그래서 체크해서 해제하는 코드 삭제함!
     * @return
     */
    private boolean changeToVODPlayState(String callMethod) {
        ProxyManager.navigator().changeStateToAVWatchingWhenStandby();

        // 데이타 채널일 경우 VOD 바로 전환하면 문제 많아서 sleep : 전환 실패 or 이어보기 팝업 포커스 잃음 등등...
        boolean needVodStartDelay = ProxyManager.navigator().changePromoChannelWhenDataService();
        // workaround : 먼저 가이드 채널로 튜닝을 한 후 VOD 전환하도록 한다.

        boolean isError = false;

        if (needVodStartDelay) {
            // 박스가 켜질 때 VOD 이어보기와 같은 팝업이 뜨면 팝업이 사라지는 문제가 있다.
            // 이를 피해가기 위해서 sleep 을 준다.
            isError = Util.hasErrorForSleep(callMethod);
        }

        return isError;
    }

    /**
     * 홈메뉴 바로가기(CTL1033, STBCommand.ShowCategory)
     * @param params
     */
    protected String showCategory(Properties params) {
        LOG.message("showCategory(CTL1033)");

        String toApp = params.getProperty(Constants.TO_APP);
        String categoryID = params.getProperty(Constants.CATEGORY_ID);

        if (toApp == null || categoryID == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        String result;

        try {
            LOG.message("showCategory, app's aid="
                        + toApp + "(0x3001=12289), postMessage=[" + KeyConstant.METHOD
                        + " : hp_showCategory, " + KeyConstant.CAT_ID + " : " + categoryID + "]");

            HomePortal.hp_showCategory(categoryID);

            result = ErrorCode.SUCCESS;
        } catch (Exception e) {
            result = ErrorCode.STB_ETC;
        }

        return result;
    }

    /**
     * 앱 실행하기(CTL1034, STBCommand.ExecuteApp)
     * @param params
     */
    protected String executeApp(Properties params) {
        LOG.message("executeApp(CTL1034)");

        String toApp = params.getProperty(Constants.TO_APP);
        String msg = params.getProperty(Constants.MSG);

        if (toApp == null || msg == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        LOG.message("executeApp, toApp=" + toApp + ", msg=" + msg);

        String result;

        try {
            Kidscare.passMessage(msg, toApp);

            result = ErrorCode.SUCCESS;
        } catch (Exception e) {
            LOG.message(e.toString());
            result = ErrorCode.STB_ETC;
        }

        return result;
    }

    /**
     * 리모컨 찾기(CTL1035, Remocon.Find)
     * @param params CODE : 0(리모컨 찾기 요청)
     */
    protected String findRemocon(Properties params) {
        LOG.message("findRemocon(CTL1035)");

        String code = params.getProperty(Constants.CODE);

        if (code == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        if (!Constants.CODE_FIND_REMOCON.equals(code)) { // KTKIDSCARE-12
            return ErrorCode.C112;
        }

        String supportedBluetooth = System.getProperty(Constants.SYSTEM_PROPERTY_KEY_KT_BLE_RCU);
        String resultCode = ErrorCode.C551;

        if (Constants.SUPPORTED.equalsIgnoreCase(supportedBluetooth)) {
            resultCode = ProxyManager.keyHandler().findRCU() ? ErrorCode.C000 : ErrorCode.C552;
        }

        return resultCode;
    }

    /**
     * 앱 실행 후 응답결과 전달하기(CTL1036, STBCommand.ExecuteAppWith)
     * @param params
     */
    protected String executeAppWith(Properties params) {
        LOG.message("executeAppWith(CTL1036)");

        String toApp = params.getProperty(Constants.TO_APP);
        String msg = params.getProperty(Constants.MSG);

        if (toApp == null || msg == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        LOG.message("executeAppWith, toApp=" + toApp + ", msg=" + msg);

        HashMap result = Kidscare.passMessageCallback(toApp, msg);

        if (result == null) {
            return ErrorCode.STB_ETC;
        }

        String resultCode;
        if (result.isEmpty()) {
            LOG.message("executeAppWith, empty in the Result Map");
            resultCode = ErrorCode.C699;
        } else {
            resultCode = (String) result.get(KeyConstant.RESULT_CODE);

            if (result.containsKey(KeyConstant.RESPONSE_MESSAGE)) {
                resultCode += CharConstant.CHAR_CARET + result.get(KeyConstant.RESPONSE_MESSAGE);
            }
        }

        LOG.message("executeAppWith, resultCode=" + resultCode);

        return resultCode;
    }

    /**
     * STB 자동 전원온오프 설정(CTL1037, STBCommand.setAutoOnOff)
     * @param params TIMEON, TIMEOFF, RPT_SET
     */
    protected String setAutoOnOff(Properties params) {
        LOG.message("setAutoOnOff(CTL1037)");

        String timeOn = params.getProperty(Constants.TIMEON); // HHmm
        String timeOff = params.getProperty(Constants.TIMEOFF); // HHmm
        String repeat = params.getProperty(Constants.RPT_SET);

        if (timeOn == null || timeOff == null || repeat == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        if (timeOn.length() != 4 || timeOff.length() != 4) {
            return ErrorCode.C106;
        }

        HashMap result = HomePortal.hp_setAutoPower(timeOn, timeOff, repeat);
        if (result == null) {
            return ErrorCode.STB_ETC;
        }

        String resultCode;
        if (result.isEmpty()) {
            LOG.message("setAutoOnOff, empty in the Result Map");
            resultCode = ErrorCode.C699;
        } else {
            resultCode = (String) result.get(KeyConstant.RESULT_CODE);
        }

        LOG.message("setAutoOnOff, resultCode=" + resultCode);

        return resultCode;
    }

    private String turnOffNow(Properties params) {
        // 즉시 끄기 하면, 끄기 예약은 삭제되고, 잠김 상태가 되어야 한다
        if (ReserveManager.getInstance().isTurnOffStarted()) {
            LOG.message("turnOff(Now), already turn off started");
            return ErrorCode.SUCCESS;
        }

        LOG.message("turnOff(Now), immediate turn off request");

        if (ProxyManager.navigator().isNotRunningBySTB()) {
            LOG.message("turnOff(Now), STBState is not RUNNING! STBState="
                    + ProxyManager.stateHandler().getSTBState());
            return ErrorCode.SUCCESS;
        }

        final String restrict = params.getProperty(Constants.CFG_TM);
        final String[] messages = PopupManager.getInstance().getMessageAfterCheck(params);

        LOG.message("turnOff(Now), STBState is RUNNING!");
        new Thread("Thread.intoStandby") {
            public void run() {
                ReserveManager.getInstance().setTurnOffStarted();

                PopupManager.getInstance().showMessageByThread(messages);

                LOG.message("turnOff(Now), STBState is RUNNING, sleep 7 secs");
                try {
                    Thread.sleep(TimeConstant.SEVEN_SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                LOG.message("turnOff(Now), STBState is RUNNING, after sleep 7 secs");
                // 즉시 끄기 : 대기 모드로 빠지고 시청시간제한에 걸리게 함.
                ProxyManager.stateHandler().changeStateToStandby();

                boolean setupRestrict = !Constants.SET_OFF.equals(restrict); // KTKIDSCARE-17

                if (setupRestrict) { // 즉시끄기는 00:00 ~ 23:59 까지 시청제한을 건다.
                    ProxyManager.navigator().setLimitedWatchingTime();
                    LOG.message("turnOff(Now), setting reserve restriction time = [00:00]~[23:59]");
                }

                // 끄기 예약이 되어있으면 해제해 줌.
                ReserveManager.getInstance().cancelReserveSTBTurnOff();
            }
        }.start();

        return ErrorCode.SUCCESS;
    }

    private String turnOffBooking(Properties params) {
        String startTime = params.getProperty(Constants.START_TIME);
        String endTime = params.getProperty(Constants.END_TIME);

        if (startTime == null || endTime == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        String restrict = params.getProperty(Constants.CFG_TM);
        String[] messages = PopupManager.getInstance().getMessageAfterCheck(params);
        boolean changeRestrictWatchTime = !Constants.SET_OFF.equals(restrict); // KTKIDSCARE-17
        String result = ErrorCode.SUCCESS;

        try {
            ReserveManager.getInstance().reserveSTBTurnOff(startTime, endTime, messages, changeRestrictWatchTime);

            LOG.message("turnOff(Booking), setting reserve restriction time = ["
                        + startTime + "]~[" + endTime + "]");

            PopupManager.getInstance().showMessageByThread(messages);
        } catch (NumberFormatException e) {
            result = ErrorCode.INVALID_NUMBER_VALUE;
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            result = ErrorCode.C111;
        } catch (Exception e) {
            e.printStackTrace();
            result = ErrorCode.C199;
        }

        return result;
    }

    private String turnOffUnlock(Properties params) {
        ProxyManager.navigator().cancelAndClearLimitedWatchingTime();
        PopupManager.getInstance().showMessageByThread(params);

        LOG.message("turnOff(Unlock), set free from restrict time, and hide popup");

        return ErrorCode.SUCCESS;
    }

    private String turnOffUnbooking(Properties params) {
        ReserveManager.getInstance().cancelReserveSTBTurnOff();
        PopupManager.getInstance().showMessageByThread(params);

        LOG.message("turnOff(Unbooking), set free from reserved restrict time");

        return ErrorCode.SUCCESS;
    }

    private boolean invalidChangeType(String type) {
        return !Constants.TYPE_NOW_CHANGE.equals(type)
                && !Constants.TYPE_RESERVATION_CHANGE.equals(type)
                && !Constants.TYPE_RESERVATION_CHANGE_CANCLE.equals(type);
    }

    private boolean invalidRunMode(String runMode) {
        return !Constants.RUN_MODE_IDLE.equals(runMode) && !isChannelOrVODRunMode(runMode);
    }

    private boolean isChannelOrVODRunMode(String run_mode) {
        return Constants.RUN_MODE_CHANNEL.equals(run_mode)
                || Constants.RUN_MODE_VOD.equals(run_mode)
                || Constants.RUN_MODE_DATA_SERVICE.equals(run_mode);
    }

    private String sendKey(int keyCode) {
        boolean success = ProxyManager.keyHandler().sendKey(keyCode);
        return getResultByKey(success);
    }

    private String getResultByKey(boolean success) {
        return success ? ErrorCode.SUCCESS : ErrorCode.STB_ETC;
    }
}
