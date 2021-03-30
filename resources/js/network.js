"use strict";

acba.define("js:network", ["log:kids(network)"],
function (log) {
    log.level = log.DEBUG;

    var STATE_COMPLETE = 4,
        STATUS_OK = 200,
        SERVER_TIMEOUT = 3500,
        CALLBACK_TYPE_TEXT = 'text',
        CALLBACK_TYPE_XML = 'xml',
        CALLBACK_TYPE_XHR = 'xhr';

    var _Ajax = (function () {
        var _instance = undefined;

        function _init() {
            var _xhr = new XMLHttpRequest(),
                _contentType = undefined,
                _acceptCharset = 'UTF-8',
                _callbackType = undefined,
                _timeout = SERVER_TIMEOUT,
                _timeout_lnr = undefined,
                _success_lnr = undefined,
                _error_lnr = undefined;

            _xhr.onreadystatechange = (function (lnr) {
                var state_listener = lnr;
                return function () {
                    // This function is evaluated on XMLHttpRequest context.
                    // So, "this" means XMLHttpRequest itself.
                    state_listener(this);
                };
            }(_invokeStateListener));

            function _invokeStateListener(httpRequest) {
                var param;

                if (httpRequest.readyState == STATE_COMPLETE) {
                    log.info("###################### server message ################################");
                    log.info("## responseText : " + httpRequest.responseText);
                    log.info("######################################################################");

                    log.info('callbackType=' + _callbackType);

                    if (_callbackType === CALLBACK_TYPE_XHR) {
                        param = httpRequest;
                    } else if (_callbackType === CALLBACK_TYPE_XML) {
                        param = httpRequest.responseXML;
                    } else {
                        param = httpRequest.responseText;
                    }
                    callback(httpRequest.status == STATUS_OK, param);
                }
            }

            function _initXHR() {
                if (_contentType) {
                    _xhr.setRequestHeader('Content-Type', _contentType);
                } else {
                    _xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
                }

                if (_acceptCharset) {
                    _xhr.setRequestHeader('accept-charset', _acceptCharset);
                }

                if (_timeout != undefined) {
                    _xhr.timeout = _timeout;
                    _xhr.ontimeout = _timeout_lnr;
                }
            }

            function callback(success, httpRequest) {
                var callbackListener = null;

                if (success) {
                    if (_success_lnr) {
                        callbackListener = _success_lnr;
                    } else {
                        log.debug("don't call success listener!");
                    }
                } else {
                    if (_error_lnr) {
                        callbackListener = _error_lnr;
                    } else {
                        log.debug("don't call error listener!");
                    }
                }

                if (callbackListener) {
                    log.debug("callback listener! success=" + success + ", httpRequest=" + httpRequest);
                    callbackListener(success, httpRequest);
                }
            }

            return { // exposed interface
                CALLBACK_TYPE_TEXT : CALLBACK_TYPE_TEXT,
                CALLBACK_TYPE_XML : CALLBACK_TYPE_XML,
                CALLBACK_TYPE_XHR : CALLBACK_TYPE_XHR,
                sendPostMethod: function (url, msg) {
                    try {
                        _xhr.open("POST", url, true);
                        _initXHR();
                        _xhr.send(msg);
                    } catch (exception) {
                        log.debug("catch exception in the sendPostMethod");
                        callback(false, exception); 
                    }
                },
                setContentType: function (contentType) {
                    _contentType = contentType;
                },
                setAcceptCharset: function (acceptCharset) {
                   _acceptCharset = acceptCharset; 
                },
                setCallbackType : function (callbackType) {
                   _callbackType = callbackType; 
                },
                setTimeout: function (time, lnr) {
                    _timeout = time;
                    _timeout_lnr = lnr;
                },
                setCallbackListener: function (successListener, errorListener) {
                    _success_lnr = successListener;
                    _error_lnr = errorListener;
                }
            };
        }

        return {
            getInstance: function () {
                if (!_instance) {
                    _instance = _init();
                }
                return _instance;
            }
        };
    }());

    return {
        Ajax: _Ajax
    };
});