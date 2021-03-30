'use strict';

acba.define('js:Enum', ['log:kids(Enum)'], 
function (log) {

    var URL = {
        PROPERTIES: 'file:///mnt/usb1/webmw.properties',
        CONTROL: {
            LIVE: 'http://rwmp.megatvdnp.co.kr',
            TEST: 'http://dev.copynpaste.co.kr'
        },
        HDS: {
            LIVE: 'https://svcm.homen.co.kr',
            TEST: 'https://125.147.35.170'
        },
        KAKAO: 'http://ollehtvplay.ktipmedia.co.kr',
        AMOC: 'https://webui.ktipmedia.co.kr',
    },
    ERROR_CODE = {
        SUCCESS_000: '000', // 성공
        MESSAGE_FORMAT_101: '101', // 메세지 형식 오류
        PURCHASE_ALREADY_405: '405',
        PURCHASE_FAIL_408: '408',
        IMAGE_TOO_MUCH_SIZE_419: '419', // 사진 이미지 에러, 사이즈 너무 큼
        IMAGE_UNSUPPORT_FORMAT_420: '420', // 사진 이미지 에러, 포맷 안 맞음
        IMAGE_UNSUPPORT_MODEL_421: '421', // 사진 이미지 에러, 지원하지 않는 모델
        IMAGE_OTHER_ERROR_422: '422', // 사진 이미지 에러, 기타 오류
        IMAGE_HIGH_RESOLUTION_423: '423', // 사진 이미지 에러, 해상도 큰 오류
        TTS_ERROR_430: '430',
        TTS_ERROR_POPUP_431: '431',
        ETC_BY_STB_499: '499', // STB 관련 기타 오류
        CERTIFICATION_501: '501', // HDS 인증 오류
        PASSWORD_502: '502', // 비밀번호 오류
        AUTO_POWER_NO_API_540: '540', // 자동전원온오프 API(hp_get) 없음
        AUTO_POWER_OTHER_ERROR_541: '541', // 자동전원온오프 연동 기타 오류
        AUTO_POWER_NO_SET_542: '542', // 자동전원온오프 설정값 없음
        LINK_HDS_601: '601',
    },
    APP = {
        OID: '4e30',
        ID: {
            Observer: '4e30.3000',
            HomePortal: '4e30.3001',
            Qrator: '4e30.3009',
            kidscare: '4e30.3013',
            MashupManager: '4e30.3020'
        },
        NAME: {
            kidscare: 'kidscare',
            Observer: 'Observer',
            HomePortal: 'HomePortal',
            MashupManager: 'MashupManager'
        },
        TYPE: {
            FullBrowser: '1',
            UnboundApp: '4',
            UnicastApp: '5',
            NAMES: {
                '0': 'no app',
                '1': 'full browser UI',
                '2': 'usb app',
                '3': 'voice app',
                '4': 'unbound app',
                '5': 'unicast app'
            }
        },
        STATUS: {
            NO_GET_HOME_STATE_API: 'SU:000',
            STANDBY: 'SU:001',
            SHOW_HOME_PORTAL: 'SU:200',
            WATCHING_AV: 'SU:201',
            PLAYING_VOD: 'SU:202',
            PAUSE_VOD: 'SU:203',
            VOD_DETAIL: 'SU:204',
            SHOW_APP: 'SU:208',
            TV: {
                WATCHING_AV: 0,
                PLAYING_VOD: 1,
                SHOW_APP: 2,
                SHOW_APP_WHILE_WATCHING_AV: 3,
                SHOW_APP_WHILE_PLAYING_VOD: 4,
                STANDBY: 5,
            },
            VOD: {
                PLAYING: 0,
                PAUSE: 1
            }
        }
    },
    METHOD = {
        OBSERVER: 'obs_',
        OBS_IS_LOADING_UNBOUND_APP: 'obs_isLoadingUnboundApplication',
        OBS_START_UNBOUND_APP: 'obs_startUnboundApplication',
        OBS_STOP_MANAGED_APP: 'obs_stopOBSManagedApplications',
        HOMEPORTAL: 'hp_',
        HP_SEARCH_VOD: 'hp_searchVOD',
        HP_SEARCH_RESULT: 'hp_searchResult',
        HP_WATCH_CONTENT: 'hp_watchContent',
        HP_WATCH_CONTENT_FORCED: 'hp_watchContentForced',
        HP_SHOW_CATEGORY: 'hp_showCategory',
        HP_CHECK_VERSION: 'hp_checkVersion',
        HP_GET_AUTO_POWER: 'hp_getAutoPower',
        HP_SET_AUTO_POWER: 'hp_setAutoPower',
        HP_GET_HOME_STATE: 'hp_getHomeState2',
        QRT_MOVE_SHARP_LINK: 'qrt_moveToSharpLink'
    },
    SERVICE = {
        CODE_ON_SCREEN_JOIN_KIDSCARE: '03',
        REG_PATH_MY_OLLEH: '07',
        KIDSCARE_CODE: '2445', // 키즈케어 부가상품코드
        ID_AUTH_BUY_PIN: 'authbuypin',
        ID_AUTH_BUY_PIN_IPG: 'authbuypinIPG',
        ON_LINE: 'HDSOnLineWebSVC/HDSOnLineWebSVC.asmx',
        PIN: 'HDSIMNPWebSVC/HDSIMNPWebSVC.asmx',
        METHOD_HDS_DBLOGON: 'HDSDBLogon',
        METHOD_HDS_PINUPDATE: 'HDSPinUpdate',
        METHOD_ON_SCREEN_JOIN: 'OnScreenRegSVC',
        METHOD_ON_SCREEN_CANCEL: 'OnScreenCnclSVC',
    },
    VALUE = {
        SCREEN_HEIGHT: 720,
        SERVER_TIMEOUT: 3500,
        SHORTCUT: {
            BOUND: 0,
            UNBOUND: 1,
            CHILD_APP: 2
        },
        LINK: {
            HDS: 'hds',
            AMOC: 'amoc',
            POST_MESSAGE: 'post_message'
        },
        PURCHASE: {
            SUCCESS: 0,
            FAIL: 1,
            ALREADY: 2
        },
        TTS_STATE: {
            START: 0,
            END: 1,
            ERROR: 2,
            Names: [ '(TTS_STATE_START)', '(TTS_STATE_END)', '(TTS_STATE_ERROR)' ]
        }
    },
    KEY = {
        TITLE: 'title',
        TITLE_PREFIX: 'title_prefix',
        MESSAGE: 'message',
        PHONE: 'cellPhone',
        UUID: 'uuid',
        DISPLAY_TIME: 'display_time',
        SHOTCUT_NAME: 'shotcut_name',
        SHOTCUT_TYPE: 'shotcut_type',
        SHOTCUT_URL: 'shotcut_url',
        SHORTCUT_APPID: 'shortcut_appID',
        REPLY: 'reply',
        SERVICE_CODE: 'service_code',
        SAID: 'said',
        REPLY_MESSAGE: 'reply_messge',
        IMAGE_URL: 'image_url',
        ERROR_TYPE: 'error_type',
        REQUEST: 'request',
        EMOTICON_SELECTED_INDEX: 'selectedIndex',
    },
    POPUP = {
        TYPE: {
            NOTICE: 0,
            TALK: 1,
            PICTURE: 2,
            TEXT: 3,
            EMOTICON: 4
        },
        STYLE: {
            NOTICE: 'notice',
            TALK: 'talk',
            PICTURE: 'picture',
            REPLY: 'reply'
        },
        BG_EXPAND: {
            HEIGHT: 'h',
            WIDTH: 'w',
        },
        TITLE_ICON: {
            INFO: 'info',
            MAIL: 'mail',
        },
        COLUMN_SIZE: 20,
        PICTURE_SIZE: {
            WH423: 423,
            MAX_HEIGHT: 477
        }
    },
    CHAR = {
        HYPHEN: '-',
        UNDERSCORE: '_',
        CARET: '^',
        SEMICOLON: ';',
        COLON: ':',
        COMMA: ',',
        PIXEL: 'px',
        DOT: '.'
    };

    return {
        URL: URL,
        ERROR_CODE: ERROR_CODE,
        APP: APP,
        METHOD: METHOD,
        SERVICE: SERVICE,
        VALUE: VALUE,
        KEY: KEY,
        POPUP: POPUP,
        CHAR: CHAR
    }
});