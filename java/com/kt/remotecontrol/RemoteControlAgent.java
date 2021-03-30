package com.kt.remotecontrol;

import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.manager.ConfigManager;
import com.kt.remotecontrol.manager.ServerManager;
import com.kt.remotecontrol.util.Log;

import java.util.HashMap;

/**
 * <code>RemoteControlAgent</code>
 *
 */
public class RemoteControlAgent {

    private static final Log LOG = new Log("RemoteControlAgent");

    /**
     * singleton instance
     */
    private static RemoteControlAgent instance = new RemoteControlAgent();

    private ServerManager serverManager;

    private RemoteControlAgent() {
        LOG.message("constructor");

        ConfigManager.init();
        LOG.message("VERSION : " + WorkingConfig.VERSION);
        LOG.message("LOG INCLUDE : " + WorkingConfig.LOG_INCLUDE);

        HashMap services = ConfigManager.createServices();
        serverManager = new ServerManager(services);

        ProxyManager.navigator().init();
    }

    public static RemoteControlAgent getInstance() {
        return instance;
    }

    public void startService() {
        messageWithDecorate("startService ~~~~~~~");

        serverManager.startHTTPServer(); // HTTP 데몬이 안 떠있으면 띄운다

        ProxyManager.stateHandler().toRunning();

        LOG.message("startService, HTTP serverStarted? : " + serverManager.isStartedServer());

        serverManager.startTCPServer();

        LOG.message("startService, TCP serverStarted");
    }

    public boolean isStartedServer() {
        return serverManager.isStartedServer();
    }

    public void stopService() {
        messageWithDecorate("stopService ~~~~~~~");

        try {
            ProxyManager.navigator().removeListener();

            serverManager.stopServer();

            messageWithDecorate("stopService, [ Stopped KIDSCARE Application ]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void messageWithDecorate(String message) {
        LOG.message("####################################");
        LOG.message(message);
        LOG.message("####################################");
    }
}
