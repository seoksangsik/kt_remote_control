/*
 *  Copyright (c) 2004 Alticast Corp.
 *  All rights reserved. http://www.alticast.com/
 *
 *  This software is the confidential and proprietary information of
 *  Alticast Corp. ("Confidential Information"). You shall not
 *  disclose such Confidential Information and shall use it only in
 *  accordance with the terms of the license agreement you entered into
 *  with Alticast.
 */
package com.kt.remotecontrol.util;

import com.kt.remotecontrol.manager.StatusManager;

public class Constants {


    private Constants() {
    }

    public static final String PROPERTIES_NAME = "webmw.properties";

    /**
     * 키즈케어 어플의 상품 코드 (region bit)
     */
    public static final String PRODUCT_CODE = "273";

    public static final String PRODUCT_KIDSCARE_CODE_BY_KT = "2445";

    /*
     * 제어 요청은 여기에서 온 것만 통과시킨다.
     */
    public static String CONTROL_SERVER_IP = "rwmpout.megatvdnp.co.kr";

    /**
     * I/F 서버 주소, 동기화 요청할 때만 사용한다.
     */
    public static final String REQUEST_SERVER_IP = "rwmp.megatvdnp.co.kr";

    public static final String TEST_SERVER_IP = "dev.copynpaste.co.kr";

    /**
     * I/F 서버 포트
     */
    public static final int SERVER_PORT = 8088;

    /**
     * 시청내역 누적 I/F 서버 포트
     */
    public static final int SERVER_PORT_WATCH_HISTORY = 8090;

    /**
     * I/F 서버 경로 및 페이지
     */
    public static final String REQUEST_SERVICE = "/ifserver/stb/interfaceCall.do";

    /**
     * I/F Server 에서 RemoteAgent 에 접속하는 httpd 포트번호
     */
    public static final int SERVICE_PORT = 1717;

    /**
     * 리모콘 어플에서 마우스 이벤트를 보내기 위해서 접속하는 TCP Port
     */
    public static final int TCP_SERVICE_PORT = 1718;

    /**
     * 호출 들어오는 URI, 이 URI가 아니면 원격제어 서비스를 하지 않는다.
     */
    public static final String CALL_URI = "/ipstb/rtcontrol";

    public static final String NOTICE_URI = "/notice";

    public static final String HDS_LIVE_URI = "https://svcm.homen.co.kr";
    public static final String HDS_TEST_URI = "https://125.147.35.170";

    public static final String PIN = "/HDSIMNPWebSVC/HDSIMNPWebSVC.asmx/";
    public static final String HDS_SERVICE_DBLOGON = PIN + "HDSDBLogon";
    public static final String HDS_SERVICE_PINUPDATE = PIN + "HDSPinUpdate";
    public static final String HDS_SERVICE_GETHDSTOKEN = PIN + "GetHDSToken";

    public static final String ON_LINE = "/HDSOnLineWebSVC/HDSOnLineWebSVC.asmx/";
    public static final String HDS_SERVICE_ON_SCREEN_JOIN = ON_LINE + "OnScreenRegSVC";
    public static final String HDS_SERVICE_ON_SCREEN_CANCEL = ON_LINE + "OnScreenCnclSVC";

    public static final String AMOC_LIVE_URI = "https://webui.ktipmedia.co.kr";
    public static final String AMOC_SERVICE_BUY_CONTENTS = "/amoc-api/vod/buy/in-cash";

    public static final String KAKAO_LIVE_URI = "http://ollehtvplay.ktipmedia.co.kr";
    public static final String KAKAO_SERVICE_EMOTICON = "/WEB/image_icon.xml";


    public static final int SUCCESS = 0;
    public static final int NETWORK_FAIL = 1;
    public static final int AUTHROZED_FAIL = 2;

    public static final int SYNC_RETRY_CNT = 3;  // 제어서버 동기화 실패시 재시도 회수
    public static final int SYNC_RETRY_GAP = 60000; // 제어서버 동기화 실패시 재시도 간격

    // Channel Mode for change channel
    public static final String CH_MODE_SKYLIFE = "0"; // OTS(Satellite Channel)
    public static final String CH_MODE_KT = "1"; // OTV(KT Olleh TV Channel)

    // 파라미터의 값
    public static final String STB_STATE_OFF = "0";
    public static final String STB_STATE_STANDBY = "1";
    public static final String STB_STATE_RUNNING = "2";

    public static final String TYPE_NOW_CHANGE = "0"; // 즉시전환
    public static final String TYPE_RESERVATION_CHANGE = "1"; // 예약 전환
    public static final String TYPE_RESERVATION_CHANGE_CANCLE = "2"; // 예약 전환 취소

    public static final String RUN_MODE_IDLE = StatusManager.WS_IDLE + "";
    public static final String RUN_MODE_CHANNEL = StatusManager.WS_CHANNEL + "";
    public static final String RUN_MODE_VOD = StatusManager.WS_VOD + "";
    public static final String RUN_MODE_DATA_SERVICE = StatusManager.WS_DATASERVICE + "";
    public static final String RUN_MODE_BROWSER = StatusManager.WS_FULL_BROWSER + ""; // only Web Smart Box

    public static final String MSG_YN_EMPTY_MSG = "";
    public static final String MSG_YN_NO_MSG = "0";
    public static final String MSG_YN_SHOW_MSG = "1";

    public static final String ALL_OFF_EACH_CHANNEL = "0";
    public static final String ALL_OFF_ALL_CHANNEL = "1";

    public static final String OFF_TYPE_NOW_TURN_OFF = "0";
    public static final String OFF_TYPE_RESERVATION = "1";
    public static final String OFF_TYPE_UNLOCK = "2";
    public static final String OFF_TYPE_RESERVATION_CANCLE = "3";

    public static final String STATE_CLEAR = "0";
    public static final String STATE_SET = "1";

    public static final String STATE_ONCE = "0";
    public static final String STATE_REPEAT = "1";

    public static final String CTL1004 = "CTL1004";

    public static final String SSO1003 = "SSO1003"; // 키즈케어 가입
    public static final String SSO1004 = "SSO1004"; // 키즈케어 해지

    public static final String ETC0104 = "ETC0104"; // 최초 상태 알림
    public static final String ETC0105 = "ETC0105"; // [신규] Push 메시지 ? 켜짐 알림
    public static final String ETC0301 = "ETC0301"; // 메가티비 동기화
    public static final String ETC0304 = "ETC0304"; // 시청내역 누적
    public static final String ETC1001 = "ETC1001";  // 원격 구매

    // 파라미터 인자 상수
    public static final String BUY_YN = "BUY_YN";
    public static final String CFG_AD = "CFG_AD";
    public static final String CFG_AG = "CFG_AG";
    public static final String CFG_CH = "CFG_CH";
    public static final String CFG_PC = "CFG_PC";
    public static final String CFG_TM = "CFG_TM";
    public static final String CMD = "CMD";
    public static final String CONTENTS_ID = "CONTENTS_ID";
    public static final String DOWNLOAD_LIST = "DOWNLOAD_LIST";
    public static final String END_TIME = "END_TIME";
    public static final String HP_NO = "HP_NO";
    public static final String ID = "ID";
    public static final String NAME = "NAME";
    public static final String CH_TYPE = "CH_TYPE";
    public static final String NEW_PASSWORD = "NEW_PASSWORD";
    public static final String OFF_TYPE = "OFF_TYPE";
    public static final String PASSWD = "PASSWD";
    public static final String MSG = "MSG";
    public static final String MSG_YN = "MSG_YN";
    public static final String MSG_TIME = "MSG_TIME";
    public static final String MSG_TITLE = "MSG_TITLE";
    public static final String MSG_TYPE = "MSG_TYPE";
    public static final String RUN_MODE = "RUN_MODE";
    public static final String SAID = "SAID";
    public static final String START_TIME = "START_TIME";
    public static final String STB_IP = "STB_IP";
    public static final String STB_TYPE = "STB_TYPE";
    public static final String POPUP_MSG = "POPUP_MSG";
    public static final String PIN_NO = "PIN_NO";
    public static final String CONTENTS_TYPE = "CONTENTS_TYPE";
    public static final String CONTENTS_NAME = "CONTENTS_NAME";
    public static final String ST_TIME = "ST_TIME";
    public static final String CATEGORY_ID = "CATEGORY_ID";
    public static final String PRICE = "PRICE";
    public static final String AUDIO_MSG = "AUDIO_MSG";
    public static final String MSGUP_YN = "MSGUP_YN";

    public static final String PRD_CD = "PRD_CD"; // 상품 코드 (ICOD only)
    public static final String BQ_CD = "BQ_CD"; // 부케 코드 (ICOD only)
    public static final String ALL_OFF = "ALL_OFF"; // 채널제한 전체 해제
    public static final String CH_LIST = "CH_LIST";
    public static final String SCH_TYPE = "SCH_TYPE";
    public static final String TYPE = "TYPE";
    public static final String TIME = "TIME";

    // 리모콘 어플이 추가되면서 들어간 문자열 상수
    public static final String SVC_CD = "SVC_CD";
    public static final String KEY_CD = "KEY_CD";
    public static final String IN_STR = "IN_STR";
    public static final String CH_ID = "CH_ID";
    public static final String CH_MODE = "CH_MODE";
    public static final String CON_ID = "CON_ID";
    public static final String NOTI_CFG = "NOTI_CFG";
    public static final String NOTI_START = "NOTI_START";
    public static final String NOTI_END = "NOTI_END";
    // UMT 전송 추가
    public static final String TO_APP = "TO_APP";
    public static final String UMT_MSG = "UMT_MSG";
    // 이어보기
    public static final String PRICE_YN = "PRICE_YN";
    public static final String SEEMLEES_TIME = "SEEMLEES_TIME";
    public static final String OTN_SAID = "OTN_SAID";
    // 카테고리
    public static final String CAT_ID = "CAT_ID";

    public static final String KIDSCARE = "KIDSCARE";
    public static final String URL = "URL";
    public static final String ARGU_1 = "ARGU_1";
    public static final String PW_TYPE = "PW_TYPE";

    public static final String IMAGE_URL = "IMAGE_URL"; // 이미지 URL

    public static final String POPUP_RESP = "POPUP_RESP";
    public static final String POPUP_GOTITLE = "POPUP_GOTITLE";
    public static final String POPUP_GOURLTYPE = "POPUP_GOURLTYPE";
    public static final String POPUP_GOURL = "POPUP_GOURL";
    public static final String POPUP_GOAppID = "POPUP_GOappid";
    public static final String HP_UUID = "HP_UUID"; // 유니크한 번호
    public static final String RESP_MSG = "RESP_MSG"; // TV문자 - 회신 메시지
    public static final String IMAGE_SIZE = "IMAGE_SIZE"; // 이미지 사이즈(단위 Byte)
    public static final String IMAGE_FMAT = "IMAGE_FMAT"; // 이미지 포맷(JPG, PNG)
    public static final String CODE = "CODE";

    // CTL1037
    public static final String TIMEON = "TIMEON";
    public static final String TIMEOFF = "TIMEOFF";
    public static final String RPT_SET = "RPT_SET";
    public static final String RPT_SET_ONCE = "0";
    public static final String RPT_SET_REPEAT = "1";

    public static final String SRCH_PARAM = "SRCH_PARAM";
    public static final String SHARPCH_ID = "SHARPCH_ID";

    // PW_TYPE : CFG3001
    public static final String PW_TYPE_ADULT = "1";
    public static final String PW_TYPE_BUY = "2";

    public static final String STB_TYPE_WEB = "3"; // ETC0301

    public static final String STB_WEB_TYPE = "1"; // QRY1001

    public static final String SET_OFF = "0";
    public static final String SET_ON = "1";

    public static final String CHANNEL_UNSET = "0";
    public static final String CHANNEL_SET = "1";

    public static final String CODE_FIND_REMOCON = "0"; // CTL1035

    // CTL1036
    public static final String LINK_HDS = "hds";
    public static final Object LINK_SSO = "sso";
    public static final String LINK_AMOC = "amoc";
    public static final String LINK_POST_MESSAGE = "post_message";

    // POPUP_RESP : ETC1002, ETC1004
    public static final String POPUP_RESP_OFF = "0";
    public static final String POPUP_RESP_TEXT = "1";
    public static final String POPUP_RESP_EMOTICON = "2";

    // POPUP_GOURLTYPE : ETC1002, ETC1004
    public static final String POPUP_GOURL_TYPE_BOUND = "0";
    public static final String POPUP_GOURL_TYPE_UNBOUND = "1";
    public static final String POPUP_GOURL_TYPE_CHILDAPP = "2";

    // ETC1004
    public static final String POPUP_TYPE_NOTICE = "0";
    public static final String POPUP_TYPE_TALK = "1";
    public static final String POPUP_TYPE_PICTURE = "2";

    // ETC1004
    public static final long IMAGE_SIZE_6MB = 6 * 1024 * 1024;
    public static final String IMAGE_FMAT_JPG = "JPG";
    public static final String IMAGE_FMAT_JPEG = "JPEG";
    public static final String IMAGE_FMAT_PNG = "PNG";

    public static final int TOO_MUCH_SIZE = 419;
    public static final int UNSUPPORT_FORMAT = 420;
    public static final int UNSUPPORT_MODEL = 421;
    public static final int OTHER_ERROR = 422;

    // ETC1005
    public static final String MSGUP_YN_ONLY_VOICE = "0";
    public static final String MSGUP_YN_BOTH = "1";

    public static final String SYSTEM_PROPERTY_KEY_KT_BLE_RCU = "KT_BLE_RCU";
    public static final String SUPPORTED = "SUPPORTED";
}
