'use strict';

acba.define('js:KidsUI',
    ['log:kids(KidsUI)', 'js:Kids', 'js:Enum', 'js:NavCoreProxy',
    'js:HDSManager', 'js:VBOWrapper', 'js:AppProxy', 'js:KidsProxy', 'js:AMOCProxy',
    'js:Util', 'js:Popup'],
    function (log, Kids, Enum, NavCoreProxy,
              HDSManager, VBOWrapper, AppProxy, KidsProxy, AMOCProxy,
              Util, Popup) {
console.error('KidsUI begin!');
    log.info('version: ' + KIDS.VERSION);

    KIDS.register(log);
    Kids.initLog(KIDS.LOG_LEVEL);

    var APP_NAME = Enum.APP.NAME,
        METHOD = Enum.METHOD,
        VALUE = Enum.VALUE,
        CHAR = Enum.CHAR,
        ERROR_CODE = Enum.ERROR_CODE,
        audioTTS, handleTTS,
        executeAPI = {
            showPopup: showPopup,
            showVoicePopup: showVoicePopup,
            talkToApp: AppProxy.talkToApp
        },
        callStackPostMsg = {};

    function init() {
        log.debug('init');

        NavCoreProxy.getCurrentChannel(); // for init createChannelConfig().channelList
        KidsProxy.setPopupListener({
            postMessage: postMessage,
            sendKeyword: sendKeyword,
            buyContents: AMOCProxy.buyContents,
            execute: execute
        });

        window.addEventListener('message', _receiveExecuteMessage);
    }

    Popup.setKidsMethod({
        goShortcut : _goShortcut
    });

    function _goShortcut(param) {
        var SHORTCUT = VALUE.SHORTCUT,
            url = param.url;

        switch (param.type) {
            case SHORTCUT.BOUND:
                log.debug('goShortcut(BOUND), url=' + url);
                VBOWrapper.setChannelByServiceID(url);
                break;
            case SHORTCUT.UNBOUND:
                log.debug('goShortcut(UNBOUND), url=' + url);
                AppProxy.startUnboundApp(url);
                break;
            case SHORTCUT.CHILD_APP:
                log.debug('goShortcut(CHILD_APP), url=' + url);
                _shortcutByChildApp(param['appID'], url);
                break;
        }

        Popup.hidePopup();
    }

    function _shortcutByChildApp(appID, message) {
        log.debug('shortcut(CHILD_APP), appID=' + appID + ', message=' + message);
        _executePostMessage({
            'appID': (appID ? AppProxy.getAppIDWithOID(appID) : AppProxy.getMashupManagerID()), // KTKIDSCARE-53
            'msg': message,
        });
    }

    /**
     * call method of args : obs_startUnboundApplication, hp_watchContent,
     *                      hp_watchContentForced as_runSASApp, showCategory
     *
     * @param {Object}
     *            args
     * @param {Object}
     *            appID
     */
    function postMessage(args, appID) {
        var method = args.method;

        if (!appID && Util.isHomePortal(method)) {
            appID = AppProxy.getHomePortalID();
        }

        if (Util.isWatchContentAPI(method)) {
            log.debug('postMessage, ' + method + ', msgObject=' + args);
            AppProxy.watchContent(args);
        } else if (appID) {
            _talkToOtherAppByID(appID, args);
        } else {
            log.debug('postMessage! appID error, appID=' + appID);
        }
    }

    /**
    * @param {String} keywod
    */
    function sendKeyword(keyword) {
        log.debug('sendKeyword, keyword=' + keyword);

        if (_isDataChannel() || AppProxy.isBoundApp()) {
            _notifyInvalidKeyword();
        } else {
            AppProxy.searchVOD(keyword);
        }
    }

    /**
     * @param params
     */
    function execute(params) {
        if (!params) {
            log.debug('execute, invalid params=' + params);
            return ;
        }

        var LINK = VALUE.LINK,
            linkType = params['link'],
            method;

        log.debug('execute, linkType=' + linkType);

        if (LINK.HDS === linkType) { // joinKidscare, cancelKidscare, checkBuyPIN, changeBuyPIN
            HDSManager.execute(params, KidsProxy.notifyExecuteResult);
        } else if (LINK.AMOC === linkType) { // remotePurchase
            AMOCProxy.buyContents(params);
        } else if (LINK.POST_MESSAGE === linkType) { // post Message
            log.debug('execute(postMessage)');
            _executePostMessage(params);
        } else { // showPopup, showVoicePopup
            method = params['method'];
            log.debug('execute, method=' + method);

            try {
                executeAPI[method](params);
            } catch (ex) {
                log.error("execute, Exception : " + ex);
            }
        }
    }

    function showPopup(params) {
        var popupType = params['popupType'],
            type;
        log.debug('showPopup, params=' + params + ', popupType=' + popupType);

        try {
            type = parseInt(popupType);
            Popup.showPopup(type, params);
        } catch (ex) {
            log.error('showPopup, exception=' + ex);
        }
    }

    function showVoicePopup(params) {
        if (!audioTTS) {
            try {
                audioTTS = oipfObjectFactory.createAudioTtsObject();
            } catch (ex) {
                log.error('showVoicePopup, createAudioTtsObject failed!, ex=' + ex);
                _notifyResultByTTS(ERROR_CODE.ETC_BY_STB_499);
                return ;
            }
        }

        log.debug('showVoicePopup');
        var methodName;

        try {
            audioTTS.addEventListener('SpeechStateChange', speechStateChange);

            handleTTS = audioTTS.startSpeech(params['message'], audioTTS.VOICE_FEMALE);
            log.debug('showVoicePopup, handleTTS=' + handleTTS);

            if (!handleTTS) {
                _removeSpeechAndNotifyErrorResult();
                return ;
            }

            methodName = params['args'];
            log.debug('showVoicePopup, methodName=' + methodName);

            if (!methodName) {
                _notifyResultByTTS(ERROR_CODE.SUCCESS_000);
                return;
            }

            try {
                executeAPI[methodName](params);
                _notifyResultByTTS(ERROR_CODE.SUCCESS_000);
            } catch (ex) {
                log.error('showVoicePopup, ex=' + ex);
                _notifyResultByTTS(ERROR_CODE.TTS_ERROR_POPUP_431);
            }
        } catch (ex) {
            log.error('showVoicePopup, ex=' + ex);
            _removeSpeechAndNotifyErrorResult();
        }
    }

    var speechStateChange = function(event) {
        var id = event.id,
            state = event.state,
            TTS_STATE = VALUE.TTS_STATE;

        if (!state) {
            log.error('speechStateChanage(TTS), state invalid!');
            return;
        }

        log.debug('speechStateChange(TTS), id=' + id + ', handleTTS=' + handleTTS
            + ', state=' + TTS_STATE.Names[state]);

        if (TTS_STATE.END === state || TTS_STATE.ERROR === state) {
            _stopTTS(id);
        }
    };

    function _notifyResultByTTS(code) {
        log.debug('notifyResult(TTS), code=' + code);
        KidsProxy.notifyExecuteResult({
            'method' : 'showVoicePopup',
            'resultCode' : code
        });
    }

    function _removeSpeechAndNotifyErrorResult() {
        _removeSpeechStateChangeEvent();
        _notifyResultByTTS(ERROR_CODE.TTS_ERROR_430);
    }

    function _removeSpeechStateChangeEvent() {
        log.debug('removeSpeechStateChangeEvent');
        if (audioTTS) {
            audioTTS.removeEventListener('SpeechStateChange', speechStateChange);
        }
    }

    function _stopTTS(id) {
        log.debug('stopTTS, handleID=' + id + ', handleTTS=' + handleTTS);
        if (!id) {
            return;
        }

        try {
            var result = audioTTS.stopSpeech(id);
            log.debug('stopTTS, handleID=' + id + ', result=' + result);
            _removeSpeechStateChangeEvent();
            handleTTS = undefined;
        } catch (ex) {
            log.error('stopTTS, handleID=' + id + ', ex=' + ex);
        }
    }

    function _receiveExecuteMessage(event) {
        log.debug('receiveExecuteMessage, event=' + event);
        var data = event['data'],
            method,
            firstCaller;

        if (!data) {
            log.error('receiveExecuteMessage, invalid data=' + data);
            return;
        }

        method = data['method'];
        log.debug('receiveExecuteMessage, method=' + method);

        firstCaller = _getFirstCaller(method);

        if (!firstCaller) {
            log.error('receiveExecuteMessage, invalid firstCaller, method=' + method);
            return;
        }

        log.debug('receiveExecuteMessage, firstCaller=' + firstCaller);

        if (firstCaller['callback']) {
            firstCaller['callback'](event, firstCaller['params']);
        } else {
            log.error('receiveExecuteMessage, invalid firstCaller callback');
            _shiftFirstCaller(method);
        }
    }

    function _getFirstCaller(method) {
        log.debug('getFirstCaller, method=' + method);
        var aCallStack = _getCallStackPostMessage(method),
            firstCaller;

        if (!aCallStack) {
            log.error('getFirstCaller, invalid aCallStack, method=' + method);
            return null;
        }

        firstCaller = aCallStack[0];

        if (!firstCaller) {
            log.error('getFirstCaller, invalid fristCaller, method=' + method);
            return null;
        }

        return firstCaller;
    }

    function _shiftFirstCaller(method) {
        log.debug('shiftFirstCaller, method=' + method);
        var aCallStack = _getCallStackPostMessage(method),
            firstCaller;

        if (!aCallStack) {
            log.error('shiftFirstCaller, invalid aCallStack, method=' + method);
            return null;
        }

        firstCaller = aCallStack.shift();

        if (!firstCaller) {
            log.error('shiftFirstCaller, invalid fristCaller, method=' + method);
            return null;
        }

        return firstCaller;
    }

    function _getCallStackPostMessage(method) {
        return callStackPostMsg[method];
    }

    function _getCallStackPostMessageWhenNoExistNewObject(method) {
        var callStack = _getCallStackPostMessage(method);

        if (callStack === undefined) {
            callStack = [];
            callStackPostMsg[method] = callStack;
        }

        return callStack;
    }

    function _notifyInvalidKeyword() {
        KidsProxy.notifyKeyword(false);
    }

    function _executePostMessage(params) {
        var appID = params['appID'],
            method = params['method'],
            app = null;

        if (appID === undefined && Util.isHomePortal(method)) {
            appID = AppProxy.getHomePortalID();
        }

        if (!appID) {
            log.error('executePostMessage, invalid appID');
            return;
        }

        log.debug('executePostMessage, appID=' + appID);
        app = AppProxy.getApplicationByAppID(appID);

        if (!app) {
            log.error('executePostMessage, don\'t find Applications(\'dvb.appId\'=\'' + appID + '\')');
            return;
        }

        log.debug('executePostMessage, appID=' + appID + ', method=' + method);
        _validExecutePostMessage(params, app.window);
    }

    function _validExecutePostMessage(params, appWindow) {
        var method = params['method'],
            aCaller = {
                method: undefined,
                params: undefined,
                depends: undefined,
                callback: undefined
            },
            postMsgObject;

        if (method) {
            aCaller['method'] = method;
            aCaller['params'] = params;

            if (Util.isHomePortal(method) && method.indexOf('AutoPower') > 0) {
                log.debug('executePostMessage, setAutoPower or getAutoPower API');
                postMsgObject = { 'method': params['depends'] };
                aCaller['depends'] = params['depends'];
                aCaller['callback'] = _callbackAutoPower;
            } else {
                log.debug('executePostMessage, Other method API, message=' + params['msg']);
                postMsgObject = Util.getJSONObject(params['msg']);
                aCaller['callback'] = _callbackFromApp;
            }
        } else {
            log.debug('executePostMessage, undefined method API, message=' + params['msg']);
            postMsgObject = Util.getJSONObject(params['msg']);
        }

        if (postMsgObject) {
            _callPostMessage(appWindow, postMsgObject, aCaller);
        } else {
            KidsProxy.notifyExecuteResult({
                'method': method,
                'resultCode': ERROR_CODE.MESSAGE_FORMAT_101
            });
        }
    }

    function _callPostMessage(appWindow, postMsgObj, aCaller) {
        log.debug('callPostMessage, appWindow=' + appWindow);

        if (!appWindow) {
            log.error('callPostMessage, invalid appWindow');
            return;
        }

        if (!postMsgObj) {
            log.error('callPostMessage, invalid postMsgObj=' + postMsgObj);
            return;
        }

        var method = postMsgObj['method'],
            aCallStack = _getCallStackPostMessageWhenNoExistNewObject(method);

        try {
            appWindow.postMessage(postMsgObj, '*');
            aCallStack.push(aCaller);
        } catch (e) {
            log.error('callPostMessage, exception=' + e);
        }
    }

    function _callbackAutoPower(event, callObject) {
        log.debug('callback [AutoPower], event=' + event + ', callObject=' + callObject);
        var data = event['data'],
            receiveMethod,
            method,
            firstCaller;

        if (!Util.isObject(data)) {
            log.error('callback [AutoPower], invalid data=' + data);
            return;
        }

        receiveMethod = data['method'];

        log.debug('callback [AutoPower], receive method=' + receiveMethod);
        firstCaller = _shiftFirstCaller(receiveMethod);
        method = firstCaller['method'];

        if (METHOD.HP_CHECK_VERSION === receiveMethod) {
            _callbackByCheckVersion(method, data, firstCaller, event);
        } else if (METHOD.HP_GET_AUTO_POWER === receiveMethod) {
            _callbackByGetAutoPower(method, data['result']);
        } else if (METHOD.HP_SET_AUTO_POWER === receiveMethod) {
            _callbackBySetAutoPower(method, data['result']);
        } else {
            log.debug('callback [AutoPower] invalid method(' + receiveMethod + ')');
        }
    }

    function _callbackByCheckVersion(method, data, firstCaller, event) {
        var params,
            postObjMsg = {};

        if (data['api'] && data['api'].indexOf(method) !== -1) {
            postObjMsg['method'] = method;

            if (METHOD.HP_SET_AUTO_POWER === method) {
                params = firstCaller['params'];

                postObjMsg['on_time'] = params['on_time'];
                postObjMsg['off_time'] = params['off_time'];
                postObjMsg['repeat'] = params['repeat'];
            }

            _callPostMessage(event.source, postObjMsg, firstCaller);
        } else {
            log.error('callback [AutoPower] api=' + data['api']);
            _notifyAutoPower(method, ERROR_CODE.AUTO_POWER_NO_API_540);
        }
    }

    function _callbackByGetAutoPower(method, result) {
        log.debug('callback [AutoPower], result=' + result);
        var resultArray = null;

        if (result && result !== '') {
            resultArray = result.split(CHAR.SEMICOLON);

            if (resultArray && resultArray.length === 3) { // ex) false;0900;2230 [반복여부;켜짐시간;꺼짐시간]
                _notifyAutoPower(method, ERROR_CODE.SUCCESS_000,
                    resultArray[1] + CHAR.CARET + resultArray[2]
                    + CHAR.CARET + (resultArray[0].toLowerCase() === 'true' ? '1' : '0'));
            } else {
                _notifyAutoPower(method, ERROR_CODE.AUTO_POWER_NO_API_540);
            }
        } else { // 설정정보 없음
            _notifyAutoPower(method, ERROR_CODE.AUTO_POWER_NO_SET_542);
        }
    }

    function _callbackBySetAutoPower(method, result) {
        log.debug('callback [AutoPower], result=' + result);
        var code;

        if (result == '1') { // 성공
            code = ERROR_CODE.SUCCESS_000;
        } else { // 실패(저장 실패)
            code = ERROR_CODE.AUTO_POWER_OTHER_ERROR_541;
        }

        _notifyAutoPower(method, code);
    }

    function _notifyAutoPower(method, resultCode, responseMsg) {
        log.debug('notifyAutoPower, method=' + method + ', resultCode=' + resultCode + ', responseMessage=' + responseMsg);
        var resultObj = {
            'method' : method,
            'resultCode' : resultCode
        };

        if (responseMsg) {
            resultObj['responseMessage'] = responseMsg;
        }

        KidsProxy.notifyExecuteResult(resultObj);
    }

    function _callbackFromApp(event) {
        log.debug('callback [FromApp], event=' + event);
        var data = event['data'],
            method,
            firstCaller,
            response = null;

        if (!Util.isObject(data)) {
            log.error('callback [FromApp], invalid data=' + data);
            return;
        }

        method = data['method'];
        firstCaller = _shiftFirstCaller(method);

        if (!firstCaller) {
            log.error('callbck [FromApp] invalid fristCaller, method=' + method);
            return;
        }

        try {
            response = JSON.stringify(data);
            log.debug('callback [FromApp], JSON.stringify=' + response);
        } catch (ex) {
            log.error('callback [FromApp], data=' + data + ', ex=' + ex);
            response = data;
        }

        KidsProxy.notifyExecuteResult({
            'method' : firstCaller['method'],
            'resultCode' : ERROR_CODE.SUCCESS_000,
            'responseMessage' : response
        });
    }

    function _isDataChannel() { // Is independent channel?
        var realCurrentChannel = NavCoreProxy.getRealCurrentChannel(),
            isDataChannelType = realCurrentChannel && (realCurrentChannel.channelType === Channel.TYPE_OTHER); // Channel.TYPE_OTHER=2
        log.debug('isDataChannel=' + isDataChannelType);
        return isDataChannelType;
    }

    function _talkToOtherAppByID(appID, msgObject) {
        var app = AppProxy.getApplicationByAppID(appID),
            method = msgObject['method'],
            from;

        log.debug('talkToOtherAppByID, appID=' + appID + ', method=' + method);

        if (!app) {
            log.debug('talkToOtherAppByID, don\'t find Applications(\'dvb.appId\', ' + appID + ', method=' + method);
            return;
        }

        log.debug('talkToOtherAppByID, find Applications(\'dvb.appId\', ' + appID + ')');
        from = msgObject['from'];

        if (from === undefined || from === '') {
            msgObject['from'] = APP_NAME.kidscare;
        }

        log.debug('talkToOtherAppByID, postMessage, appID=' + appID + ', method=' + method + ', from='
            + msgObject['from']);
        app.window.postMessage(msgObject, '*');
    }

    return {
        init: init
    };
});