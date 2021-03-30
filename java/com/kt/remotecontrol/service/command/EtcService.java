package com.kt.remotecontrol.service.command;

import com.kt.remotecontrol.interlock.server.amoc.AMOC;
import com.kt.remotecontrol.interlock.server.hds.BasicResponse;
import com.kt.remotecontrol.interlock.server.hds.HDS;
import com.kt.remotecontrol.service.Service;
import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.ErrorCode;
import com.kt.remotecontrol.util.KeyConstant;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.util.MethodConstant;
import com.kt.remotecontrol.util.TimeConstant;
import com.kt.remotecontrol.interlock.ProxyManager;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Properties;

public class EtcService extends CommandService implements Service {

    private static final Log LOG = new Log("EtcService");

    public EtcService() {
        super();

        publishCommand.put("ETC1001", "remotePurchase"); // 원격 구매, User.PurchaseSTB
        publishCommand.put("ETC1002", "showMessage"); // Popup 메세지(TV 쪽지 보내기), STBInfo.sendTVMessage
        publishCommand.put("ETC1004", "showImage"); // TV 사진 전송, STBInfo.sendTVImage
        publishCommand.put("ETC1005", "showVoiceMessage"); // Popup 메세지(TV 음성 쪽지 보내기), STBInfo.sendTVMessageAu
    }

    public String execute(Properties params) {
        String cmd = params.getProperty(Constants.CMD);

        if (!publishCommand.containsKey(cmd)) {
            return ErrorCode.INVALID_COMMAND;
        }

        String methodName = (String) publishCommand.get(cmd);
        return execute(this, methodName, params);
    }

    /**
     * 원격 구매(ETC1001, User.PurchaseSTB)
     * @param params
     */
    protected String remotePurchase(Properties params) {

        LOG.message("remotePurchase(ETC1001)");

        String contentType = params.getProperty(Constants.CONTENTS_TYPE);
        String contentId = params.getProperty(Constants.CONTENTS_ID);
        String contentName = params.getProperty(Constants.CONTENTS_NAME);
        String categoryId = params.getProperty(Constants.CATEGORY_ID);
        String price = params.getProperty(Constants.PRICE);
        String pinNo = params.getProperty(Constants.PIN_NO);

        if (contentType == null || contentId == null || contentName == null
                || categoryId == null || price == null || pinNo == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        BasicResponse response = HDS.checkBuyPin(pinNo);
        if (response.isPinError()) { // KTKIDSCARE-3
            LOG.message("remotePurchase, fail to checkBuyPIN");
            return ErrorCode.C502;
        }

        String resultCode = AMOC.buyContents(params);

        if (resultCode == null) {
            resultCode = ErrorCode.STB_ETC;
        }

        return resultCode;
    }

    /**
     * Popup 메세지 - TV 쪽지 보내기(ETC1002, STBInfo.sendTVMessage)
     * @param params (필수) CMD, SVC_CD, SAID, POPUP_MSG, HP_NO, MSG_TITLE, MSG_TIME
     *               (선택) MSG_TYPE, POPUP_RESP, POPUP_GOTITLE, POPUP_GOURLTYPE, POPUP_GOURL, HP_UUID
     */
    protected String showMessage(Properties params) {

        LOG.message("showMessage(ETC1002)");

        String message = params.getProperty(Constants.POPUP_MSG);
        String phoneNumber = params.getProperty(Constants.HP_NO);
        String title = params.getProperty(Constants.MSG_TITLE);
        String time = params.getProperty(Constants.MSG_TIME);

        if (message == null || phoneNumber == null || title == null || time == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        // 동작 모드가 아니면 에러
        if (ProxyManager.navigator().isNotRunningBySTB()) {
            LOG.message("responsePopup STB is not RUNNING");
            return ErrorCode.C411;
        }

        HashMap ropParam = makePopupParam(params, "showMessage");
        ropParam.put(KeyConstant.MESSAGE, message);
        ropParam.put(KeyConstant.POPUP_TYPE, getMessageType(params));

        ProxyManager.appHandler().execute(ropParam);

        return ErrorCode.SUCCESS;
    }

    /**
     * TV 사진 전송(ETC1004, STBInfo.SendTVImage)
     * @param params (필수) CMD, SVC_CD, SAID, HP_NO, MSG_TIME, IMAGE_URL
     *                        (선택) POPUP_RESP, POPUP_GOTITLE, POPUP_GOURLTYPE, POPUP_GOURL, HP_UUID
     */
    protected String showImage(Properties params) {

        LOG.message("showImage(ETC1004)");

        String cellPhone = params.getProperty(Constants.HP_NO);
        String msgTime = params.getProperty(Constants.MSG_TIME);
        String imageURL = params.getProperty(Constants.IMAGE_URL);

        if (cellPhone == null || msgTime == null || imageURL == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        String imgFormat = params.getProperty(Constants.IMAGE_FMAT);
        if (invalidImageFormat(imgFormat)) {
            LOG.error("showImage, unsupported image format!");

            return getResultCodeAfterShowImageError(Constants.UNSUPPORT_FORMAT, params);
        }

        long imageSize = 0;
        try {
            Integer.parseInt(msgTime);
            imageSize = Long.parseLong(params.getProperty(Constants.IMAGE_SIZE));
        } catch (NumberFormatException e) {
            return ErrorCode.C107;
        }

        if (Constants.IMAGE_SIZE_6MB < imageSize) {
            LOG.error("showImage, unsupported image size!");
            return getResultCodeAfterShowImageError(Constants.TOO_MUCH_SIZE, params);
        }

        String encodeImageURL = null;
        try {
            encodeImageURL = URLDecoder.decode(imageURL, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return getResultCodeAfterShowImageError(Constants.OTHER_ERROR, params);
        } catch (Exception e) {
            e.printStackTrace();
            return getResultCodeAfterShowImageError(Constants.OTHER_ERROR, params);
        }

        HashMap ropParam = makePopupParam(params, "showImage");
        ropParam.put(KeyConstant.POPUP_TYPE, Constants.POPUP_TYPE_PICTURE);
        ropParam.put(KeyConstant.IMAGE_URL, encodeImageURL);
        ropParam.put(KeyConstant.WAIT_TIMEOUT, Long.valueOf(TimeConstant.FIVE_SECONDS));

        return getResultCodeByExecute(ropParam, "showImage");
    }

    /**
     *  Popup 메세지 - TV 음성 쪽지 보내기(ETC1005, STBInfo.sendTVMessageAu)
     * @param params (필수) CMD, SVC_CD, SAID, AUDIO_MSG, MSGUP_YN
     *               (선택) HP_NO, MSG_TITLE, MSG_TIME MSG_TYPE, POPUP_RESP,
     *                POPUP_GOTITLE, POPUP_GOURLTYPE, POPUP_GOURL, HP_UUID
     */
    protected String showVoiceMessage(Properties params) {

        LOG.message("showVoiceMessage(ETC1005)");

        String audioMessage = params.getProperty(Constants.AUDIO_MSG);
        if (audioMessage == null) {
            return ErrorCode.MISSING_REQUIRED_PARAMETER;
        }

        String messagePopup = params.getProperty(Constants.MSGUP_YN);
        HashMap ropParam = new HashMap();

        if (Constants.MSGUP_YN_BOTH.equals(messagePopup)) {
            ropParam = makePopupParam(params, "showVoiceMessage");

            ropParam.put(KeyConstant.POPUP_TYPE, getMessageType(params));
            ropParam.put(KeyConstant.ARGS, MethodConstant.showPopup);
        }

        ropParam.put(KeyConstant.METHOD, MethodConstant.showVoicePopup);
        ropParam.put(KeyConstant.MESSAGE, audioMessage);
        ropParam.put(KeyConstant.WAIT_TIMEOUT, Long.valueOf(TimeConstant.FIVE_SECONDS));

        return getResultCodeByExecute(ropParam, "showVoiceMessage");
    }

    private HashMap makePopupParam(Properties params, String callMethod) {

        LOG.error("makePopupParam(" + callMethod + ")");

        HashMap ropParam = new HashMap();
        ropParam.put(KeyConstant.METHOD, MethodConstant.showPopup);
        ropParam.put(KeyConstant.CELLPHONE, params.getProperty(Constants.HP_NO));
        ropParam.put(KeyConstant.DISPLAY_TIME, params.getProperty(Constants.MSG_TIME));

        addReplyParam(ropParam, params, callMethod);
        addShortcutParam(ropParam, params, callMethod);

        return ropParam;
    }

    private void addReplyParam(HashMap ropParam, Properties params, String callMethod) {
        String responseType = params.getProperty(Constants.POPUP_RESP);
        String replyType = null;

        if (Constants.POPUP_RESP_TEXT.equals(responseType)) {
            replyType= "text";
        } else if (Constants.POPUP_RESP_EMOTICON.equals(responseType)) {
            replyType = "emoticon";
        }

        if (replyType == null) {
            return ;
        }

        ropParam.put(KeyConstant.REPLY, replyType);
        ropParam.put(KeyConstant.SERVICE_CODE, params.getProperty(Constants.SVC_CD));
        ropParam.put(KeyConstant.UUID, params.getProperty(Constants.HP_UUID));
        ropParam.put(KeyConstant.SAID, params.getProperty(Constants.SAID));

        LOG.error("addReplyParam(" + callMethod + "), replyType=" + replyType);
    }

    private void addShortcutParam(HashMap ropParam, Properties params, String callMethod) {
        String shortcutName = params.getProperty(Constants.POPUP_GOTITLE);
        String shortcutType = params.getProperty(Constants.POPUP_GOURLTYPE);
        String shortcutURL = params.getProperty(Constants.POPUP_GOURL);

        if (shortcutName == null || shortcutType == null || shortcutURL == null) {
            return ;
        }

        int index = shortcutURL.indexOf(CharConstant.CHAR_SEMICOLON);
        if (index >= 0) {
            shortcutURL = shortcutURL.substring(index + 1); // KTKIDSCARE-86
        }

        boolean validShortcutType = Constants.POPUP_GOURL_TYPE_BOUND.equals(shortcutType)
                                    || Constants.POPUP_GOURL_TYPE_UNBOUND.equals(shortcutType)
                                    || Constants.POPUP_GOURL_TYPE_CHILDAPP.equals(shortcutType);

        if (!validShortcutType) {
            LOG.error("addShortcutParam(" + callMethod + "), invalid shortcutType=" + shortcutType);
            return ;
        }

        ropParam.put(KeyConstant.SHOTCUT_NAME, shortcutName);
        ropParam.put(KeyConstant.SHOTCUT_TYPE, shortcutType);
        ropParam.put(KeyConstant.SHOTCUT_URL, shortcutURL);
        ropParam.put(KeyConstant.SHORTCUT_APPID, params.getProperty(Constants.POPUP_GOAppID)); // KTKIDSCARE-150

        LOG.message("addShortcutParam(" + callMethod + "), shortcutName=" + shortcutName
                    + ", shortcutType=" + shortcutType + ", shortcutURL=" + shortcutURL);
    }

    private String getMessageType(Properties params) {
        String messageType = params.getProperty(Constants.MSG_TYPE);
        return "talk".equals(messageType) ? Constants.POPUP_TYPE_TALK : Constants.POPUP_TYPE_NOTICE;
    }

    private String getResultCodeAfterShowImageError(int errorType, Properties params) {

        LOG.message("showImageError, errorType=" + errorType);

        HashMap ropParam = new HashMap();
        ropParam.put(KeyConstant.METHOD, MethodConstant.showPopup);
        ropParam.put(KeyConstant.POPUP_TYPE, Constants.POPUP_TYPE_TALK);
        ropParam.put(KeyConstant.CELLPHONE, params.getProperty(Constants.HP_NO));
        ropParam.put(KeyConstant.DISPLAY_TIME, params.getProperty(Constants.MSG_TIME));
        ropParam.put(KeyConstant.ERROR_TYPE, String.valueOf(errorType));

        ProxyManager.appHandler().execute(ropParam);

        return getImageErrorCode(errorType);
    }

    private String getImageErrorCode(int errorType) {
        String errorCode = "";

        switch (errorType) {
            case Constants.TOO_MUCH_SIZE:
                errorCode = ErrorCode.C419;
                break;
            case Constants.UNSUPPORT_FORMAT:
                errorCode = ErrorCode.C420;
                break;
            case Constants.UNSUPPORT_MODEL:
                errorCode = ErrorCode.C421;
                break;
            case Constants.OTHER_ERROR:
            default:
                errorCode = ErrorCode.C422;
                break;
        }

        return errorCode;
    }

    private boolean invalidImageFormat(String imgFormat) {
        boolean isValidFormat = Constants.IMAGE_FMAT_JPG.equalsIgnoreCase(imgFormat)
                || Constants.IMAGE_FMAT_JPEG.equalsIgnoreCase(imgFormat)
                || Constants.IMAGE_FMAT_PNG.equalsIgnoreCase(imgFormat);

        return !isValidFormat;
    }

    private String getResultCodeByExecute(HashMap ropParam, String caller) {
        HashMap responseMap = ProxyManager.appHandler().execute(ropParam);
        return getResultCodeByCallback(responseMap, caller);
    }

    private String getResultCodeByCallback(HashMap responseMap, String caller) {
        String resultCode = null;

        if (responseMap == null) {
            LOG.message("getResultCodeByCallback(" + caller + "), null in the Result Map");
            resultCode = ErrorCode.STB_ETC;
        } else if (responseMap.isEmpty()) {
            LOG.message("getResultCodeByCallback(" + caller + "), empty in the Result Map");
            resultCode = ErrorCode.UNDEFINED;
        } else {
            resultCode = (String) responseMap.get(KeyConstant.RESULT_CODE);
        }

        LOG.message("getResultCodeByCallback(" + caller + "), resultCode=" + resultCode);

        return resultCode;
    }
}
