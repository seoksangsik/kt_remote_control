'use strict';

acba.define('js:HDSManager', ['log:kids(HDSManager)', 'js:Enum', 'js:Config', 'js:network'], 
function (log, Enum, config, network) {

    log.level = log.DEBUG;

    var HDSManager = {},
        ssoClient = oipfObjectFactory.createSSOClient(),
        hdsUrl = config.getHDSURL(),
        SERVICE = Enum.SERVICE,
        ERROR_CODE = Enum.ERROR_CODE,
        executeAPI = {
            'checkBuyPin': checkBuyPin,
            'changeBuyPin': changeBuyPin,
            'joinKidscare': joinKidscare,
            'cancelKidscare': cancelKidscare
        };

    function onLogon(parameter, callback) {
        var callbackLogon = function(event) {
            log.debug('onLogon, event=' + event + ', parameter=' + parameter);

            if (event.success) {
                if (event.result) {
                    log.debug('onLogon, message=\'' + event.result + '\'');
                    parameter['stbToken'] = ssoClient.getSTBToken();
                    parameter['hdsToken'] = ssoClient.getHDSToken();
                    log.debug('onLogon, stbToken=' + parameter['stbToken'] + ', hdsToken=' + parameter['hdsToken']);
                    executeWithToken(parameter, callback);
                } else {
                    log.error('onLogon, invalid result!, don\'t call executeWithToken');
                }
            } else {
                log.error('onLogon, fail!, don\'t call executeWithToken');
            }
            removeUserLogonEventListener();
        };

        function removeUserLogonEventListener(){
            ssoClient.removeUserLogonEventListener(callbackLogon);
            log.debug('removeUserLogonEventListener!');
        }

        return callbackLogon;
    }

    function execute(parameter, callback) {
        log.debug('execute, try to call ssoClient.userLogon because token needs');
        ssoClient.addUserLogonEventListener(onLogon(parameter, callback));
        ssoClient.userLogon('HOMEPORTAL');
    }

    function executeWithToken(parameter, callback) {
        var depends = parameter['depends'],
            method = parameter['method'];

        log.debug('executeWithToken, method=\'' + method + '\', depends=\'' + depends + '\'');

        if (method === 'checkBuyPin' || method === 'changeBuyPin') {
            parameter['Calltype'] = SERVICE.ID_AUTH_BUY_PIN;
        }

        if (depends === 'checkBuyPin') {
            log.debug('executeWithToken, ' + depends + ' depends API');
            if (method === 'joinKidscare' || method === 'cancelKidscare') {
                parameter['Calltype'] = SERVICE.ID_AUTH_BUY_PIN_IPG;
            }
            executeAPI[depends](parameter, callback);
        } else {
            try {
                executeAPI[method](parameter, callback);
                log.debug('executeWithToken, ' + method + ' method API');
            }  catch (Exception) {
                log.error('executeWithToken, don\'t implements ' + method + ' method API');
                if (callback) {
                    doCallback(callback, method, ERROR_CODE.ETC_BY_STB_499);
                }
            }
        }
    }

    /**
     * ????????? ??????(HDSDBLogon)
     * : ????????? Said??? ????????? ????????? ???????????? ????????? ??? ?????? ???????????? ??????, ??????Credential ????????? ????????????.
     * 
     * [Parameter]
     * Calltype : ????????? ID
     *  - authbuypin : ??????????????? ??????ID
     *  - authobuypinIPG : ????????? ????????? ??????????????? ?????? ??????ID
     * input1 : SAID
     * input2 : ?????? PIN
     * input3 : ?????? ????????????(Reserved??? ??????)
     * HDSToken : HDS ????????????????????? ?????? Server ??????
     * STBToken : ????????? Client ??????
     */
    function checkBuyPin(parameter, callback) {
        var uri = hdsUrl + '/' + SERVICE.PIN + '/' + SERVICE.METHOD_HDS_DBLOGON,
            data = 'Calltype=' + parameter['Calltype']
                    + '&input1=' + parameter['said']
                    + '&input2=' + parameter['pin']
                    + '&input3=Reserved&HDSToken=' + parameter['hdsToken']
                    + '&STBToken=' + parameter['stbToken'],
             myCallback;

         myCallback = function (isSuccess, responseXML) {
             log.debug('checkBuyPin(callback), isSuccess=' + isSuccess + ', responseXML=' + responseXML);
             var hdsResponse,
                aSvcPinInfo,
                 method = parameter['method'],
                 stringNodeList = null;

             if (isSuccess) {
                 stringNodeList = responseXML.getElementsByTagName('string');

                 if (stringNodeList && stringNodeList.length > 0) {
                     hdsResponse = parseHDSResponseByText(stringNodeList[0].childNodes[0].nodeValue);
                     log.debug('checkBuyPin(callback), result=' + hdsResponse.result);
                     if (hdsResponse.result) {
                         if (parameter['depends'] && parameter['depends'] !== method) {
                             aSvcPinInfo = hdsResponse.SvcPinInfo;
                             if (aSvcPinInfo) {
                                if (aSvcPinInfo.PinAuthValidate) {
                                    parameter['PinAuthValidate'] = aSvcPinInfo.PinAuthValidate;
                                } else {
                                    log.error('checkBuyPin(callback), SvcPinInfo.PinAuthValidate(' + aSvcPinInfo.PinAuthValidate + ') is invalid!');
                                }
                                if (aSvcPinInfo.BuyPinAuthKey) {
                                    parameter['BuyPinAuthKey'] = aSvcPinInfo.BuyPinAuthKey;
                                } else {
                                    log.error('checkBuyPin(callback), SvcPinInfo.BuyPinAuthKey(' + aSvcPinInfo.BuyPinAuthKey + ') is invalid!');
                                }
                             } else {
                                 log.error('checkBuyPin(callback), SvcPinInfo(' + aSvcPinInfo + ') is invalid!');
                             }
                             executeAPI[method](parameter, callback);
                         } else {
                             notifySuccessCode(callback, method);
                         }
                         return;
                     } else if ((hdsResponse.returnDESC && hdsResponse.returnDESC.indexOf('HDSE019') > 0)
                                || (hdsResponse.returnCode && hdsResponse.returnCode.indexOf('HDSE019') > 0)) {
                         // ????????? ?????????????????????. ????????? ????????? ?????? ??????????????? ????????????.[HDSE019]
                         doCallback(callback, method, ERROR_CODE.PASSWORD_502);
                         return;
                     }
                 }
             }
             notifyHDSLinkCode(callback, method);
         }; // finish myCallback,

        // O : returnVal^True|userName^?????????|SvcPnInfo^???????????? ????????? ??????
        //      returnVal^True|userName^?????????|SvcPnInfo^PinAuthValidate=1000ab2d32
        // X : returnVal^False|returnDESC^???????????????
        log.debug('checkBuyPin');
        request(uri, data, myCallback);
    }

    /**
     * ????????? ??????
     * [Parameter]
     * said : SAID
     * PinNo : PIN(?????? ??? ?????? PIN)
     * sValue : BuyPinNo^????????? ?????? PIN
     * HDSToken : HDS ????????????????????? ?????? Server ??????
     * STBToken : ????????? Client ??????
     */
    function changeBuyPin(parameter, callback){
        var uri = hdsUrl + '/' + SERVICE.PIN + '/' + SERVICE.METHOD_HDS_PINUPDATE,
            data = 'said=' + parameter['said']
                    + '&PinNo=' + parameter['pin']
                    + '&sValue=BuyPinNo^' + parameter['newPin']
                    + '&HDSToken=' + parameter['hdsToken']
                    + '&STBToken=' + parameter['stbToken'],
            myCallback;

        myCallback = function(isSuccess, responseXML) {
            log.debug('changeBuyPin(callback), isSuccess=' + isSuccess + ', responseXML=' + responseXML);

            var hdsResponse = null,
                method = parameter['method'],
                stringNodeList;

            if (isSuccess) {
                stringNodeList = responseXML.getElementsByTagName('string');

                if (stringNodeList && stringNodeList.length > 0) {
                    hdsResponse = parseHDSResponseByText(stringNodeList[0].childNodes[0].nodeValue);
                    log.debug('changeBuyPin(callback), result=' + hdsResponse.result);
                    if (hdsResponse.result) {
                        notifySuccessCode(callback, method);
                        return ;
                    }
                }
            }

            notifyHDSLinkCode(callback, method);
        };

        // O : returnVal^True|returnDESC^Success
        // X : returnVal^False|returnDESC^???????????????
        log.debug('changeBuyPin');
        request(uri, data, myCallback);
    }

    /**
     * (??????) ???????????? ??????(OnScreenRegSVC)
     * : STB?????? ?????????????????? ????????? ????????????, ??????VOD, ???????????? ?????? ??? ??????????????? ??????????????? HDS ????????????????????? ????????????.
     * 
     * [Parameter]
     * ServiceType : ???????????? ????????????
     *  - 03 : ???????????? ???????????? ??????
     * SAID : SAID
     * ssovalidate : ?????? Credential(User Logon ?????? ??? PinAuthValidate ??????)
     * BuyPinAuthKey : ??????????????????(????????? ????????? ???????????? BuyPinAuthKey)
     * regPath: ??????????????????(07 : ????????????)
     * procSubSVC : ????????? ???????????? ??????????????????(2445)
     * sValue : ??????????????????
     *  - mobileNum : ???????????? ????????? ?????? ???????????????(?????? 2??? ???????????? ,??? ??????) - 03 : ???????????? ???????????? ??????
     */
    function joinKidscare(parameter, callback) {
        var uri = hdsUrl + '/' + SERVICE.ON_LINE + '/' + SERVICE.METHOD_ON_SCREEN_JOIN,
            data = 'ServiceType=' + SERVICE.CODE_ON_SCREEN_JOIN_KIDSCARE
                     + '&SAID=' + parameter['said']
                     + '&ssovalidate=' + parameter['PinAuthValidate']
                     + '&BuyPinAuthKey=' + parameter['BuyPinAuthKey']
                     + '&regPath=' + SERVICE.REG_PATH_MY_OLLEH
                     + '&procSubSVC=' + SERVICE.KIDSCARE_CODE
                     + '&sValue=mobileNum^' + parameter['cellPhone'],
             myCallback;

        myCallback = function(isSuccess, responseXML) {
            log.debug('joinKidscare(callback), isSuccess=' + isSuccess + ', responseXML=' + responseXML);

            var hdsResponse = null,
                method = parameter['method'],
                stringNodeList,
                errorMessage= '';

            if (isSuccess) {
                stringNodeList = responseXML.getElementsByTagName('string');

                if (stringNodeList && stringNodeList.length > 0) {
                    hdsResponse = parseHDSResponseByText(stringNodeList[0].childNodes[0].nodeValue);
                    log.debug('joinKidscare(callback), result=' + hdsResponse.result);
                    if (hdsResponse.result) {
                        notifySuccessCode(callback, method);
                    } else {
                        if (hdsResponse.returnCode) {
                            errorMessage = '[' + hdsResponse.returnCode + '] ';
                        }
                        errorMessage += hdsResponse.returnDESC;

                        doCallback(callback, method, ERROR_CODE.LINK_HDS_601, errorMessage);
                    }
                    return ;
                }
            }
            notifyHDSLinkCode(callback, method);
        };

        // O : returnVal^True|returnDESC^Success
        // X : returnVal^False|returnDESC^???????????????
        log.debug('joinKidscare');
        request(uri, data, myCallback);
    }

    /**
     * (??????) ???????????? ??????(OnScreenCnclSVC)
     * : ???????????? ?????????(????????? ?????? ???)?????? VOD/?????? ??????????????? ????????? ??? ?????? ????????????.
     * STB???????????? VOD/?????? ???????????? ??????????????? ?????? ?????? ?????????(ICIS,nCRAB)??? ???????????? ??????????????? ????????????.
     * 
     * [Parameter]
     * SAID : SAID
     * ssovalidate : ?????? Credential(User Logon ?????? ??? PinAuthValidate ??????)
     * BuyPinAuthKey : ??????????????????
     * regPath : ??????????????????(07 : ????????????)
     * procSubSVC : ????????? ???????????? ??????????????????(2445)
     */
    function cancelKidscare(parameter, callback) {
        var uri = hdsUrl + '/' + SERVICE.ON_LINE + '/' + SERVICE.METHOD_ON_SCREEN_CANCEL,
            data = 'SAID=' + parameter['said']
                    + '&ssovalidate=' + parameter['PinAuthValidate']
                    + '&BuyPinAuthKey=' + parameter['BuyPinAuthKey']
                    + '&regPath=' + SERVICE.REG_PATH_MY_OLLEH
                    + '&procSubSVC=' + SERVICE.KIDSCARE_CODE
                    + '&sValue=',
            myCallback;

        myCallback = function (isSuccess, responseXML) {
            log.debug('cancelKidscare(callback), isSuccess=' + isSuccess + ', responseXML=' + responseXML);

            var hdsResponse = null,
                method = parameter['method'],
                stringNodeList,
                errorMessage = '';

            if (isSuccess) {
                stringNodeList = responseXML.getElementsByTagName('string');
    
                if (stringNodeList && stringNodeList.length > 0) {
                    hdsResponse = parseHDSResponseByText(stringNodeList[0].childNodes[0].nodeValue);
                    log.debug('cancelKidscare(callback), result=' + hdsResponse.result);
                    if (hdsResponse.result) {
                        notifySuccessCode(callback, method);
                    } else {
                        if (hdsResponse.returnCode) {
                            errorMessage = '[' + hdsResponse.returnCode + '] ';
                        }
                        errorMessage += hdsResponse.returnDESC;

                        doCallback(callback, method, ERROR_CODE.LINK_HDS_601, errorMessage);
                    }
                    return;
                }
            }
            notifyHDSLinkCode(callback, method);
        };

        // O : returnVal^True|returnDESC^Success
        // X : returnVal^False|returnDESC^???????????????
        log.debug('cancelKidscare');
        request(uri, data, myCallback);
    }

    function HDSResponse() {
        this.result = false; // made by sangsik
        this.returnVal;
        this.userName;
        this.SvcPinInfo;
        this.returnDESC;
        this.returnCode;
    }

    function SvcPinInfo() {
        this.PinAuthValidate;
        this.PinAuthKey;
        this.homePhone;
        this.IPTVSAID;
        this.UserAddr;
        this.homePhone;
        this.BuyPinAuthKey;
    }

    function notifySuccessCode(callback, method) {
        log.debug('notifySuccessCode(000)');
        doCallback(callback, method, ERROR_CODE.SUCCESS_000);
    }

    function notifyHDSLinkCode(callback, method) {
        log.debug('notifyHDSLinkCode(601)');
        doCallback(callback, method, ERROR_CODE.LINK_HDS_601);
    }

    function doCallback(callback, method, resultCode, errorMsg) {
        log.debug('doCallback, method=' + method + ', resultCode=' + resultCode);
        var resultObj = {};
        resultObj['method'] = method;
        resultObj['resultCode'] = resultCode;

        if (errorMsg) {
            log.debug('doCallback, errorMessage=' + errorMsg);
            resultObj['errorMessage'] = errorMsg;
        }

        if (callback) {
            log.debug('doCallback');
            callback(resultObj);
        } else {
            log.debug('doCallback, invalid callback!');
        }
    }

    function parseHDSResponseByText(resultText) {
        log.debug('parseHDSResponseByText, resultText=' + resultText);
        var hdsResponse = new HDSResponse(),
            i = 0,
            key, value,
            responseArray = resultText.split('|'),
            parseArray;

        for (i = 0; i < responseArray.length; i++) {
            parseArray = responseArray[i].split('^');

            if (parseArray) {
                if (parseArray.length === 2) {
                    key = parseArray[0];
                    value = parseArray[1];

//                    log.debug('parseHDSResponseByText, [' + i + '] key=' + key + ', value=' + value);
                    if (key === 'returnVal') {
                        hdsResponse.returnVal = value;
                        hdsResponse.result = ('true' == value.toLowerCase());
                    } else if (key === 'UserName') {
                        hdsResponse.UserName = value;
                    } else if (key === 'SvcPinInfo') {
                        hdsResponse.SvcPinInfo = parseSvcPinInfo(value.split('_'));
                    } else if (key === 'returnDESC') {
                        hdsResponse.returnDESC = value;
                        hdsResponse.result = ('success' == value.toLowerCase());
                    } else if (key === 'returnCode') {
                        hdsResponse.returnCode = value;
                    } else {
                        log.info('parseHDSResponseByText, undefined key=' + key + ', value=' + value);
                    }
                } else if (parseArray.length === 3) {
                    value = parseArray[2];
                    log.debug('parseHDSResponseByText, PinAuthValidate, value=' + value);
                    if (value.indexOf('PinAuthValidate') === -1) {
                        log.debug('parseHDSResponseByText, doesn\'t exist PinAuthValidate');
                    } else {
                        hdsResponse.SvcPinInfo = parseSvcPinInfo([value]);
                    }
                } else {
                    log.error('parseHDSResponseByText, invalid parsing(^)');
                }
            } else {
                log.error('parseHDSResponseByText, parseArray=' + parseArray);
            }
        }
        return hdsResponse;
    }

    function parseHDSResponseByXML(childNodeArray) {
        log.debug('parseHDSResponseByXML, childNodeArray=' + childNodeArray);

        var hdsResponse = new HDSResponse(),
            i = 0,
            aNode = null;

        if (childNodeArray) {
            for (i = 0; i < childNodeArray.length; i++) {
                aNode = childNodeArray[i];

                if (aNode) {
                    if ('returnVal' === aNode.nodeName) {
                        hdsResponse.returnVal = aNode.nodeValue;
                    } else if ('UserName' === aNode.nodeName) {
                        hdsResponse.UserName = aNode.nodeValue;
                    } else if ('SvcPinInfo' === aNode.nodeName) {
                        hdsResponse.SvcPinInfo = aNode.nodeValue;
                    } else if ('returnDESC' === aNode.nodeName) {
                        hdsResponse.returnDESC = aNode.nodeValue;
                    } else if ('returnCode' === aNode.nodeName) {
                        hdsResponse.returnCode = aNode.nodeValue;
                    } else {
                        log.error('parseHDSResponseByXML, undefined node, name=' + aNode.nodeName + ', value=' + aNode.nodeValue);
                    }
                } else {
                    log.error('parseHDSResponseByXML, invalid node=' + aNode);
                }
            }
        } else {
            log.error('parseHDSResponseByXML, invalid childNodeArray=' + childNodeArray);
        }

        return hdsResponse;
    }

    function parseSvcPinInfo(svcPinInfoArray) {
        log.debug('parseSvcPinInfo, svcPinInfoArray=' + svcPinInfoArray);
        var svcPinInfo = new SvcPinInfo(),
            i = 0,
            key, value,
            parseArray;

        for (i = 0; i < svcPinInfoArray.length; i++) {
            parseArray = svcPinInfoArray[i].split('=');

            if (parseArray && parseArray.length === 2) {
                key = parseArray[0];
                value = parseArray[1];

                if ('PinAuthValidate' === key) {
                    svcPinInfo.PinAuthValidate = value;
                } else if ('PinAuthKey' === key) {
                    svcPinInfo.PinAuthKey = value;
                } else if ('IPTVSAID' === key) {
                    svcPinInfo.IPTVSAID = value;
                } else if ('UserAddr' === key) {
                    svcPinInfo.UserAddr = value;
                } else if ('homePhone' === key) {
                    svcPinInfo.homePhone = value;
                } else if ('BuyPinAuthKey' === key) {
                    svcPinInfo.BuyPinAuthKey = value;
                } else {
                    log.info('parseSvcPinInfo, undefined key=' + key + ', value=' + value);
                }
            } else {
                log.debug('parseSvcPinInfo, parseArray.length=' + parseArray.length);
            }
        }
        return svcPinInfo;
    }

    function request(url, data, callback) {
        log.debug('request, url=\'' + url + '\', data=\'' + data + '\'');

        var xhr = network.Ajax.getInstance();
        xhr.setTimeout(network.SERVER_TIMEOUT, callback);
        xhr.setAcceptCharset(null);
        xhr.setCallbackType(xhr.CALLBACK_TYPE_XML);
        xhr.setCallbackListener(callback, callback);
        xhr.sendPostMethod(url, data);
    }

    Object.defineProperties(HDSManager, {
        execute: {
            value: execute,
            writable: false
        },
    });

    return HDSManager;
});