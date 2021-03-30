'use strict';

acba.define('js:AMOCProxy',
    ['log:kids(AMOCProxy)', 'js:Enum', 'js:network', 'js:Config', 'js:KidsProxy'],
function (log, Enum, network, Config, KidsProxy) {

    KIDS.register(log);

    var VALUE = Enum.VALUE,
        CHAR = Enum.CHAR,
        ERROR_CODE = Enum.ERROR_CODE,
        ajax = network.Ajax.getInstance();

    function buyContents(params) {
        if (!params) {
            KidsProxy.notifyRemotePurchase(ERROR_CODE.ETC_BY_STB_499);
            return;
        }

        log.debug('buyContents, saId=' + params['saId'] + ', contentId=' + params['contsId']
            + ', contentName=' + params['contsName'] + ', buyingDate=' + params['buyingDate']
            + ', buyingPrice=' + params['buyingPrice'] + ', categoryId=' + params['catId']);

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
        ajax.setTimeout(VALUE.SERVER_TIMEOUT, _responseBuyContents);
        ajax.setCallbackListener(_responseBuyContents, _responseBuyContents);
        ajax.sendPostMethod(Config.getAMOCURL() + '/amoc-api/vod/buy/in-cash', callParam);
    }

    function _responseBuyContents(isSuccess, responseData) {
        var PURCHASE = VALUE.PURCHASE,
            flag = PURCHASE.FAIL,
            message = '',
            result = '',
            expireDate = '',
            jsonData;

        log.debug('responseBuyContents isSuccess=' + isSuccess + ', responseData=' + responseData);

        if (isSuccess && responseData) {
            jsonData = JSON.parse(responseData);

            if (jsonData) {
                flag = parseInt(jsonData['flag']);
                message = jsonData['message'];
                expireDate = jsonData['expireDate'];
            }
        }

        log.debug('responseBuyContents flag=' + flag + ', message=' + message + ', expireDate=' + expireDate);

        switch (flag) {
            case PURCHASE.SUCCESS:
                result = ERROR_CODE.SUCCESS_000;
                break;
            case PURCHASE.FAIL:
                result = ERROR_CODE.PURCHASE_FAIL_408;
                break;
            case PURCHASE.ALREADY:
                result = ERROR_CODE.PURCHASE_ALREADY_405;
                break;
        }

        if (message) {
            result += (CHAR.CARET + message);
        }

        KidsProxy.notifyRemotePurchase(result);
    }

    return {
        buyContents: buyContents
    }
});