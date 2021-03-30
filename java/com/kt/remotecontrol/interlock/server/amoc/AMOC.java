package com.kt.remotecontrol.interlock.server.amoc;

import com.kt.remotecontrol.http.HttpRequest;
import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.ErrorCode;
import com.kt.remotecontrol.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

public class AMOC {
    private static final Log LOG = new Log("AMOC");

    private static final int PURCHASE_SUCCESS = 0;
    private static final int PURCHASE_FAIL = 1;
    private static final int PURCHASE_ALREADY = 2;

    public static String buyContents(Properties params) {
        String said = params.getProperty(Constants.SAID);
        String contentId = params.getProperty(Constants.CONTENTS_ID);
        String contentName = params.getProperty(Constants.CONTENTS_NAME);
        String price = params.getProperty(Constants.PRICE);
        String categoryId = params.getProperty(Constants.CATEGORY_ID);

        String resultCode = null;
        try {
            ArrayList responseData = request(said, contentId, contentName, price, categoryId);
            resultCode = response(responseData);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return resultCode;
    }

    private static ArrayList request(String said, String contentID, String contentName, String buyPrice,
                                  String categoryID) throws IOException {

        String spec = Constants.AMOC_LIVE_URI + Constants.AMOC_SERVICE_BUY_CONTENTS;
        Date now = new Date(System.currentTimeMillis());
        String buyDate = new SimpleDateFormat("yyyyMMddHHmmss").format(now);

        String postData = "saId=" + said             // 가입자SAID
                + "&pkgYn=N"                             // 묶음 구매 여부(N:건별구매)
                + "&contsId=" + contentID        // 컨텐츠ID 또는 카테고리ID
                + "&contsName=" + contentName    // 컨텐츠명 또는 카테고리명
                + "&buyingDate=" + buyDate  // 구매일시
                + "&buyingPrice=" + buyPrice // 구매금액
                + "&buyingType=B"                         // 구매 수단(고정값)
                + "&catId=" + categoryID             // 구매하려는 컨텐츠가 편성된 카테고리ID
                + "&appCd=H"                              // 어플리케이션 코드(H:HomePortal, KC:Kidscare)
                + "&reqPathCd=01"                         // 진입경로(01:홈메뉴)
                + "&ltFlag=0"                             // 장기여부(0:단기구매, 1:장기구매)
                + "&hdYn="                                // HD/SD 시리즈 여부(통합편성된 시리즈카테고리를 구매할 경우만 입력)
                + "&saleYn=N"                             // 재구매할인 적용 여부
                + "&WMOCKey=OTVHome";

        ArrayList results = new HttpRequest().post(spec, postData);
        return results;
    }

    private static String response(ArrayList responseData) {

        if (responseData.isEmpty()) {
            return ErrorCode.C408;
        }

        int flag = PURCHASE_FAIL;
        String message = null;
        String expireDate = "";

        LOG.message("responseBuyContents, responseData=" + responseData);

        if (responseData != null) {
            String jsonData = null;
//            jsonData = JSON.parse(responseData);

            if (jsonData != null) {
//                flag = parseInt(jsonData['flag']);
//                message = jsonData["message"];
//                expireDate = jsonData["expireDate"];
            }
        }

        LOG.message("responseBuyContents flag=" + flag + ", message=" + message
                + ", expireDate=" + expireDate);
        String result = "";

        switch (flag) {
            case PURCHASE_SUCCESS:
                result = ErrorCode.SUCCESS;
                break;
            case PURCHASE_FAIL:
                result = ErrorCode.C408;
                break;
            case PURCHASE_ALREADY:
                result = ErrorCode.C405;
                break;
        }

        if (message != null) {
            result += (CharConstant.CHAR_CARET + message);
        }

        return result;
    }
}
