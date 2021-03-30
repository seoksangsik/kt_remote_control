'use strict';

acba.define('js:NavCoreProxy', ['log:kids(NavCoreProxy)', 'js:nav/NavLegacy',
    'js:nav/External'],
function(log, navLegacy, external) {
    log.level = log.DEBUG;

    var _NavCoreProxy = {};

    // nav/NavLegacy API
    function getCurrentChannel() {
        log.debug('getCurrentChannel');
        return navLegacy.getCurrentChannel();
    }

    function getRealCurrentChannel() {
        log.debug('getRealCurrentChannel');
        return navLegacy.getRealCurrentChannel();
    }

    function selectPromoChannel() {
        log.debug('selectPromoChannel');
        return navLegacy.selectPromoChannel();
    }

    function getPromoCCID() {
        log.debug('getPromoCCID');
        return navLegacy.getPromoCCID();
    }

    // nav/External API
    function enableIMELocation(x, y) {
        log.debug('enableIMELocation, x=' + x + ', y=' + y);
        external.enableIMEStaticLocation(x, y);
    }

    function disableIMELocation() {
        log.debug('disableIMELocation');
        external.disableIMEStaticLocation();
    }

    Object.defineProperties(_NavCoreProxy, {
        getCurrentChannel : {value : getCurrentChannel},
        getRealCurrentChannel : {value : getRealCurrentChannel},
        selectPromoChannel : {value : selectPromoChannel},
        getPromoCCID : {value : getPromoCCID},
        enableIMELocation : {value : enableIMELocation},
        disableIMELocation : {value : disableIMELocation},
    });

    return _NavCoreProxy;
});