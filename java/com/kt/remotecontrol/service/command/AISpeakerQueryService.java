package com.kt.remotecontrol.service.command;

import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.ErrorCode;
import com.kt.remotecontrol.util.KeyConstant;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.util.MethodConstant;
import com.kt.remotecontrol.util.TimeConstant;
import com.kt.remotecontrol.interlock.ProxyManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class AISpeakerQueryService extends QueryService {

    private static final Log LOG = new Log("AISpeakerQueryService");

    private ArrayList myCommand;

    public AISpeakerQueryService() {
        super();

        myCommand = new ArrayList();

        putCommand("QRY1016", "getCurrentTVState");
    }

    public String execute(Properties params) {
        String cmd = params.getProperty(Constants.CMD);

        if (!publishCommand.containsKey(cmd)) {
            return ErrorCode.INVALID_COMMAND;
        }

        String methodName = (String) publishCommand.get(cmd);

        if (myCommand.contains(cmd)) {
            return execute(this, methodName);
        }

        return executeSuperClass(this, methodName, params);
    }

    protected String getCurrentTVState() {

        String uiStatus = sendTalkToApp("getHomeState");
        String tvState = com.kt.remotecontrol.interlock.ProxyManager.navigator().getCurrentTVState(uiStatus);

        return getSUCCESS(tvState).toString();
    }

    private void putCommand(String cmd, String method) {
        publishCommand.put(cmd, method);
        myCommand.add(cmd);
    }

    private String sendTalkToApp(String target) {
        HashMap request = new HashMap();
        request.put(KeyConstant.METHOD, MethodConstant.talkToApp);
        request.put(KeyConstant.TARGET, target);
        request.put(KeyConstant.WAIT_TIMEOUT, Long.valueOf(TimeConstant.FIVE_SECONDS));

        Map response = ProxyManager.appHandler().execute(request);

        if (response == null || !response.containsKey(KeyConstant.RESULT_CODE)) {
            LOG.message("sendTalkToApp(" + target + "), invalid response");
            return null;
        }

        return (String) response.get(KeyConstant.RESULT_CODE);
    }
}
