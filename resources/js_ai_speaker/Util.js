'use strict';

acba.define('js:Util',
    ['log:kids(Util)', 'js:Enum'],
    function (log, Enum) {

    KIDS.register(log);

    var METHOD = Enum.METHOD;

    function getJSONObject(data) {
        log.debug('getJSONObject, data=' + data);
        var jsonObject = null;

        try {
            jsonObject = JSON.parse(data);
        } catch (ex) {
            log.error('getJSONObject, exception=' + ex);
        }

        return jsonObject;
    }

    function isObject(data) {
        return data && typeof data === 'object';
    }

    function isObserver(method) {
        return method ? method.indexOf(METHOD.OBSERVER) === 0 : false;
    }

    function isHomePortal(method) {
        return method ? method.indexOf(METHOD.HOMEPORTAL) === 0 : false;
    }

    /**
     * method is HP_WATCH_CONTENT or HP_WATCH_CONTENT_FORCED
     * @param method
     * @returns {boolean}
     */
    function isWatchContentAPI(method) {
        return method ? method.indexOf(METHOD.HP_WATCH_CONTENT) === 0 : false;
    }

    return {
        getJSONObject: getJSONObject,
        isObject: isObject,
        isObserver: isObserver,
        isHomePortal: isHomePortal,
        isWatchContentAPI: isWatchContentAPI
    }
});