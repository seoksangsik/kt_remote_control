'use strict';

acba.define('js:kids_ui',
['log:kids(kids_ui)', 'rop:remotecontrol.RemoteControlInterface', 'js:Enum', 'js:NavCoreProxy',
    'js:network', 'js:Config', 'js:Popup', 'js:HDSManager'],
function (log, rcInterface, Enum, navCoreProxy, network, config, popup, hdsManager) {
    log.level = log.DEBUG;
    log.info('version: v3.21_20210312');

    var _KIDS_UI = {},
        APP = Enum.APP,
        METHOD = Enum.METHOD,
        VALUE = Enum.VALUE,
        CHAR = Enum.CHAR,
        ERROR_CODE = Enum.ERROR_CODE,
        am = window.oipfObjectFactory.createApplicationManagerObject(),
        channelSelectionEventManager = null,
        audioTTS,
        handleTTS,
        ajax = network.Ajax.getInstance(),
        executeAPI = {
            'showPopup': showPopup,
            'showVoicePopup': showVoicePopup,
        },
        callStackPostMsg = {};

    popup.setKidsMethod({
        goShortcut : goShortcut,
        notifyExecuteResult : notifyExecuteResult
    });

    function stopManagedAppWhenRunning(appType) {
        if (APP.TYPE.FullBrowser == appType
            || APP.TYPE.UnicastApp == appType
            || APP.TYPE.UnboundApp == appType) {
            stopManagedApp();
        }
    }

    function stopManagedApp() {
        var promoCCID = navCoreProxy.getPromoCCID();
        log.debug('stopManagedApp! promoCCID=' + promoCCID);

        talkToOtherApp('Observer', {
            'method' : METHOD.OBS_STOP_MANAGED_APP,
            'args' : promoCCID
        });
    }

    var channelSelectionEventCallback = function (event) {
        log.debug('channelSelectionEvent, event=' + event);
        channelSelectionEventManager.removeChannelSelectionEventListener(channelSelectionEventCallback);
        log.debug('channelSelectionEventCallback! removeChannelSelectionEventListener');
        talkToWatchContentAfterCheck();
    };

    function talkToWatchContentAfterCheck() {
        var receiveMethod = METHOD.OBS_IS_LOADING_UNBOUND_APP;
        var firstCaller = shiftFirstCaller(receiveMethod);

        if (!firstCaller) {
            log.error('talkToWatchContentAfterCheck, invalid Caller, method=' + receiveMethod);
            return ;
        }

        var method = firstCaller['method'];
        log.debug('talkToWatchContentAfterCheck, method=' + method);

        if (isWatchContentAPI(method)) {
            talkToOtherApp('HomePortal', firstCaller['params']);
        }
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
    var postMessage = function(args, appID) {
        var method = args.method;
        if (!appID && isHomePortal(method)) {
            appID = APP.ID['HomePortal'];
        }

        if (isWatchContentAPI(method)) {
            log.debug('postMessage, ' + method + ', msgObject=' + args);
            executeWatchContent(method, args);
        } else if (appID) {
            log.debug('postMessage! appID=' + appID);
            talkToOtherAppByID(appID, args);
        } else {
            log.debug('postMessage! appID error, appID=' + appID);
        }
    };

    function executeWatchContent(method, params) {
        var aCaller = new Caller();

        aCaller['method'] = method;
        aCaller['params'] = params;
        aCaller['callback'] = callbackWatchContent;

        pushCallStackAndisLoadingUnboundApp(aCaller);
   }

   function callbackWatchContent(event, msgObject) {
       var data = event['data'];
       var method = data['method'];

       log.debug('callback [WatchContent], method=' + method);

       if (method !== METHOD.OBS_IS_LOADING_UNBOUND_APP) {
           return;
       }

       var result = data['res'];
       var appType = data['args'];

       stopManagedAppWhenRunning(appType);

       if (APP.TYPE.FullBrowser == appType || APP.TYPE.UnicastApp == appType) {
           addChannelSelectionEventListener();
           log.debug('callback [WatchContent], method=' + method + ', addChannelSelectionEventListener');
       } else {
           log.debug('callback [WatchContent], msgObject of method=' + msgObject.method);
           talkToWatchContentAfterCheck();
       }

       log.debug('callback [WatchContent], obs_isLoadingUnboundApplication! appType='
               + appType + ', result=' + result + ', msgObject=' + msgObject);
   }

   function executeSearchVOD(keyword) {
        var aCaller = new Caller();

        aCaller['method'] = METHOD.HP_SEARCH_VOD;
        aCaller['params'] = keyword;
        aCaller['callback'] = callbackSearchVOD;

        pushCallStackAndisLoadingUnboundApp(aCaller);
    }

    function callbackSearchVOD(event, keyword) {
        var data = event['data'];
        var method = data['method'];
        var result;

        log.debug('callback [SearchVOD], method=' + method);

        if (method !== METHOD.OBS_IS_LOADING_UNBOUND_APP && method !== METHOD.HP_SEARCH_VOD) {
            return ;
        }

        if (method === METHOD.OBS_IS_LOADING_UNBOUND_APP) {
            result = data['res'];

            var appType = data['args'];
            var startHomePortal = APP.TYPE.UnboundApp == appType
                                 && data['target'] == APP.ID['HomePortal'];

            log.debug('callback [SearchVOD], ' + method + '! appType='
                + appType + '(' + APP.TYPE.Names[appType] + '), startHomePortal=' + startHomePortal
                + ', target=' + data['target']);

            var caller = shiftFirstCaller(method);

            if (startHomePortal) { // KTWEBMW-2996, KTUHDII-375
                pushCallStack(caller['method'], caller);

                talkToOtherApp('HomePortal', {
                    'method': METHOD.HP_SEARCH_VOD,
                    'req_cd': '00',
                    'srchword': keyword, // search word
                    'srchopt': '', // option
                    'append': "false" // true : append, false : overwrite
                });
            } else {
                notifyKeyword(false);
            }
        }

        if (method === METHOD.HP_SEARCH_VOD) {
            result = data['result'];
            log.debug('callback [SearchVOD], method=' + method + ', result=' + result);
            shiftFirstCaller(method);
            notifyKeyword(result);
        }
    }

    function pushCallStackAndisLoadingUnboundApp(caller) {
        pushCallStack(METHOD.OBS_IS_LOADING_UNBOUND_APP, caller);
        isLoadingUnboundApp();
    }

    function pushCallStack(method, caller) {
        var aCallStack = callStackPostMsg[method];

        if (aCallStack === undefined) {
            aCallStack = [];
            callStackPostMsg[method] = aCallStack;
        }

        aCallStack.push(caller);
    }

    function isHomePortal(method) {
        return method ? method.indexOf('hp_') === 0 : false;
    }

    function isObserver(method) {
        return method ? method.indexOf('obs_') === 0 : false;
    }

    function isWatchContentAPI(method) {
        return method ? method.indexOf(METHOD.HP_WATCH_CONTENT) === 0 : false;
    }

    function notifyKeyword(result) {
        log.debug('notifyKeyword, result=' + result);
        rcInterface.notifyKeyword(result);
    }

    var sendKeyword = function (keyword) {
        log.debug('sendKeyword');
        if (isDataChannel() || isBoundApp()) {
            notifyKeyword(false);
        } else { // Is Unbound application?
            log.debug('sendKeyword, keyword=' + keyword + ', check Unbound app to Observer!');
            executeSearchVOD(keyword);
        }
    };

    function isLoadingUnboundApp() {
        talkToOtherApp('Observer', {
            'method' : METHOD.OBS_IS_LOADING_UNBOUND_APP
        });
    }

    function isDataChannel() { // Is independent channel?
        var realCurrentChannel = navCoreProxy.getRealCurrentChannel(),
            isDataChannelType = realCurrentChannel && (realCurrentChannel.channelType === Channel.TYPE_OTHER); // Channel.TYPE_OTHER=2
        log.debug('isDataChannel=' + isDataChannelType);
        return isDataChannelType;
    }

    function isBoundApp() {
        var boundApps = am.findApplications('useType', 'bound'),
            boundApp = boundApps && boundApps.length > 0;
        log.debug('boundApp=' + boundApp + (boundApp ? ', app\'s count : ' + boundApps.length : ''));
        return boundApp;
    }

    function talkToOtherAppByID(appID, msgObject) {
        var app = am.findApplications('dvb.appId', appID)[0],
            method = msgObject['method'],
            from;
        log.debug('talkToOtherAppByID, appID=' + appID + ', method=' + method
                + ', am=' + am);

        if (app) {
            log.debug('talkToOtherAppByID, find Applications(\'dvb.appId\', '
                    + appID + ')');
            from = msgObject['from'];

            if (from === undefined || from === '') {
                msgObject['from'] = 'kidscare';
            }

            log.debug('talkToOtherAppByID, postMessage, appID=' + appID
                    + ', method=' + method + ', from=' + msgObject['from']);
            app.window.postMessage(msgObject, '*');
        } else {
            log.debug('talkToOtherAppByID, don\'t find Applications(\'dvb.appId\', '
                    + appID + ', method=' + method);
        }
    }

    function talkToOtherApp(appName, msgObject) {
        var appID = APP.ID[appName];
        log.debug('talkToOtherApp, appName=' + appName + '(' + appID
                + '), method=' + msgObject['method']);
        talkToOtherAppByID(appID, msgObject);
    }

    function addChannelSelectionEventListener() {
        channelSelectionEventManager.addChannelSelectionEventListener(channelSelectionEventCallback);
    }

    var init = function () {
        log.debug('init');

        channelSelectionEventManager = oipfObjectFactory.createChannelSelectionEventManager();

        navCoreProxy.getCurrentChannel(); // for init createChannelConfig().channelList
        rcInterface.setRemoteControlEvent(_KIDS_UI);

        window.addEventListener('message', receiveExecuteMessage);
    };

    function buyContents(params) {
        if (!params) {
            notifyRemotePurchase(ERROR_CODE.ETC_BY_STB_499);
            return ;
        }

        log.debug('buyContents, saId=' + params['saId'] + ', contentId='
                + params['contsId'] + ', contentName=' + params['contsName']
                + ', buyingDate=' + params['buyingDate'] + ', buyingPrice='
                + params['buyingPrice'] + ', categoryId=' + params['catId']);

        var callParam = 'saId=' + params['saId']             // 가입자SAID
                    + '&pkgYn=N'                             // 묶음 구매 여부(N:건별구매)
                    + '&contsId=' + params['contsId']        // 컨텐츠ID 또는 카테고리ID
                    + '&contsName=' + params['contsName']    // 컨텐츠명 또는 카테고리명
                    + '&buyingDate=' + params['buyingDate']  // 구매일시
                    + '&buyingPrice=' + params['buyingPrice'] // 구매금액
                    + '&buyingType=B'                         // 구매 수단(고정값)
                    + '&catId=' + params['catId']             // 구매하려는 컨텐츠가 편성된 카테고리ID
                    + '&appCd=H'                              // 어플리케이션 코드(H:HomePortal, KC:Kidscare)
                    + '&reqPathCd=01'                         // 진입경로(01:홈메뉴)
                    + '&ltFlag=0'                             // 장기여부(0:단기구매, 1:장기구매)
                    + '&hdYn='                                // HD/SD 시리즈 여부(통합편성된 시리즈카테고리를 구매할 경우만 입력)
                    + '&saleYn=N'                             // 재구매할인 적용 여부
                    + '&WMOCKey=OTVHome';

        log.debug('buyContents, callParam=' + callParam);
        ajax.setCallbackType(ajax.CALLBACK_TYPE_TEXT);
        ajax.setTimeout(VALUE.SERVER_TIMEOUT, responseBuyContents);
        ajax.setCallbackListener(responseBuyContents, responseBuyContents);
        ajax.sendPostMethod(config.getAMOCURL() + '/amoc-api/vod/buy/in-cash', callParam);
    }

    function responseBuyContents(isSuccess, responseData) {
        var flag = VALUE.PURCHASE.FAIL,
            message = '',
            result = '',
            expireDate = '',
            jsonData;

        log.debug('responseBuyContents isSuccess=' + isSuccess
                + ', responseData=' + responseData);
        if (isSuccess && responseData) {
            jsonData = JSON.parse(responseData);
            if (jsonData) {
                flag = parseInt(jsonData['flag']);
                message = jsonData['message'];
                expireDate = jsonData['expireDate'];
            }
        }

        log.debug('responseBuyContents flag=' + flag + ', message=' + message
                + ', expireDate=' + expireDate);

        switch (flag) {
        case VALUE.PURCHASE.SUCCESS:
            result = ERROR_CODE.SUCCESS_000;
            break;
        case VALUE.PURCHASE.FAIL:
            result = ERROR_CODE.PURCHASE_FAIL_408;
            break;
        case VALUE.PURCHASE.ALREADY:
            result = ERROR_CODE.PURCHASE_ALREADY_405;
            break;
        }

        if (message) {
            result += (CHAR.CARET + message);
        }

        notifyRemotePurchase(result);
    }

    function notifyRemotePurchase(result) {
        log.debug('notifyRemotePurchase, result=' + result);
        rcInterface.notifyRemotePurchase(result);
    }

    function showPopup(params) {
        var popupType = params['popupType'];
        log.debug('showPopup, params=' + params + ', popupType=' + popupType);
        try {
            popup.showPopup(parseInt(popupType), params);
        } catch (exception) {
            log.error('showPopup, ex=' + exception);
        }
    }

    function showVoicePopup(params) {
        if (!audioTTS) {
            try {
                audioTTS = oipfObjectFactory.createAudioTtsObject();
            } catch (ex) {
                log.error('showVoicePopup, createAudioTtsObject failed!, ex=' + ex);
                notifyResultByTTS(ERROR_CODE.ETC_BY_STB_499);
                return ;
            }
        }

        log.debug('showVoicePopup');

        try {
            audioTTS.addEventListener('SpeechStateChange', speechStateChange);

            handleTTS = audioTTS.startSpeech(params['message'], audioTTS.VOICE_FEMALE);
            log.debug('showVoicePopup, handleTTS=' + handleTTS);

            if (handleTTS) {
                var methodName = params['args'];

                log.debug('showVoicePopup, methodName=' + methodName);
                if (methodName) {
                    try {
                        executeAPI[methodName](params);
                        notifyResultByTTS(ERROR_CODE.SUCCESS_000);
                    } catch (ex) {
                        log.error('showVoicePopup, ex=' + ex);
                        notifyResultByTTS(ERROR_CODE.TTS_ERROR_POPUP_431);
                    }
                } else {
                    notifyResultByTTS(ERROR_CODE.SUCCESS_000);
                }
            } else {
                removeSpeechAndNotifyErrorResult();
            }
        } catch (ex) {
            log.error('showVoicePopup, ex=' + ex);
            removeSpeechAndNotifyErrorResult();
        }
    }

    function notifyResultByTTS(code) {
        log.debug('notifyResult(TTS), code=' + code);
        notifyExecuteResult({
            'method' : 'showVoicePopup',
            'resultCode' : code
        });
    }

    function removeSpeechAndNotifyErrorResult() {
        removeSpeechStateChangeEvent();
        notifyResultByTTS(ERROR_CODE.TTS_ERROR_430);
    }

    function removeSpeechStateChangeEvent() {
        log.debug('removeSpeechStateChangeEvent');
        if (audioTTS) {
            audioTTS.removeEventListener('SpeechStateChange', speechStateChange);
        }
    }

    function stopTTS(id) {
        log.debug('stopTTS, handleID=' + id + ', handleTTS=' + handleTTS);
        if (id) {
            try {
                var result = audioTTS.stopSpeech(id);
                log.debug('stopTTS, handleID=' + id + ', result=' + result);
                removeSpeechStateChangeEvent();
                handleTTS = undefined;
            } catch (ex) {
                log.error('stopTTS, handleID=' + id + ', ex=' + ex);
            }
        }
    }

    var speechStateChange = function(event) {
        var id = event.id,
            state = event.state;

        if (state) {
            log.debug('speechStateChange(TTS), id=' + id + ', handleTTS='
                    + handleTTS + ', state=' + VALUE.TTS_STATE.Names[state]);

            if (VALUE.TTS_STATE.END === state || VALUE.TTS_STATE.ERROR === state) {
                stopTTS(id);
            }
        } else {
            log.error('speechStateChanage(TTS), state invalid!');
        }
    }

    function execute(params) {
        if (!params) {
            log.debug('execute, invalid params=' + params);
            return ;
        }

        var linkType = params['link'],
            method;

        log.debug('execute, linkType=' + linkType);

        if (VALUE.LINK.HDS === linkType) { // joinKidscare, cancelKidscare, checkBuyPIN, changeBuyPIN
            hdsManager.execute(params, notifyExecuteResult);
        } else if (VALUE.LINK.AMOC === linkType) { // remotePurchase
            buyContents(params);
        } else if (VALUE.LINK.POST_MESSAGE === linkType) { // post Message
            log.debug('execute(postMessage)');
            executePostMessage(params);
        } else { // showPopup
            method = params['method'];
            log.debug('execute, method=' + method);
            try {
                executeAPI[method](params);
            } catch (exception) {
                log.error("execute, Exception : " + exception);
            }
        }
    }

    function executePostMessage(params) {
        var app = null,
            appID = params['appID'],
            method = params['method'],
            aCaller,
            postMsgObject,
            isHomeMethod = isHomePortal(method);

        if (appID === undefined && isHomeMethod) {
            appID = APP.ID['HomePortal'];
        }

        if (appID) {
            log.debug('executePostMessage, appID=' + appID + ', am=' + am);
            app = am.findApplications('dvb.appId', appID)[0];

            if (app) {
                log.debug('executePostMessage, appID=' + appID + ', method=' + method);
                aCaller = new Caller();

                if (method) {
                    aCaller['method'] = method;
                    aCaller['params'] = params;

                    if (isHomeMethod && method.indexOf('AutoPower') > 0) {
                        log.debug('executePostMessage, setAutoPower or getAutoPower API');
                        postMsgObject = {'method' : params['depends']};
                        aCaller['depends'] = params['depends'];
                        aCaller['callback'] = callbackAutoPower;
                    } else {
                        log.debug('executePostMessage, Other method API, message='
                                + params['msg']);
                        postMsgObject = getJSONObject(params['msg']);
                        aCaller['callback'] = callbackFromApp;
                    }
                } else {
                    log.debug('executePostMessage, undefined method API, message='
                            + params['msg']);
                    postMsgObject = getJSONObject(params['msg']);
                }

                if (postMsgObject) {
                    callPostMessage(app.window, postMsgObject, aCaller);
                } else {
                    notifyExecuteResult({
                        'method' : method,
                        'resultCode' : ERROR_CODE.MESSAGE_FORMAT_101
                    });
                }
            } else {
                log.error('executePostMessage, don\'t find Applications(\'dvb.appId\'=\''
                        + appID + '\')');
            }
        } else {
            log.error('executePostMessage, invalid appID');
        }
    }

    function getJSONObject(data) {
        log.debug('getJSONObject, data=' + data);
        var jsonObject = null;

        try {
            jsonObject = JSON.parse(data);
        } catch (exception) {
            log.error('getJSONObject, exception=' + exception);
        }
        return jsonObject;
    }

    function callPostMessage(appWindow, postMsgObj, aCaller) {
        log.debug('callPostMessage, appWindow=' + appWindow);
        var method,
            aCallStack;

        if (appWindow) {
            if (postMsgObj) {
                method = postMsgObj['method'];
                aCallStack = callStackPostMsg[method];

                if (aCallStack === undefined) {
                    aCallStack = [];
                    callStackPostMsg[method] = aCallStack;
                }

                try {
                    appWindow.postMessage(postMsgObj, '*');
                    aCallStack.push(aCaller);
                } catch (exception) {
                    log.error('callPostMessage, exception=' + exception);
                }
            } else {
                log.error('callPostMessage, invalid postMsgObj=' + postMsgObj);
            }
        } else {
            log.error('callPostMessage, invalid appWindow');
        }
    }

    function Caller() {
        this.method;
        this.depends;
        this.params;
        this.callback;
    }

    function notifyExecuteResult(result) {
        if (result) {
            log.debug('notifyExecuteResult, result=' + result);
            rcInterface.notifyExecuteResult(result);
        } else {
            log.debug('notifyExecuteResult, invalid result=' + result);
        }
    }

    function receiveExecuteMessage(event) {
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

        firstCaller = getFirstCaller(method);
        if (firstCaller) {
            log.debug('receiveExecuteMessage, firstCaller=' + firstCaller);

            if (firstCaller['callback']) {
                firstCaller['callback'](event, firstCaller['params']);
            } else {
                log.error('receiveExecuteMessage, invalid firstCaller callback');
                shiftFirstCaller(method);
            }
        } else {
            log.error('receiveExecuteMessage, invalid firstCaller, method=' + method);
        }
    }

    function callbackFromApp(event) {
        log.debug('callback [FromApp], event=' + event);
        var data = event['data'],
            method,
            firstCaller,
            response = null;

        if (data && typeof data === 'object') {
            method = data['method'];
            firstCaller = shiftFirstCaller(method);

            if (firstCaller) {
                try {
                    response = JSON.stringify(data);
                    log.debug('callback [FromApp], JSON.stringify=' + response);
                } catch (ex) {
                    log.error('callback [FromApp], data=' + data + ', ex=' + ex);
                    response = data;
                }

                notifyExecuteResult({
                    'method' : firstCaller['method'],
                    'resultCode' : ERROR_CODE.SUCCESS_000,
                    'responseMessage' : response
                });
            } else {
                log.error('callbck [FromApp] invalid fristCaller, method=' + method);
            }
        } else {
            log.error('callback [FromApp], invalid data=' + data);
        }
    }

    function callbackAutoPower(event, callObject) {
        log.debug('callback [AutoPower], event=' + event + ', callObject=' + callObject);
        var data = event['data'],
            receiveMethod,
            method,
            result = null,
            resultArray = null,
            firstCaller,
            params,
            postObjMsg = {};

        if (data && typeof data === 'object') {
            receiveMethod = data['method'];

            log.debug('callback [AutoPower], receive method=' + receiveMethod);
            firstCaller = shiftFirstCaller(receiveMethod);
            method = firstCaller['method'];

            if ('hp_checkVersion' === receiveMethod) {
                if (data['api'] && data['api'].indexOf(method) !== -1) {
                    postObjMsg['method'] = method;

                    if ('hp_setAutoPower' === method) {
                        params = firstCaller['params'];
                        postObjMsg['on_time'] = params['on_time'];
                        postObjMsg['off_time'] = params['off_time'];
                        postObjMsg['repeat'] = params['repeat'];
                    }

                    callPostMessage(event.source, postObjMsg, firstCaller);
                } else {
                    log.error('callback [AutoPower] api=' + data['api']);
                    notifyAutoPower(method, ERROR_CODE.AUTO_POWER_NO_API_540);
                }
            } else if ('hp_getAutoPower' === receiveMethod) {
                result = data['result'];
                log.debug('callback [AutoPower], result=' + result);
                if (result && result !== '') {
                    resultArray = result.split(CHAR.SEMICOLON);

                    if (resultArray && resultArray.length === 3) { // ex) false;0900;2230 [반복여부;켜짐시간;꺼짐시간]
                        notifyAutoPower(method, ERROR_CODE.SUCCESS_000,
                                    resultArray[1] + CHAR.CARET + resultArray[2]
                                    + CHAR.CARET + (resultArray[0].toLowerCase() === 'true' ? '1' : '0'));
                    } else {
                        notifyAutoPower(method, ERROR_CODE.AUTO_POWER_NO_API_540);
                    }
                } else { // 설정정보 없음
                    notifyAutoPower(method, ERROR_CODE.AUTO_POWER_NO_SET_542);
                }
            } else if ('hp_setAutoPower' === receiveMethod) {
                result = data['result'];
                log.debug('callback [AutoPower], result=' + result);
                if (result == '1') { // 성공
                    notifyAutoPower(method, ERROR_CODE.SUCCESS_000);
                } else { // 실패(저장 실패)
                    notifyAutoPower(method, ERROR_CODE.AUTO_POWER_OTHER_ERROR_541);
                }
            } else {
                log.debug('callback [AutoPower] invalid method(' + receiveMethod + ')');
            }
        } else {
            log.error('callback [AutoPower], invalid data=' + data);
        }
    }

    function notifyAutoPower(method, resultCode, responseMsg) {
        log.debug('notifyAutoPower, method=' + method + ', resultCode='
                + resultCode + ', responseMessage=' + responseMsg);
        var resultObj = {
            'method' : method,
            'resultCode' : resultCode
        };

        if (responseMsg) {
            resultObj['responseMessage'] = responseMsg;
        }

        notifyExecuteResult(resultObj);
    }

    function getFirstCaller(method) {
        log.debug('getFirstCaller, method=' + method);
        var aCallStack = callStackPostMsg[method],
            firstCaller;

        if (!aCallStack) {
            log.error('getFirstCaller, invalid aCallStack, method=' + method);
            return null;
        }

        firstCaller = aCallStack[0];

        if (firstCaller) {
            return firstCaller;
        }

        log.error('getFirstCaller, invalid fristCaller, method=' + method);

        return null;
    }

    function shiftFirstCaller(method) {
        log.debug('shiftFirstCaller, method=' + method);
        var aCallStack = callStackPostMsg[method],
            firstCaller;

        if (aCallStack) {
            firstCaller = aCallStack.shift();

            if (firstCaller) {
                return firstCaller;
            } else {
                log.error('shiftFirstCaller, invalid fristCaller, method=' + method);
            }
        } else {
            log.error('shiftFirstCaller, invalid aCallStack, method=' + method);
        }

        return null;
    }

    function goShortcut(param) {
        var url = param.url,
            app, appID;

        switch (param.type) {
        case VALUE.SHORTCUT.BOUND:
            log.debug('goShortcut(BOUND), url=' + url);
            VBOWrapper.setChannelByServiceID(url);
            break;
        case VALUE.SHORTCUT.UNBOUND:
            app = am.findApplications('dvb.appId', APP.ID['Observer'])[0];

            if (app) {
                log.debug('goShortcut(UNBOUND), find observer, url=' + url);

               app.window.postMessage({
                    'method' : 'obs_startUnboundApplication',
                    'from' : 'kidscare',
                    'target' : '4e30.' + url
               }, '*');
            } else {
                log.error('goShortcut(UNBOUND), don\'t find observer!');
            }
            break;
        case VALUE.SHORTCUT.CHILD_APP:
            appID = param['appID'];
            log.debug('goShortcut(CHILD_APP), appID=' + appID + ', url=' + url);
            executePostMessage({
                'appID' : (appID ? '4e30.' + appID : APP.ID['MashupManager']), // KTKIDSCARE-53
                'msg' : url,
            });
            break;
        }

        popup.hidePopup();
    }

    // VideoBroadcastObject wrapper
    var VBOWrapper = (function () {
        var vbo = window.oipfObjectFactory.createVideoBroadcastObject(),
            stbInfo = acba("js:nav/STBInfo"),
            cconfig = undefined,
            favList;

        vbo.setAttribute("class", "VideoBroadcast");
        document.body.appendChild(vbo);
        cconfig = vbo.getChannelConfig();

        function getChannelList() {
            var i = 0,
                channelList = [],
                skyKidsChannels, ktKidsChannels,
                kidsCareMode = (stbInfo['STBInfo'].getInfo('kidsCareMode') == 'true') ? true : false;

            if (kidsCareMode) {
                log.debug("getChannelList, KidsCare Channel List!");
                channelList = [];
                favList = cconfig.favouriteLists;
                skyKidsChannels = favList.getFavouriteList("favourite:SKYLIFE_CHANNELS_KIDS_CARE");
                ktKidsChannels = favList.getFavouriteList("favourite:KT_CHANNELS_KIDS_CARE");

                log.debug("getChannelList, KidsCare Count, Skylife=" + skyKidsChannels.length + ", KT=" + ktKidsChannels.length);
                for (i = 0; i < skyKidsChannels.length; i++) {
                    channelList.push(skyKidsChannels[i]);
                }
                for (i = 0; i < ktKidsChannels.length; i++) {
                    channelList.push(ktKidsChannels[i]);
                }
            } else {
                log.debug("getChannelList, All Channel List!");
                channelList = cconfig.channelList;
                log.debug("getChannelList, All Channel Count=" + channelList.length);
            }
            return channelList;
        }

        function searchChannel(value) {
            var list = getChannelList(),
                len = list.length,
                channel,
                idx = 0;

            for (idx = 0; idx < len; idx++) {
                channel = list[idx];
                if (channel.majorChannel == value || channel.name == value) {
                    return channel;
                }
            }
            return null;
        }

        return {
            setChannel: function (channel) {
                log.debug("setChannel, channel=" + channel);
                if (channel === undefined || channel === null) {
                    return;
                }

                vbo.setChannel(channel);
            },
            setChannelByServiceID: function (value) {
                log.debug('setChannelByServiceID, value=' + value);
                var ch = searchChannel(value);
                if (ch) {
                    vbo.setChannel(ch);
                } else {
                    log.error('setChannelByServiceID, don\'t find channel(' + value + ')');
                }
            },
            setAllChannel: function (ccid) {
                log.debug("setAllChannel, ccid: " + ccid);
                if (ccid === undefined || ccid === null) {
                    return;
                }
                vbo.setChannel(cconfig.channelList.getChannel(ccid));
            },
        };
    }());

    Object.defineProperties(_KIDS_UI, {
        'init' : {
            value : init,
            writable : false
        },
        'postMessage' : {
            value : postMessage,
            writable : false
        },
        'sendKeyword' : {
            value : sendKeyword,
            writable : false
        },
        'buyContents' : {
            value : buyContents,
            writable : false
        },
        'execute' : {
            value : execute,
            writable : false
        },
    });

    return {
        'KIDS_UI' : _KIDS_UI
    };
});