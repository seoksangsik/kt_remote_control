'use strict';

acba.define('js:AppProxy',
    ['log:kids(AppProxy)', 'js:Enum', 'js:KidsProxy', 'js:NavCoreProxy'],
    function (log, Enum, KidsProxy, NavCoreProxy) {

    KIDS.register(log);

    var am = window.oipfObjectFactory.createApplicationManagerObject(),
        channelSelectionEventManager = oipfObjectFactory.createChannelSelectionEventManager(),
        APP = Enum.APP,
        CHAR = Enum.CHAR,
        METHOD = Enum.METHOD,
        ERROR_CODE = Enum.ERROR_CODE,
        APP_ID = APP.ID,
        TYPE = APP.TYPE,
        STATUS = APP.STATUS,
        API = {
            searchResult: searchResult,
            launchSharpLink: launchSharpLink,
            forwarding: forwarding,
            getHomeState: getHomeState
        },
        watchContentMsgObject,
        homePortalMethods,
        channelSelectionEventCallback;

    function talkToApp(params) {
        log.debug('talkToApp');

        var target = params.target,
            myCallback = function (result) {
                log.debug('talkToApp(callback), result=' + result);
                _notifyResultByTalkToApp(target, result);
            };

        try {
            API[target](params, myCallback);
        } catch (ex) {
            log.error('talkToApp, api=' + api + ', ex=' + ex);
            _notifyResultByTalkToApp(target, ERROR_CODE.ETC_BY_STB_499);
        }
    }

    /**
     *
     * @param params(message)
     * message = {
            "reqmsg": "아이언맨3 찾아줘",
            "srchWord": "아이언맨3",
            "contType": "0xff",
            "srchOpt": "17:미국",
            "bsort": "true",
            "sortFields": "POINT"
        }
     * @param callback
     */
    function searchResult(params, callback) {
        var method = METHOD.HP_SEARCH_RESULT,
            data,
            dataObj,
            methodCallback = function (e) {
            try {
                var dataObjByEvent = e.data,
                    SUCCESS = '1',
                    result,
                    code;

                if (method !== dataObjByEvent.method) {
                    return ;
                }

                _removeMessageEventListener(methodCallback);

                result = dataObjByEvent.result;
                log.debug('searchResult(callback), result=' + result + '(0:FAIL, 1:SUCCESS)');
                code = SUCCESS === result ? ERROR_CODE.SUCCESS_000 : ERROR_CODE.ETC_BY_STB_499;
            } catch (ex) {
                log.error('searchResult(callback), ex=' + ex);
                code = ERROR_CODE.ETC_BY_STB_499;
            }

            _fireCallback(callback, code);
        };

        data = params.message;
        log.debug('searchResult, data=' + data);
        dataObj = JSON.parse(data);

        _postMessageToHomePortal({
            method: method,
            from: APP_ID.kidscare,
            req_cd: '',
            srchword: dataObj.srchWord,
            srchopt: dataObj.srchOpt,
            srchQry: '',
            wordType: '',
            contType: dataObj.contType,
            bsort: dataObj.bsort,
            sortFields: dataObj.sortFields
        }, methodCallback);
    }

    /**
     *
     * @param params(message)
     * @param callback
     */
    function launchSharpLink(params, callback) {
        var data = params.message;

        log.debug('launchSharpLink, data=' + data);

        _postMessageToQrator({
            method: METHOD.QRT_MOVE_SHARP_LINK,
            from: APP_ID.kidscare,
            req_cd: 'COM', // 공용제어
            sharpLink_id: data
        });
        _fireSuccessCallback(callback);
    }

    /**
     *
     * @param params(appID, message)
     * @param callback
     */
    function forwarding(params, callback) {
        var dvbID = getAppIDWithOID(params.appID),
            message = params.message;

        log.debug('forwarding, dvbID=' + dvbID + ', message=' + message);

        _talkToUnbound(dvbID, JSON.parse(message));
        _fireSuccessCallback(callback);
    }

    /**
     * tvstatus: 0(TV 시청상태) 1(VOD 재생상태) 3(TV 시청 중 다른 앱 실행상태) 4(VOD 재생 중 다른 앱 실행상태)
     * hpstatus: 홈포탈 화면 코드
     * vodstatus: tvstatus가 1, 4일 때만 전달, 0(VOD 재생), 1(VOD 일시정지)
     * @param params
     * @param callback
     */
    function getHomeState(params, callback) {
        var method = METHOD.HP_GET_HOME_STATE,
            status = [],
            callbackMethod = function (e) {
            try {
                var data = e.data,
                    methodName = data.method,
                    tvStatus,
                    hpStatus,
                    vodStatus,
                    result;

                if (method !== methodName) {
                    log.debug('getHomeState(callback), don\'t listen data.method(' + methodName + ')');
                    return ;
                }

                _removeMessageEventListener(callbackMethod);

                tvStatus = data.tvstatus;
                hpStatus = data.hpstatus;
                vodStatus = data.vodstatus;

                log.debug('getHomeState(callback), status[tv=' + tvStatus + ', hp=' + hpStatus + ', vod=' + vodStatus + ']');

                result = _getStatus(tvStatus, hpStatus, vodStatus, status);

                _fireCallback(callback, result);
            } catch (ex) {
                log.error('getHomeState(callback), ex=' + ex);
            }
        };

        isLoadingUnboundApp(function (appType, appTarget) {
            var result,
                startHomePortal = TYPE.UnboundApp === appType && getHomePortalID() === appTarget;

            if (startHomePortal) {
                status.push(STATUS.SHOW_HOME_PORTAL);
            }

            checkHomeAPI(method, function (existAPI) {

                if (!existAPI) {
                    status.unshift(STATUS.NO_GET_HOME_STATE_API);
                    result = status.join(CHAR.SEMICOLON);

                    _fireCallback(callback, result);
                    return ;
                }

                _postMessageToHomePortal({
                    method: method
                }, callbackMethod);
            });
        });
    }

    function checkHomeAPI(method, callback) {
        if (_hasHomeAPI()) {
            _callbackAtCheckHomeAPI(method, callback);
        } else {
            checkVersionByHomePortal(function () {
                _callbackAtCheckHomeAPI(method, callback);
            });
        }
    }

    function _callbackAtCheckHomeAPI(method, callback) {
        var exist = _existHomeAPI(method);

        if (callback) {
            callback(exist);
        }
    }

    function checkVersionByHomePortal(callback) {
        var method = METHOD.HP_CHECK_VERSION,
            callbackMethod = function (e) {
                try {
                    var data = e.data,
                        methodName = data.method;

                    if (method !== methodName) {
                        log.debug('checkVersion(callback), don\'t listen data.method(' + methodName + ')');
                        return ;
                    }

                    _removeMessageEventListener(callbackMethod);

                    log.debug('checkVersion(callback), result=' + data.api);
                    homePortalMethods = data.api;

                    if (callback) {
                        callback();
                    }
                } catch (ex) {
                    log.error('checkVersion(callback), ex=' + ex);
                }
            };

        _postMessageToHomePortal({
            'method': method
        }, callbackMethod);
    }

    function searchVOD(keyword) {
        var method = METHOD.HP_SEARCH_VOD,
            methodCallback = function (e) {
                try {
                    var data = e.data,
                        methodName = data.method,
                        result;

                    if (method !== methodName) {
                        log.debug('searchVOD(callback), don\'t listen data.method(' + methodName + ')');
                        return ;
                    }

                    _removeMessageEventListener(methodCallback);

                    result = data.result;
                    log.debug('searchVOD(callback), result=' + result + '(0:FAIL, 1:SUCCESS)');

                    KidsProxy.notifyKeyword(result);
                } catch (ex) {
                    log.error('searchVOD(callback), ex=' + ex);
                }
            };

        isLoadingUnboundApp(function (appType, appTarget) {
            var startHomePortal = TYPE.UnboundApp === appType
                                && getHomePortalID() === appTarget; // KTWEBMW-2996, KTUHDII-375

            log.debug('searchVOD(isLoadingUnboundApp), startHomePortal=' + startHomePortal);

            if (!startHomePortal) {
                KidsProxy.notifyKeyword(false);

                return ;
            }

            _postMessageToHomePortal({
                'method': method,
                'req_cd': '00',
                'srchword': keyword, // search word
                'srchopt': '', // option
                'append': "false" // true : append, false : overwrite
            }, methodCallback);
        });
    }

    function watchContent(msgObject) {
        log.debug('watchContent, msgObject=' + msgObject);

        watchContentMsgObject = msgObject;

        isLoadingUnboundApp(function (appType, appTarget) {
            log.debug('watchContent(isLoadingUnboundApp), appType=' + appType);

            _stopManagedAppWhenRunning(appType);

            if (TYPE.FullBrowser == appType || TYPE.UnicastApp == appType) {
                _addChannelSelectionEventListener();
                log.debug('watchContent(isLoadingUnboundApp), addChannelSelectionEventListener');
            } else {
                log.debug('watchContent(isLoadingUnboundApp), msgObject of method=' + msgObject.method);
                _sendWatchContent();
            }
        });
    }

    function isLoadingUnboundApp(callback) {
        var method = METHOD.OBS_IS_LOADING_UNBOUND_APP,
            methodCallback = function(e) {
            var data,
                methodName,
                appType,
                appTarget;

            try {
                data = e.data;
                methodName = data.method;

                if (method !== methodName) {
                    log.debug('isLoadingUnboundApp(callback), don\'t listen data.method(' + methodName + ')');
                    return ;
                }

                _removeMessageEventListener(methodCallback);

                appType = data.args;
                appTarget = data.target;

                log.debug('isLoadingUnboundApp(callback), appType=' + appType + '(' + TYPE.NAMES[appType]
                    + '), target=' + appTarget);

                if (callback) {
                    callback(appType, appTarget);
                }
            } catch (ex) {
                log.error('isLoadingUnboundApp(callback), ex=' + ex);
            }
        };

        _postMessageToObserver({
            'method': method
        }, methodCallback);
    }

    function startUnboundApp(target) {
        log.debug('startUnboundApp, target=' + target);

        _postMessageToObserver({
            'method': METHOD.OBS_START_UNBOUND_APP,
            'from': APP_ID.kidscare,
            'target': getAppIDWithOID(target)
        });
    }

    function getAppID(name) {
        return APP_ID[name];
    }

    function getApplicationByAppID(appID) {
        return _getApplication('dvb.appId', appID);
    }

    function isBoundApp() {
        var boundApps = _getBoundApplications(),
            boundApp = boundApps && boundApps.length > 0;
        log.debug('boundApp=' + boundApp + (boundApp ? ', app\'s count : ' + boundApps.length : ''));
        return boundApp;
    }

    function getHomePortalID() {
        return getAppID(APP.NAME.HomePortal);
    }

    function getMashupManagerID() {
        return getAppID(APP.NAME.MashupManager);
    }

    function getAppIDWithOID(appID) {
        return APP.OID + CHAR.DOT + appID;
    }

    function _fireSuccessCallback(callback) {
        _fireCallback(callback, ERROR_CODE.SUCCESS_000);
    }

    function _fireCallback(callback, code) {
        if (!callback) {
            return;
        }

        callback(code);
    }

    function _notifyResultByTalkToApp(target, code) {
        log.debug('notifyResult(TalkToApp), target=' + target + ', code=' + code);
        KidsProxy.notifyExecuteResult({
            'method': target,
            'resultCode': code
        });
    }

    function _postMessageToObserver(msgObject, callback) {
        log.debug('postMessage(Observer), message=' + JSON.stringify(msgObject));

        _talkToUnbound(APP_ID.Observer, msgObject, callback);
    }

    function _postMessageToHomePortal(msgObject, callback) {
        log.debug('postMessage(HomePortal), message=' + JSON.stringify(msgObject));

        _talkToUnbound(APP_ID.HomePortal, msgObject, callback);
    }

    function _postMessageToQrator(msgObject, callback) {
        log.debug('postMessage(Qrator), message=' + JSON.stringify(msgObject));

        _talkToUnbound(APP_ID.Qrator, msgObject, callback);
    }

    function _talkToUnbound(id, msgObj, callback) {
        var app = getApplicationByAppID(id);

        _postMessage(app, msgObj, callback);
    }

    function _postMessage(app, msgObj, callback) {

        if (!msgObj) {
            log.error('postMessage, message(' + msgObj + ') invalid!!');
            return ;
        }

        try {
            log.debug('postMessage, application=' + app + ', do postMessage!');
            if (typeof msgObj === 'object') {
                log.debug('postMessage, method=' + msgObj['method']);
            }

            _addMessageEventListener(callback);

            app.window.postMessage(msgObj, '*');
        } catch (ex) {
            log.error('postMessage, ex=' + ex + '\nmsgObj=' + msgObj);
        }
    }

    function _getApplication(key, id) {
        var apps = _findApplications(key, id);

        return apps[0];
    }

    function _getBoundApplications() {
        return _findApplications('useType', 'bound');
    }

    function _findApplications(key, id) {
        return am.findApplications(key, id);
    }

    function _addMessageEventListener(callback) {
        if (!callback) {
            return ;
        }

        log.debug('addMessageEventListener');
        window.addEventListener('message', callback);
    }

    function _removeMessageEventListener(callback) {
        log.debug('removeMessageEventListener');
        window.removeEventListener('message', callback);
    }

    function _stopManagedAppWhenRunning(appType) {
        if (TYPE.FullBrowser == appType
            || TYPE.UnicastApp == appType
            || TYPE.UnboundApp == appType) {
            _stopManagedApp();
        }
    }

    function _stopManagedApp() {
        var promoCCID = NavCoreProxy.getPromoCCID();
        log.debug('stopManagedApp, promoCCID=' + promoCCID);

        _postMessageToObserver({
            'method' : METHOD.OBS_STOP_MANAGED_APP,
            'args' : promoCCID
        });
    }

    function _addChannelSelectionEventListener() {
        channelSelectionEventManager.addChannelSelectionEventListener(channelSelectionEventCallback);
    }

    channelSelectionEventCallback = function (e) {
        log.debug('channelSelectionEvent(callback), msgObject=' + watchContentMsgObject);

        if (!watchContentMsgObject) {
            return;
        }

        channelSelectionEventManager.removeChannelSelectionEventListener(channelSelectionEventCallback);
        log.debug('channelSelectionEvent(callback), remove');

        _sendWatchContent();
    };

    function _sendWatchContent() {
        log.debug('watchContent(to HomePortal), method=' + watchContentMsgObject.method);
        _postMessageToHomePortal(watchContentMsgObject);
        watchContentMsgObject = null;
    }

    function _hasHomeAPI() {
        return homePortalMethods;
    }

    function _existHomeAPI(method) {
        return homePortalMethods ? homePortalMethods.indexOf(method) !== -1 : false;
    }

    function _getStatus(tvStatus, hpStatus, vodStatus, status) {

        var TV = STATUS.TV,
            VOD = STATUS.VOD;

        if (TV.WATCHING_AV === tvStatus || TV.SHOW_APP_WHILE_WATCHING_AV === tvStatus) {
            status.push(STATUS.WATCHING_AV);
        } else if (TV.PLAYING_VOD === tvStatus || TV.SHOW_APP_WHILE_PLAYING_VOD === tvStatus) {
            if (VOD.PLAYING === vodStatus) {
                status.push(STATUS.PLAYING_VOD);
            } else if (VOD.PAUSE === vodStatus) {
                status.push(STATUS.PAUSE_VOD);
            }
        } else if (TV.STANDBY === tvStatus) {
            status.unshift(STATUS.STANDBY);
        }

        if (TV.SHOW_APP === tvStatus || TV.SHOW_APP_WHILE_WATCHING_AV === tvStatus
            || TV.SHOW_APP_WHILE_PLAYING_VOD === tvStatus) {
            status.push(STATUS.SHOW_APP);
        }

        if ('H_DETAIL' === hpStatus || 'H_DETAIL_APP' === hpStatus) {
            status.push(STATUS.VOD_DETAIL);
        }

        return status.join(CHAR.SEMICOLON);
    }

    return {
        talkToApp: talkToApp,
        getAppID: getAppID,
        getApplicationByAppID: getApplicationByAppID,
        isBoundApp: isBoundApp,
        getHomePortalID: getHomePortalID,
        getMashupManagerID: getMashupManagerID,
        getAppIDWithOID: getAppIDWithOID,
        isLoadingUnboundApp: isLoadingUnboundApp,
        startUnboundApp: startUnboundApp,
        searchVOD: searchVOD,
        watchContent: watchContent,
    }
});