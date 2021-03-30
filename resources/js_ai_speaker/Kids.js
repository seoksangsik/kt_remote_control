'use strict';

acba.define('js:Kids', ['log:kids(base)'],
    function (log) {

    var _logObjects = [],
     _logNames = ['Error', 'Warn', 'Info', 'Debug', 'Trace'],
    configObj = oipfObjectFactory.createConfigurationObject(),
    configuration = configObj.configuration;

    KIDS.register = register;
    KIDS.changeLogLevel = changeLogLevel;

    register(log);

    function register(obj) {
        log.debug('register');

        _logObjects.push(obj);
    }

    function changeLogLevel(value) {
        log.debug('changeLogLevel, value=' + value);

        var logLevel;

        if (typeof value === 'string') {
            logLevel = value.toUpperCase();
        } else if (typeof value === 'number') {
            logLevel = value;
        } else {
            throw 'changeLogLevel, Error - TypeError level is not a string or number';
        }

        _changeLogLevel(logLevel);
    }

    function initLog(value) {
        var enabled = configuration.getText('log.enabled');

        if ('true' === enabled) {
            _changeLogLevel(log.DEBUG);
        } else {
            _changeLogLevel(value);
        }
    }

    function _changeLogLevel(value) {
        var level = _getLogLevel(value);

        _changeLogObject(level);
        KIDS.LOG_LEVEL = _logNames[level];
    }

    function _getLogLevel(logLevel) {
        switch (logLevel) {
            case 'WARN':
            case log.WARN:
                return log.WARN;
            case 'INFO':
            case log.INFO:
                return log.INFO;
            case 'DEBUG':
            case log.DEBUG:
                return log.DEBUG;
            case 'TRACE':
            case log.TRACE:
                return log.TRACE;
            default:
                return log.ERROR;
        }
    }

    function _changeLogObject(level) {
        _logObjects.forEach(function (logObj) {
            logObj.level = level;
        });
    }

    return {
        initLog: initLog
    };
});