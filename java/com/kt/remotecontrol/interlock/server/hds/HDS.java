package com.kt.remotecontrol.interlock.server.hds;

import com.kt.remotecontrol.http.HttpRequest;
import com.kt.remotecontrol.WorkingConfig;
import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.ErrorCode;
import com.kt.remotecontrol.util.KeyConstant;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.util.MethodConstant;
import com.kt.remotecontrol.util.TimeConstant;

import java.util.ArrayList;
import java.util.HashMap;

public class HDS {
    private static final Log LOG = new Log("HDS");

    private static String CODE_ON_SCREEN_JOIN_KIDSCARE = "03";
    private static String REG_PATH_MY_OLLEH = "07";
    private static String ID_AUTH_BUY_PIN = "authbuypin";
    private static String ID_AUTH_BUY_PIN_IPG = "authbuypinIPG";

    private static String url = WorkingConfig.HDS_SERVER;

    public static BasicResponse checkBuyPin(String pin) {
        return checkBuyPin(pin, false);
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
    public static BasicResponse checkBuyPin(String pin, boolean addedKey) {
        String uri = url + Constants.HDS_SERVICE_DBLOGON;
        String callType = addedKey ? ID_AUTH_BUY_PIN_IPG : ID_AUTH_BUY_PIN;
        String said = ProxyManager.otherHandler().getSAID();
        String data = "Calltype=" + callType
                    + "&input1=" + said
                    + "&input2=" + pin
                    + "&input3=Reserved&" + getTokens(said);

        // O : returnVal^True|userName^가입자|SvcPnInfo^암호화된 가입자 정보
        //     returnVal^True|userName^가입자|SvcPnInfo^PinAuthValidate=1000ab2d32
        // X : returnVal^False|returnDESC^에러메세지
        LOG.message("checkBuyPin");
        return new LogonResponse(requestPost(uri, data));
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
    // <?xml version=\"1.0\" encoding=\"utf-8\"?><string xmlns=\"http://tempuri.org/\">returnVal^False|returnDESC^PIN 번호가 일치하지 않습니다</string>
    public static String changeBuyPin(String pin, String newPIN) {
        String uri = url + Constants.HDS_SERVICE_PINUPDATE;
        String said = ProxyManager.otherHandler().getSAID();
        String data = "said=" + said
                    + "&PinNo=" + pin
                    + "&sValue=BuyPinNo^" + newPIN
                    + "&" + getTokens(said);

        LOG.message("changeBuyPin");

        return getResultByRequestPost(uri, data);
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
    // <?xml version="1.0" encoding="utf-8"?><string xmlns="http://tempuri.org/">returnVal^True|returnCode^HDSS001|returnDESC^SUCCESS</string>
    // <?xml version="1.0" encoding="utf-8"?><string xmlns="http://tempuri.org/">returnVal^False|returnDESC^인증에 실패하였습니다. 핀번호 확인후 다시 시도하시기 바랍니다.[HDSE019]</string>
    public static String joinKidscare(String said, String passwd, String cellPhone) {

        LogonResponse logonResponse = checkBuyPinByKidscare(passwd);

        if (!logonResponse.isResult()) {
            return getResult(logonResponse);
        }

        String uri = url + Constants.HDS_SERVICE_ON_SCREEN_JOIN;
        String parameter = getParamByKidscareService(said, logonResponse);
        String data = "ServiceType=" + CODE_ON_SCREEN_JOIN_KIDSCARE
                    + "&" + parameter + "mobileNum^" + cellPhone;

        LOG.message("joinKidscare");
        return getResultByRequestPost(uri, data);
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
    // <?xml version="1.0" encoding="utf-8"?><string xmlns="http://tempuri.org/">returnVal^True|returnCode^HDSS001|returnDESC^SUCCESS</string>
    // <?xml version="1.0" encoding="utf-8"?><string xmlns="http://tempuri.org/">returnVal^False|returnCode^HDSE099|returnDESC^HDS 시스템 에러</string>
    public static String cancelKidscare(String said, String passwd) {
        LogonResponse logonResponse = checkBuyPinByKidscare(passwd);

        if (!logonResponse.isResult()) {
            return getResult(logonResponse);
        }

        String uri = url + Constants.HDS_SERVICE_ON_SCREEN_CANCEL;
        String data = getParamByKidscareService(said, logonResponse);

        LOG.message("cancelKidscare");
        return getResultByRequestPost(uri, data);

    }

    private static LogonResponse checkBuyPinByKidscare(String passwd) {
        return (LogonResponse) checkBuyPin(passwd, true);
    }

    // <?xml version="1.0" encoding="utf-8"?><string xmlns="http://tempuri.org/">returnVal^True|returnCode^HDSS001|returnDESC^SUCCESS</string>
    // <?xml version="1.0" encoding="utf-8"?><string xmlns="http://tempuri.org/">returnVal^True|returnDESC^SUCCESS</string>
    private static String getResultByRequestPost(String uri, String data) {
        ArrayList results = requestPost(uri, data);

        BasicResponse response = new BasicResponse(results);
        return getResult(response);
    }

    private static String getResult(BasicResponse response) {
        String resultCode = ErrorCode.C601;

        if (response.isResult()) {
            resultCode = ErrorCode.SUCCESS;
        } else if (response.isPinError()) {
            resultCode = ErrorCode.C502;
        }

        return resultCode;
    }

    private static String getTokens(String said) {
        String nonce = getNonce();
        String pin = ProxyManager.otherHandler().getInfo(com.kt.remotecontrol.util.KeyConstant.STB_PIN_NUMBER);
        String hdsToken = getHDSToken(said, pin, nonce);
        String stbToken = getSTBToken(said, pin, nonce);

        return "HDSToken=" + hdsToken + "&STBToken=" + stbToken;
    }

    private static String getNonce() {
        String nonce = "";

        for (int i = 0; i < 6; i++) {
            nonce += ((int) (Math.random() * 10));
        }

//        return nonce;
        return "557996";
    }

    // <?xml version=\"1.0\" encoding=\"utf-8\"?><string xmlns=\"http://tempuri.org/\">returnVal^True|HDSToken^214be4e66db89dea4e19b157f4c835ef0b18941a4c5378e49d8aa5827e2b6fc2725db841cd4b7719</string>
    public static String getHDSToken(String said, String pin, String nonce) {
        String uri = url + Constants.HDS_SERVICE_GETHDSTOKEN;
        String data = "SAID=" + said
                    + "&PinNo=" + pin
                    + "&NONCE=" + nonce;

        BasicResponse response = new HDSTokenResponse(requestPost(uri, data));
        return response.getResultMessage();
    }

    public static String getSTBToken(String said, String pin, String nonce) {
        String key = nonce + pin + "              "; // 24자리 맞춤
        String macAddress = ProxyManager.otherHandler().getMacAddress();
        String message = said + CharConstant.CHAR_VERTICAL + macAddress;

        HashMap hashMap = new HashMap();
        hashMap.put(KeyConstant.METHOD, MethodConstant.getSTBToken);
        hashMap.put(KeyConstant.KEY, key);
        hashMap.put(KeyConstant.MESSAGE, message);
        hashMap.put(KeyConstant.LINK, Constants.LINK_SSO);
        hashMap.put(KeyConstant.WAIT_TIMEOUT, Long.valueOf(TimeConstant.FIVE_SECONDS));

        HashMap response = ProxyManager.appHandler().execute(hashMap);

        return (String) response.get(KeyConstant.RESPONSE_MESSAGE);
    }

    private static ArrayList requestPost(String uri, String data) {
        HttpRequest request = new HttpRequest();
        return request.post(uri, data);
    }

    private static String getParamByKidscareService(String said, LogonResponse logonResponse) {
        return "SAID=" + said
                + "&ssovalidate=" + logonResponse.getPinAuthValidate()
                + "&BuyPinAuthKey=" + logonResponse.getBuyPinAuthKey()
                + "&regPath=" + REG_PATH_MY_OLLEH
                + "&procSubSVC=" + Constants.PRODUCT_KIDSCARE_CODE_BY_KT
                + "&sValue=";
   }
}
