package com.kt.remotecontrol.interlock.nav;

import com.alticast.navigator.qts.QTSController;
import com.kt.navsuite.core.Channel;
import com.kt.navsuite.core.ChannelRing;
import com.kt.navsuite.core.ChannelSelectionEvent;
import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.interlock.app.Observer;
import com.kt.remotecontrol.manager.StatusManager;
import com.kt.remotecontrol.model.TVStatus;
import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.ErrorCode;
import com.kt.remotecontrol.util.KeyConstant;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.WorkingConfig;

import org.dvb.si.SIService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

public class Navigator {

    private static final Log LOG = new Log("Navigator");

    public static final int REQUESTED = ChannelSelectionEvent.REQUESTED;
    public static final int USER_BLOCKED = ChannelSelectionEvent.USER_BLOCKED;
    public static final int SUCCEEDED = ChannelSelectionEvent.SUCCEEDED;
    public static final int FAILED = ChannelSelectionEvent.FAILED;

    private NavHandlers navHandlers;

    public Navigator(NavHandlers navHandlers) {
        this.navHandlers = navHandlers;
    }

    public KeyHandler keyHandler() {
        return navHandlers.keyHandler();
    }

    public ChannelHandler channelHandler() {
        return navHandlers.channelHandler();
    }

    public StateHandler stateHandler() {
        return navHandlers.stateHandler();
    }

    public AppHandler appHandler() {
        return navHandlers.appHandler();
    }

    public OtherHandler otherHandler() {
        return navHandlers.otherHandler();
    }

    public void init() {
        otherHandler().setVersion(WorkingConfig.VERSION); // KTKIDSCARE-2
        addListener();
    }

    public void addListener() {
        channelHandler().addChannelEventListener();
        stateHandler().addSTBStateListener();
    }

    public void removeListener() {
        channelHandler().removeChannelEventListener();
        stateHandler().removeSTBStateListener();
    }

    public boolean isStandbyBySTB() {
        return stateHandler().getSTBState() == StateHandler.STB_STANDBY;
    }

    public boolean isRunningBySTB() {
        return stateHandler().getSTBState() == StateHandler.STB_RUNNING;
    }

    public boolean isNotRunningBySTB() {
        return !isRunningBySTB();
    }

    public boolean isStandby() {
        return stateHandler().getState() == StateHandler.STANDBY;
    }

    public boolean isDataService() {
        return stateHandler().getState() == StateHandler.DATA_SERVICE;
    }

    /**
     * channel number로 채널 변경
     */
    public boolean changeChannel(int channelNumber, boolean isSkylifeChannel, String argument) {
        LOG.message("changeChannel, channelNumber=" + channelNumber
                    + ", isSkylifeChannel=" + isSkylifeChannel + ", argument=" + argument);

        Channel current = channelHandler().getCurrentChannel();
        if (current == null) {
            return false;
        }

        Channel findChannel = getChannelByService(channelNumber, isSkylifeChannel);

        if (current == findChannel) {
            LOG.message("changeChannel, it is same with current, ignore");
            return true;
        } else if (findChannel == null) {
            return false;
        }

        LOG.message("changeChannel, channel number=" + findChannel.getMajorChannel()
                + ", serive id=" + findChannel.getSIService().getServiceID());

        otherHandler().setClipboardContents(argument);

        channelHandler().changeChannel(findChannel);

        return true;
    }


    public void changeStateToAVWatchingWhenStandby() {
        if (!isStandby()) {
            return ;
        }

        stateHandler().changeStateToAVWatching();
    }

    public boolean changePromoChannelWhenDataService() {
        if (!isDataService()) {
            return false;
        }

        changePromoChannel();
        return true;
    }

    public boolean changePromoChannel() {
        Channel promoChannel = channelHandler().getPromoChannel();

        if (promoChannel == null) {
            LOG.message("[Navigator] changePromoChannel, PromoChannel is NULL!");
            return false;
        }

        SIService siService = promoChannel.getSIService();

        if (siService == null) {
            LOG.message("[Navigator] changePromoChannel, PromoChannel of SIService is NULL!");
            return false;
        }

        return changeChannel(siService.getServiceID());
    }

    /**
     * serviceID로 채널 변경
     */
    public boolean changeChannel(int serviceID) {
        LOG.message("[Navigator] changeChannel, serviceID=" + serviceID);

        Channel current = channelHandler().getCurrentChannel();
        if (current != null && current.getSIService().getServiceID() == serviceID) {
            LOG.message("[Navigator] changeChannel, it is same with current, ignore");
            return true;
        }

        String ringName = getChannelRingName(false);
        Channel[] channels = channelHandler().getChannels(ringName);
        int count = channels.length;
        Channel channel;

        for (int i = 0; i < count; i++) {
            channel = channels[i];

            if (channel != null && channel.getSIService().getServiceID() == serviceID) {
                LOG.message("[Navigator] changeChannel, to:" + channel);
                channelHandler().changeChannel(channel);

                return true;
            }
        }

        return false;
    }

    public boolean changeChannel(String value) {
        boolean result = false;

        try {
            result = changeChannel(Integer.parseInt(value));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public Channel getChannelByService(int channelNumber, boolean isSkylifeChannel) {
        String ringName = getChannelRingName(isSkylifeChannel);
        ChannelRing channelRing = channelHandler().getChannelRing(ringName);
        Channel channel = channelRing.getChannel(channelNumber);

        LOG.message("getChannelByService, channel=" + (channel == null ? "null"
                    : "" + channel.getNumber()));

        return channel;
    }
    /**
     * id
     * bound : locator
     * unbound : app id
     */
    public boolean launchApp(String id) {
        LOG.message("launchApp, id=" + id);

        boolean result;
        if (isLocator(id)) { // bound app
            result = launchBoundApp(id);
        } else { // unbound app
            Observer.obs_startUnboundApplication(id);
            result = true;
        }

        return result;
    }

    private boolean launchBoundApp(String id) {
        try {
            if (!id.startsWith("dvb://")) {
                id = "dvb://" + id;
            }

            Channel ch = channelHandler().getChannel(id);

            if (ch == null) {
                LOG.error("launchApp(bound), channel is null!");
                return false;
            }

            LOG.message("launchApp(bound), changeChannel(" + ch + ")");

            channelHandler().changeChannel(ch);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getCurrentTVState(String uiState) {
        boolean isOTS = otherHandler().isOTS();
        TVStatus tvState = new TVStatus(uiState, isOTS, isKidsMode(), getAppIDByDialog());

        return tvState.toString();
    }

    private boolean isKidsMode() {
        String value = otherHandler().getUseKidsMode();
        return "ON".equals(value);
    }

    private String getAppIDByDialog() {
        String value = otherHandler().getInfo("voice.appID");

        if ("undefine" == value) {
            return "";
        }

        return value;
    }

    private boolean isLocator(String id) {
        return id.indexOf(".") > 0;
    }

    private String getChannelRingName(boolean skylifeChannelMode) {
        String ringName = ChannelRing.ALL;
        if (QTSController.OTS && skylifeChannelMode) {
            ringName = ChannelRing.SKYLIFE_CHANNELS_SATELLITE;
        }

        return ringName;
    }

    public Channel findChannel(String serviceID) {
        return findChannel(Integer.parseInt(serviceID));
    }

    public Channel findChannel(int serviceId) {
        Channel[] allChannels = getIPTVChannels(); // 전체 채널
        int count = allChannels.length;
        Channel channel;

        for (int i = 0; i < count; i++) {
            channel = allChannels[i];
            if (channel.getSIService().getServiceID() == serviceId) {
                return channel;
            }
        }

        return null;
    }

    /**
     * serviceID로 채널번호 얻기.
     */
    public int getChannelNumber(int serviceID) {
        Channel[] chs = getIPTVChannels();
        for (int i = 0; i < chs.length; i++) {
            if (chs[i].getSIService().getServiceID() == serviceID) {
                return chs[i].getNumber();
            }
        }
        return 0;
    }

    /**
     * IPTV 채널 리스트 가져오기
     *
     * @return
     */
    public Channel[] getIPTVChannels() {
        Channel[] vChannels = getChannels(ChannelRing.VIDEO_SERVICE);
        Channel[] aChannels = getChannels(ChannelRing.AUDIO);
        Channel[] iptvChannels = new Channel[vChannels.length + aChannels.length];

        System.arraycopy(vChannels, 0, iptvChannels, 0, vChannels.length);
        System.arraycopy(aChannels, 0, iptvChannels, vChannels.length, aChannels.length);

        return iptvChannels;
    }

    public void clearLimitedChannel() {
        channelHandler().clearChannelRing(ChannelRing.BLOCKED);
    }

   public String getLimitedWatchingTimeResult() {
        // "N|20080411|18:04|18:10|1|Y"
        String value = otherHandler().getLimitedWatchingTime();
        String[] values = getTokenizedStringsWithDelimiter(value, CharConstant.CHAR_VERTICAL, true);

        String startTime = values[2];
        String endTime = values[3];
        String repeat = values[4];
        String setup = values[5];

        StringBuffer data = new StringBuffer();
        if (setup.equals("Y")) { // Y: 설정, N: 해제
            data.append("1"); // 설정
        } else {
            data.append("0"); // 해제
        }
        data.append(CharConstant.CHAR_VERTICAL);

        String startTimeOnlyNum = startTime.substring(0, 2) + startTime.substring(3);
        String endTimeOnlyNum = endTime.substring(0, 2) + endTime.substring(3);

        data.append(startTimeOnlyNum).append(CharConstant.CHAR_VERTICAL);
        data.append(endTimeOnlyNum).append(CharConstant.CHAR_VERTICAL);

        if (repeat.equals("0")) { // 0: 반복, 1: 한번만
            data.append("1"); // 매일
        } else {
            data.append("0"); // 한 번
        }

        return data.toString();
    }

    /**
     * Delimiter 로 구분된 하나의 Source String 을 Delimiter 단위로 분해하여
     * Array 타입의 Strings 결과를 Return한다.
     *
     * @param szSource: source string
     * @param szDelimiter: delimiter 로 사용할 string
     * @param bIncludeNullString: Null String도 Array의 element도 잡는다.
     *
     *      예) szSource: "|123|456||789|", szDelimiter: "|"
     *              -> Return: Vector {"", "123", "456", "", "789", ""}
     */
    private String[] getTokenizedStringsWithDelimiter(String szSource, String szDelimiter, boolean bIncludeNullString) {
        if (szSource == null || szDelimiter == null) {
            throw new IllegalArgumentException();
        }

        if (szSource.length() == 0) {
            return new String[] { "" };
        }

        StringTokenizer tokenizer = new StringTokenizer(szSource, szDelimiter, bIncludeNullString);
        Vector vctString = new Vector();
        String szTemp = "";
        boolean bNextDelimiter = false;

        while (tokenizer.hasMoreTokens()) {

            szTemp = tokenizer.nextToken();

            if (szTemp.equals(szDelimiter)) {
                if (!bNextDelimiter) {
                    vctString.addElement("");
                }

                bNextDelimiter = false;
            } else {
                vctString.addElement(szTemp);
                bNextDelimiter = true;
            }
        }

        if (!bNextDelimiter) {
            vctString.addElement("");
        }

        int count = vctString.size();
        String[] values = new String[count];

        for (int i = 0; i < count; i++) {
            values[i] = (String) vctString.elementAt(i);
        }

        return values;
    }

    /**
     * 잠금 해제. -> 시청제한 풀기.
     * @return true or false
     */
    public boolean cancelAndClearLimitedWatchingTime() {
        LOG.message("[Navigator] cancelAndClearLimitedWatchingTime");

        String value = otherHandler().getLimitedWatchingTime();
        if (value != null) {
            otherHandler().cancelLimitedWatchingTime();
        }

        clearLimitedWatchingTime();

        return true;
    }

    public void clearLimitedWatchingTime() {
        String value = getClearTimeLimit();
        otherHandler().setLimitedWatchingTime(value);
    }

    public void setLimitedWatchingTime(String startHour, String startMin, String endHour, String endMin, boolean repeat) {
        String value = getOnTimeLimit(startHour, startMin, endHour, endMin, repeat);
        otherHandler().setLimitedWatchingTime(value);
    }

   public void setLimitedWatchingTime(boolean onOff, String startTime, String endTime, boolean repeat) {
        String value = getTimeLimitString(onOff, startTime, endTime, repeat);
        otherHandler().setLimitedWatchingTime(value);
    }

    // 시청 제한 시간 설정을 위한 문자열 얻기
    private String getTimeLimitString(boolean onOff, String startTime, String endTime, boolean repeat) {
        try {
            String startHour = startTime.substring(0, 2);
            String startMinute = startTime.substring(2, 4);
            String endHour = endTime.substring(0, 2);
            String endMinute = endTime.substring(2, 4);

            return getTimeLimitString(onOff, startHour, startMinute, endHour, endMinute, repeat);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return getTimeLimitString(onOff, "00", "00", "00", "00", repeat);
    }

    private String getClearTimeLimit() {
        return getTimeLimitString(false, "00", "00", "00", "00", false);
    }

    public void setLimitedWatchingTime() {
        String value = getOnTimeLimit("00", "00", "23", "59", false);
        otherHandler().setLimitedWatchingTime(value);
    }

    private String getOnTimeLimit(String startHour, String startMin, String endHour, String endMin, boolean repeat) {
        return getTimeLimitString(true, startHour, startMin, endHour, endMin, repeat);
    }

    private String getTimeLimitString(boolean onOff, String start_hour, String start_min, String end_hour,
                                             String end_min, boolean repeat) {

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        String today = format.format(Calendar.getInstance().getTime());

        StringBuffer sb = new StringBuffer();
        sb.append("N").append(CharConstant.CHAR_VERTICAL);
        sb.append(today).append(CharConstant.CHAR_VERTICAL);
        sb.append(start_hour).append(CharConstant.CHAR_COLON).append(start_min).append(CharConstant.CHAR_VERTICAL);
        sb.append(end_hour).append(CharConstant.CHAR_COLON).append(end_min).append(CharConstant.CHAR_VERTICAL);

        if (repeat) {
            sb.append("0"); // 반복
        } else {
            sb.append("1"); // 한번만
        }

        sb.append(CharConstant.CHAR_VERTICAL);

        if (onOff) {
            sb.append("Y");
        } else {
            sb.append("N");
        }

        return sb.toString();
    }

    public String updateLimitedChannel(String chList) {
        int serviceID = channelHandler().getServiceIDOfCurrentChannel();
        boolean needCurrentBlockClear = false;
        StringTokenizer tokenizer = new StringTokenizer(chList, CharConstant.CHAR_SEMICOLON); // 700|0;719|1

        while (tokenizer.hasMoreTokens()) {
            String values = tokenizer.nextToken(); // 700|0
            String sid = getSIDByToken(values);

            if (shouldAdd(values)) { // 제한 추가
                LOG.message("[Navigator] updateLimitedChannel(add), sid=" + sid);

                if (exceedChannelsBy30(getLimitedChannels())) { // 제한채널 개수 초과
                    LOG.message("[Navigator] updateLimitedChannel(add), exceed 30 channels");
                    return ErrorCode.C412;
                }

                addLimitedChannel(sid);
            } else if (shouldRemove(values)) { // 제한 해제
                LOG.message("[Navigator] updateLimitedChannel(remove), sid=" + sid);
                removeLimitedChannel(sid);

                if (Integer.parseInt(sid) == serviceID) {
                    needCurrentBlockClear = true;
                }
            }
        }

        if (needCurrentBlockClear && StatusManager.getInstance().isChannelOrIDLEState()) {
            channelHandler().selectCurrentChannel();
        }

        return null;
    }

    public void clearFavoriteChannel() {
        channelHandler().clearChannelRing(ChannelRing.FAVORITE);
    }

    public String updateFavoriteChannel(String chList) {
        StringTokenizer tokenizer = new StringTokenizer(chList, CharConstant.CHAR_SEMICOLON); // 700|0;719|1

        while (tokenizer.hasMoreTokens()) {
            String values = tokenizer.nextToken(); // 700|0
            String sid = getSIDByToken(values);

            if (shouldAdd(values)) { // Favorite channel 추가
                LOG.message("[Navigator] updateFavoriteChannel(add), sid=" + sid);

                if (exceedChannelsBy30(getFavoriteChannels())) { // Favorite 채널 개수 초과
                    LOG.message("[Navigator] updateFavoriteChannel(add), exceed 30 channels");
                    return ErrorCode.C412;
                }

                addFavoriteChannel(sid);
            } else if (shouldRemove(values)) { // Favorite 채널  해제
                LOG.message("[Navigator] updateFavoriteChannel(remove), sid=" + sid);

                removeFavoriteChannel(sid);
            }
        }

        return null;
    }

    public void clearHiddenChannel() {
        channelHandler().clearChannelRing(ChannelRing.SKIPPED);
    }

    public String updateHiddenChannel(String chList) {
        StringTokenizer tokenizer = new StringTokenizer(chList, CharConstant.CHAR_SEMICOLON); // 700|0;719|1

        while (tokenizer.hasMoreTokens()) {
            String values = tokenizer.nextToken(); // 700|0
            String sid = getSIDByToken(values);

            if (shouldAdd(values)) { // Hidden channel 추가
                LOG.message("[Navigator] updateHiddenChannel(add), sid=" + sid);

                if (exceedChannelsBy80(getHiddenChannels())) { // Hidden 채널개수 초과
                    LOG.message("[Navigator] updateHiddenChannel(add), exceed 80 channels");
                    return ErrorCode.C412;
                }

                addHiddenChannel(sid);
            } else if (shouldRemove(values)) { // Hidden channel 해제
                LOG.message("[Navigator] updateHiddenChannel(remove), sid=" + sid);

                removeHiddenChannel(sid);
            }

        }

        return null;
    }

    private String getSIDByToken(String token) {
        int idx = token.indexOf(CharConstant.CHAR_VERTICAL);
        return token.substring(0, idx);
    }

    private boolean shouldAdd(String value) {
        return value.endsWith(Constants.STATE_SET);
    }

    private boolean shouldRemove(String value) {
        return value.endsWith(Constants.STATE_CLEAR);
    }

    public Channel[] getChannels(String ringName) {
        return channelHandler().getChannels(ringName);
    }

    /**
     * 제한 채널 조회
     * @return Channel[]
     */
    public Channel[] getLimitedChannels() {
        Channel[] channels = getChannels(ChannelRing.BLOCKED);
        LOG.message("[NavigatorProxy] getLimitedChannels, channel's count:" + channels.length);
        return channels;
    }

    private boolean addLimitedChannel(String sid) {
        Channel channel = findChannel(sid);
        if (channel == null) {
            LOG.message("[Navigator] addLimitedChannel, can't find service id:" + sid);
            return false;
        }

        Channel[] channels = getLimitedChannels(); // 현재 설정된 제한채널
        if (hasChannel(channels, channel)) {
            return false;
        }

        setChannelRingByAdd(ChannelRing.BLOCKED, channels, channel);

        LOG.message("[Navigator] addLimitedChannel, new added:" + sid);

        return true;
    }

    private boolean removeLimitedChannel(String sid) {
        Channel channel = findChannel(sid);

        if (channel == null) {
            LOG.message("[Navigator] removeLimitedChannel, can't find service id:" + sid);

            return false;
        }

        Channel[] channels = getLimitedChannels(); // 현재 설정된 제한채널

        return setChannelRingByRemove(ChannelRing.BLOCKED, channels, channel);
    }

    public Channel[] getFavoriteChannels() {
        Channel[] channels = getChannels(ChannelRing.FAVORITE);
        LOG.message("[NavigatorProxy] getFavoriteChannels, channel's count:" + channels.length);
        return channels;
    }

    private boolean addFavoriteChannel(String sid) {
        Channel channel = findChannel(sid);

        if (channel == null) {
            LOG.message("[Navigator] addFavoriteChannel, can't find service id:" + sid);
            return false;
        }

        Channel[] channels = getFavoriteChannels(); // 현재 설정된 Favorite 채널
        if (hasChannel(channels, channel)) {
            return false;
        }

        setChannelRingByAdd(ChannelRing.FAVORITE, channels, channel);

        LOG.message("[Navigator] addFavoriteChannel, new added:" + sid);

        return true;
    }

    private boolean removeFavoriteChannel(String sid) {
        Channel channel = findChannel(sid);

        if (channel == null) {
            LOG.message("[Navigator] removeFavoriteChannel, can't find service id:" + sid);

            return false;
        }

        Channel[] channels = getFavoriteChannels(); // 현재 설정된 Favorite 채널

        return setChannelRingByRemove(ChannelRing.FAVORITE, channels, channel);
    }

    public Channel[] getHiddenChannels() {
        Channel[] channels = getChannels(ChannelRing.SKIPPED);
        LOG.message("[Navigator] getHiddenChannels, channel's count=" + channels.length);
        return channels;
    }

    public boolean isPromoChannel(String serviceID) {
        if (serviceID == null) {
            return false;
        }

        boolean isPromoChannel = false;

        try {
            isPromoChannel = isPromoChannel(Integer.parseInt(serviceID));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return isPromoChannel;
    }

    public boolean isPromoChannel(int serviceID) {
        Channel promoChannel = channelHandler().getPromoChannel();
        if (promoChannel == null) {
            return false;
        }

        SIService siService = promoChannel.getSIService();
        if (siService == null) {
            return false;
        }

        return serviceID == siService.getServiceID();
    }

// ================================================
    /**
     * TODO 이런 경우는 어떻게 처리되고 있는지 확인 필요!!
     * @param message
     * @param appID
     */
    public static void talkToApp(String message, String appID) {
        HashMap params = new HashMap();
        params.put(KeyConstant.METHOD, message);

        ProxyManager.appHandler().talkToApp(params, appID);
    }

    private boolean addHiddenChannel(String sid) {
        Channel channel = findChannel(sid);
        if (channel == null) {
            LOG.message("[Navigator] addHiddenChannel, can't find service id:" + sid);
            return false;
        }

        Channel[] channels = getHiddenChannels(); // 현재 설정된 Hidden 채널

        if (hasChannel(channels, channel)) {
            return false;
        }

        setChannelRingByAdd(ChannelRing.SKIPPED, channels, channel);

        LOG.message("[Navigator] addHiddenChannel, new added:" + sid);

        return true;
    }

    private boolean removeHiddenChannel(String sid) {
        Channel channel = findChannel(sid);

        if (channel == null) {
            LOG.message("[Navigator] removeHiddenChannel, can't find service id:" + sid);
            return false;
        }

        Channel[] channels = getHiddenChannels(); // 현재 설정된 숨김 채널

        return setChannelRingByRemove(ChannelRing.SKIPPED, channels, channel);
    }

    private boolean hasChannel(Channel[] channels, Channel channel) {
        int count = channels.length;

        for (int i = 0; i < count; i++) {
            if (channels[i].equals(channel)) {
                LOG.message("[Navigator] hasChannel, already added:" + channel.getNumber()
                            + ", sid=" + channel.getSIService().getServiceID());
                return true;
            }
        }

        return false;
    }

    private void setChannelRingByAdd(String ringName, Channel[] channels, Channel channel) {
        int count = channels.length;

        Channel[] newChannels = new Channel[count + 1];
        System.arraycopy(channels, 0, newChannels, 0, count);
        newChannels[count] = channel;

        channelHandler().setChannelRing(ringName, newChannels);
    }

    private boolean setChannelRingByRemove(String ringName, Channel[] channels, Channel channel) {
        int nowCount = channels.length;
        int findIndex = -1;

        for (int i = 0; i < nowCount; i++) {
            if (channels[i].equals(channel)) {
                findIndex = i;
                break;
            }
        }

        if (findIndex < 0) {
            LOG.message("[Navigator] setChannelRing(remove " +  ringName
                    + "), already removed channel=(" + channel.getNumber() + ", serviceID="
                    + channel.getSIService().getServiceID() + ")");

            return false;
        }

        int count = nowCount - 1;
        Channel[] newChannels = new Channel[count];
        System.arraycopy(channels, 0, newChannels, 0, findIndex);

        if (findIndex < count) {
            int srcPosition = findIndex + 1;
            int length = nowCount - srcPosition;
            System.arraycopy(channels, srcPosition, newChannels, findIndex, length);
        }

        channelHandler().setChannelRing(ringName, newChannels);

        LOG.message("[Navigator] setChannelRing(remove " + ringName + "), new removed channel=("
                + channel.getNumber() + ", serviceID=" + channel.getSIService().getServiceID() + ")");

        return true;
    }

    private boolean exceedChannelsBy30(Channel[] channels) {
        return exceedChannels(channels, 30);
    }

    private boolean exceedChannelsBy80(Channel[] channels) {
        return exceedChannels(channels, 80);
    }

    private boolean exceedChannels(Channel[] channels, int maxCount) {
        int count = channels.length;
        if (count >= maxCount) { // 채널 개수 초과
            LOG.message("[Navigator] exceedChannels, compare(" + count + " >=" + maxCount + ")");
            return true;
        }

        return false;
    }
}
