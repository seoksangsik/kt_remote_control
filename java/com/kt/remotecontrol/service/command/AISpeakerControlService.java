package com.kt.remotecontrol.service.command;

import com.kt.remotecontrol.service.Service;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.ErrorCode;
import com.kt.remotecontrol.util.KeyConstant;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.util.MethodConstant;
import com.kt.remotecontrol.util.TimeConstant;
import com.kt.remotecontrol.interlock.ProxyManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class AISpeakerControlService extends ControlService implements Service {

    private static final Log LOG = new Log("AISpeakerControlService");

    private ArrayList myCommand;

    public AISpeakerControlService() {
        super();

        myCommand = new ArrayList();

        putCommand("CTL1038", "searchByDialog");
        putCommand("CTL1039", "searchByHomePortal");
        putCommand("CTL1040", "launchSharpChannel");
        putCommand("CTL1041", "forwarding");
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

    protected String searchByDialog(Properties params) {

        String searchParameter = params.getProperty(Constants.SRCH_PARAM);

        if (searchParameter == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        try {
            JSONObject data = getDataObject(searchParameter);
            String keyword = data.getString("reqmsg");

            com.kt.remotecontrol.interlock.ProxyManager.otherHandler().sendKeywordToDialog(keyword);
        } catch (JSONException e) {
            return ErrorCode.INVALID_MESSAGE_FORMAT;
        }

        return ErrorCode.SUCCESS;
    }

    protected String searchByHomePortal(Properties params) {
        String searchParameter = params.getProperty(Constants.SRCH_PARAM);

        if (searchParameter == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        String message;

        try {
            JSONObject data = getDataObject(searchParameter);
            message = data.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return ErrorCode.INVALID_MESSAGE_FORMAT;
        }

        return sendTalkToApp("searchResult", message);
    }

    protected String launchSharpChannel(Properties params) {
        String sharpChannelID = params.getProperty(Constants.SHARPCH_ID);

        if (sharpChannelID == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        return sendTalkToApp("launchSharpLink", sharpChannelID);
    }

    protected String forwarding(Properties params) {
        String toApp = params.getProperty(Constants.TO_APP);
        String message = params.getProperty(Constants.MSG);

        if (toApp == null || message == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        try {
            new JSONObject(message);
        } catch (JSONException e) {
            e.printStackTrace();
            return ErrorCode.INVALID_MESSAGE_FORMAT;
        }

        return sendTalkToApp("forwarding", message, toApp);
    }

    private void putCommand(String cmd, String method) {
        publishCommand.put(cmd, method);
        myCommand.add(cmd);
    }

    private JSONObject getDataObject(String searchParameter) throws JSONException {
        JSONObject value = new JSONObject(searchParameter);

        return value.getJSONObject("data");
    }

    private String sendTalkToApp(String target, String message) {
        HashMap request = createReqeustByTalkToApp(target, message);

        return sendTalkToApp(target, request);
    }

    private String sendTalkToApp(String target, String message, String toApp) {
        HashMap request = createReqeustByTalkToApp(target, message);
        request.put(KeyConstant.APPID, toApp);

        return sendTalkToApp(target, request);
    }

    private HashMap createReqeustByTalkToApp(String target, String message) {
        HashMap request = new HashMap();
        request.put(KeyConstant.METHOD, MethodConstant.talkToApp);
        request.put(KeyConstant.TARGET, target);
        request.put(KeyConstant.MESSAGE, message);

        return  request;
    }

    private String sendTalkToApp(String target, HashMap request) {

        request.put(KeyConstant.WAIT_TIMEOUT, Long.valueOf(TimeConstant.FIVE_SECONDS));

        Map response = ProxyManager.appHandler().execute(request);

        if (response == null || !response.containsKey(KeyConstant.RESULT_CODE)) {
            LOG.message("sendTalkToApp(" + target + "), invalid response");
            return ErrorCode.C410;
        }

        return (String) response.get(KeyConstant.RESULT_CODE);
    }
}