'use strict';

acba.define('js:KidsProxy',
    ['log:kids(KidsProxy)', 'rop:kidscare.NKidsPopupManager'],
    function (log, ropPopup) {

    KIDS.register(log);

    function setPopupListener(object) {
        log.debug('setPopupListener, object=' + object);
        ropPopup.setPopupListener(object);
    }

    function notifyKeyword(result) {
        log.debug('notifyKeyword, result=' + result);
        ropPopup.notifyKeyword(result);
    }

    function notifyRemotePurchase(result) {
        log.debug('notifyRemotePurchase, result=' + result);
        ropPopup.notifyRemotePurchase(result);
    }

    function notifyExecuteResult(result) {
        if (!result) {
            log.error('notifyExecuteResult, invalid result=' + result);
            return;
        }
        log.debug('notifyExecuteResult, result=' + result);
        ropPopup.notifyExecuteResult(result);
    }

    return {
        setPopupListener: setPopupListener,
        notifyKeyword: notifyKeyword,
        notifyRemotePurchase: notifyRemotePurchase,
        notifyExecuteResult: notifyExecuteResult
    }
});