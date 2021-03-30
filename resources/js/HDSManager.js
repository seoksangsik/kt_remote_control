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
     * 구매핀 인증(HDSDBLogon)
     * : 입력된 Said와 구매핀 번호의 유효성을 확인한 뒤 해당 이용자의 성명, 인증Credential 정보를 리턴한다.
     * 
     * [Parameter]
     * Calltype : 구매핀 ID
     *  - authbuypin : 기존구매핀 인증ID
     *  - authobuypinIPG : 구매핀 인증키 추가수신을 위한 인증ID
     * input1 : SAID
     * input2 : 구매 PIN
     * input3 : 예약 파라미터(Reserved로 고정)
     * HDSToken : HDS 인증시스템으로 받은 Server 토큰
     * STBToken : 생성한 Client 토큰
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
                         // 인증에 실패하였습니다. 핀번호 확인후 다시 시도하시기 바랍니다.[HDSE019]
                         doCallback(callback, method, ERROR_CODE.PASSWORD_502);
                         return;
                     }
                 }
             }
             notifyHDSLinkCode(callback, method);
         }; // finish myCallback,

        // O : returnVal^True|userName^가입자|SvcPnInfo^암호화된 가입자 정보
        //      returnVal^True|userName^가입자|SvcPnInfo^PinAuthValidate=1000ab2d32
        // X : returnVal^False|returnDESC^에러메세지
        log.debug('checkBuyPin');
        request(uri, data, myCallback);
    }

    /**
     * 구매핀 변경
     * [Parameter]
     * said : SAID
     * PinNo : PIN(수정 전 구매 PIN)
     * sValue : BuyPinNo^수정할 구매 PIN
     * HDSToken : HDS 인증시스템으로 받은 Server 토큰
     * STBToken : 생성한 Client 토큰
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
        // X : returnVal^False|returnDESC^에러메세지
        log.debug('changeBuyPin');
        request(uri, data, myCallback);
    }

    /**
     * (통합) 온스크린 가입(OnScreenRegSVC)
     * : STB에서 온스크린으로 양방향 부가상품, 채널VOD, 키즈케어 가입 및 채널업셀링 요청정보를 HDS 인증시스템으로 전송한다.
     * 
     * [Parameter]
     * ServiceType : 온스크린 유형코드
     *  - 03 : 온스크린 키즈케어 가입
     * SAID : SAID
     * ssovalidate : 인증 Credential(User Logon 결과 중 PinAuthValidate 입력)
     * BuyPinAuthKey : 구매핀인증키(구매핀 인증시 리턴받은 BuyPinAuthKey)
     * regPath: 유입경로코드(07 : 마이올레)
     * procSubSVC : 가입할 키즈케어 부가상품코드(2445)
     * sValue : 추가입력정보
     *  - mobileNum : 키즈케어 서비스 제공 휴대폰번호(최대 2대 등록가능 ,로 구분) - 03 : 온스크린 키즈케어 가입
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
        // X : returnVal^False|returnDESC^에러메세지
        log.debug('joinKidscare');
        request(uri, data, myCallback);
    }

    /**
     * (통합) 온스크린 해지(OnScreenCnclSVC)
     * : 가입자가 온라인(서비스 사용 중)에서 VOD/채널 부가상품을 해지할 수 있는 기능이다.
     * STB으로부터 VOD/채널 부가상품 해지요청을 받고 관련 시스템(ICIS,nCRAB)에 부가상품 해지정보를 연동한다.
     * 
     * [Parameter]
     * SAID : SAID
     * ssovalidate : 인증 Credential(User Logon 결과 중 PinAuthValidate 입력)
     * BuyPinAuthKey : 구매핀인증키
     * regPath : 유입경로코드(07 : 마이올레)
     * procSubSVC : 가입할 키즈케어 부가상품코드(2445)
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
        // X : returnVal^False|returnDESC^에러메세지
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