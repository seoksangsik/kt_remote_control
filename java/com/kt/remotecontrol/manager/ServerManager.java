package com.kt.remotecontrol.manager;

import com.kt.remotecontrol.http.NanoHTTPD;
import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.ErrorCode;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.service.Service;
import com.kt.remotecontrol.service.mouse.MouseService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

public class ServerManager {

    private static final Log LOG = new Log("ServerManager");

    /**
     * client 쪽 httpd 데몬
     */
    private NanoHTTPD httpServer = null;
    private MouseService mouseService = null;

    private HashMap services;

    public ServerManager(HashMap services) {
        this.services = services;
    }

    /**
     * 서비스가 초기화 되었는가.
     * 초기화는 서버소켓 데몬이 정상적으로 작동되었는가를 의미한다.
     */
    public boolean isStartedServer() {
        return httpServer != null;
    }

    public boolean isStartedTCPServer() {
        return mouseService != null;
    }

    /**
     * 데이타를 초기화 하고, 서버 소켓을 열어서 대기한다.
     * @return true or false
     */
    public void startHTTPServer() {

        if (isStartedServer()) {
            LOG.message("startHTTPServer, already Service started!");
            return ;
        }

        LOG.message("startHTTPServer, Start Command Service!");
        int port = Constants.SERVICE_PORT;

        try {
            httpServer = new NanoHTTPD(port, this);
            LOG.message("startHTTPServer, Success Command service... port(" + port + ")");
        } catch (IOException ioe) {
            LOG.message("startHTTPServer, Fail to start Command service " + port);
            ioe.printStackTrace();
            stopHTTPServer();
        }
    }

    /**
     * 1717 포트로 들어오는 해당 I/F 서버로 부터의 요청을 처리한다.
     * 여기에서 부터 각 명령에 해당하는 메소드로 분기를 한다.
     */
    public String processRequest(String uri, Properties params) {

        LOG.message("processRequest, uri = " + uri);

        if (uri.endsWith(Constants.NOTICE_URI)) { // 긴급 공지 테스트 루틴
            LOG.message("processRequest, NOTICE_URI : ignore *** ");
            return null;
        }

        if (uri.endsWith("favicon.ico")) { // favicon
            LOG.message("processRequest, favicon : ignore *** ");
            return null;
        }

        if (uri.endsWith(Constants.CALL_URI)) { // 인증 처리 먼저 한다
            return validURI(params);
        }

        // http://stbip:1717/ipstb/rtcontrol이 아닌 경우
        LOG.message("processRequest, Cannot Process URI : " + uri);

        return null;
    }

    public String execute(Properties parameters) {
        String cmd = parameters.getProperty(Constants.CMD);
        String code = null;

        try {
            code = cmd.substring(0, 3).toUpperCase();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (services.containsKey(code)) {
            Service service = (Service) services.get(code);
            String resultCode = service.execute(parameters);

            return getResult(cmd, resultCode);
        }

        return getResult(cmd, ErrorCode.INVALID_COMMAND);
    }

    public void startTCPServer() {

        if (isStartedTCPServer()) {
            LOG.message("startTCPServer, already Mouse server started!");
            return ;
        }

        LOG.message("startTCPServer, Start Mouse Service!");

        int port = Constants.TCP_SERVICE_PORT;
        try {
            mouseService = new MouseService(port);
            LOG.message("startTCPServer, Success start Mouse Server... port(" + port + ")");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            stopTCPServer();
        }
    }

    public void stopServer() {
        stopHTTPServer();
        stopTCPServer();
    }

    // HTTP 서버 데몬을 죽이고 제어서버와 동기를 해제한다.
    private void stopHTTPServer() {
        LOG.message("stopHTTPServer");

        StatusManager.getInstance().clearSync();
        if (httpServer != null) {
            httpServer.destory(); // 리슨 쓰레드 닫고, 서버 소켓을 닫는다.
        }
        httpServer = null;
    }

    private void stopTCPServer() {
        LOG.message("stopTCPServer");

        if (!isStartedServer()) {
            return;
        }

        mouseService.destroy();
        mouseService = null;
    }

    private String validURI(Properties params) {
        String cmd = params.getProperty(Constants.CMD);
        String filterResult = filterCondition(cmd, params);

        if (filterResult != null) {
            return filterResult;
        }

        return execute(params);
    }

    private String filterCondition(String cmd, Properties params) {

        if (cmd == null) {
            LOG.message("filterCondition, CMD is null");
            return getResult("NULL", ErrorCode.C108);
        }

        String serviceCode = (String) params.get(Constants.SVC_CD);

        if (Constants.SSO1003.equals(cmd) || Constants.SSO1004.equals(cmd)) { // KTKIDSCARE-102
            LOG.message("filterCondition, joinKidscare(SSO1003) or cancelKidscare(SSO1004)");
            return null;
        } else if (Constants.KIDSCARE.equalsIgnoreCase(serviceCode) && !ProxyManager.otherHandler().isSubscriber()) { // 서비스코드가 KIDSCARE 이지만, 가입자가 아니면
            return getResult(cmd, ErrorCode.C415);
        }

        String SAID = params.getProperty(Constants.SAID);
        if (!ProxyManager.otherHandler().isValidSAID(SAID)) {
            return getResult(cmd, ErrorCode.C508);
        }

        return null;
    }

    private String getResult(String cmd, String resultCode) {
        StringBuffer data = new StringBuffer();
        data.append(cmd).append(CharConstant.CHAR_CARET);
        data.append(resultCode);

        return data.toString();
    }
}
