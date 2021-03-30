package com.kt.remotecontrol.service.command;

import com.kt.remotecontrol.service.Service;
import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.ErrorCode;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.interlock.app.Kidscare;

import java.util.Properties;

public class SSOService extends CommandService implements Service {

    private static final Log LOG = new Log("SSOService");

    public SSOService() {
        super();

        publishCommand.put("SSO1001", "getProductCode");
        publishCommand.put("SSO1002", "checkAdultPIN");
        publishCommand.put("SSO1003", "joinKidscare");
        publishCommand.put("SSO1004", "cancelKidscare");
    }

    public String execute(Properties params) {
        String cmd = params.getProperty(Constants.CMD);

        if (!publishCommand.containsKey(cmd)) {
            return ErrorCode.INVALID_COMMAND;
        }

        String methodName = (String) publishCommand.get(cmd);
        return execute(this, methodName, params);
    }

    // [신규] 가입 상품 정보 조회(SSO1001)
    protected String getProductCode(Properties params) {
        LOG.message("getProductCode(SSO1001)");

        StringBuffer data = getSUCCESS();
        data.append("1").append(CharConstant.CHAR_CARET); // ICOD
        data.append("2").append(CharConstant.CHAR_CARET); // 상품코드
        data.append(ProxyManager.otherHandler().getProductCode()).append(CharConstant.CHAR_CARET);
        data.append(ProxyManager.otherHandler().getBouquetID());

        return data.toString();
    }

    /**
     * 성인 PIN 인증(SSO1002, User.CertifyAdult)
     * @param params
     */
    protected String checkAdultPIN(Properties params) {
        LOG.message("checkAdultPIN(SSO1002)");

        String pin = params.getProperty(Constants.PIN_NO);
        if (pin == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        try {
            boolean result = ProxyManager.otherHandler().checkAdultPIN(pin);
            return result ? ErrorCode.SUCCESS : ErrorCode.C502;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ErrorCode.C502;
        }
    }

    /**
     * 키즈 모바일 가입(SSO1003)
     * @param params CMD, SAID, SVC_CD, PIN_NO, HP_NO
     */
    protected String joinKidscare(Properties params) {
        LOG.message("joinKidscare(SSO1003)");

        String passwd = params.getProperty(Constants.PIN_NO);
        String cellPhone = params.getProperty(Constants.HP_NO);

        if (passwd == null || cellPhone == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        String said = params.getProperty(Constants.SAID);
        return Kidscare.joinKidscare(said, passwd, cellPhone);
    }

    /**
     * 키즈 모바일 해지(SSO1004)
     * @param params CMD, SAID, SVC_CD, PIN_NO
     */
    protected String cancelKidscare(Properties params) {
        LOG.message("cancelKidscare(SSO1004)");

        String passwd = params.getProperty(Constants.PIN_NO);
        if (passwd == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        String said = params.getProperty(Constants.SAID);
        return Kidscare.cancelKidscare(said, passwd);
    }
}
