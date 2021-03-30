package com.kt.remotecontrol.manager;

import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.KeyConstant;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.WorkingConfig;
import com.kt.remotecontrol.interlock.ProxyManager;

import java.util.Properties;

public class PropertiesManager {

    private static final Log LOG = new Log("PropertiesManager");

    private Properties webmwProp;

    public PropertiesManager() {
        webmwProp = ProxyManager.otherHandler().getUSBProperties(Constants.PROPERTIES_NAME);

        if (webmwProp == null) {
            setConfigUnusingFile();
        } else {
            setConfigUsingFile();
        }

        if (WorkingConfig.isLive) {
            WorkingConfig.HDS_SERVER = Constants.HDS_LIVE_URI;
        } else {
            WorkingConfig.HDS_SERVER = Constants.HDS_TEST_URI;
        }
    }

    private void setConfigUnusingFile() {
        LOG.message(Constants.PROPERTIES_NAME + " is null!");

        WorkingConfig.CONTROL_SERVER = Constants.CONTROL_SERVER_IP;
        WorkingConfig.REQUEST_SERVER = Constants.REQUEST_SERVER_IP;
    }

    private void setConfigUsingFile() {
        setLiveState(KeyConstant.KIDSCARE_LIVE_SERVER);
        setLiveState(KeyConstant.KIDSCARE_LIVE);
        setCheckRequestServer(KeyConstant.KIDSCARE_CHECK_ADDRESS);
        setRequestServerIP(KeyConstant.KIDSCARE_IF_ADDRESS);
        setControlServerIP(KeyConstant.KIDSCARE_CONTROL_ADDRESS);
        setLocalServerIP(KeyConstant.KIDSCARE_LOCAL_ADDRESS);
    }

    private void setLiveState(String key) {
        if (notContainsKey(key)) {
            return ;
        }

        WorkingConfig.isLive = compareValue(key, "true");

        LOG.message(key + ", isLIVE=" + WorkingConfig.isLive);
    }

    private void setCheckRequestServer(String key) {
        if (notContainsKey(key)) {
            return;
        }

        WorkingConfig.CHECK_IFSERVER_REQUEST = compareValue(key, "true");

        LOG.message("CHECK_IFSERVER_REQUEST=" + WorkingConfig.CHECK_IFSERVER_REQUEST);
    }

    private boolean compareValue(String key, String expected) {
        String value = webmwProp.getProperty(key);
        return expected.equalsIgnoreCase(value);
    }

    private void setRequestServerIP(String key) {
        String address = Constants.TEST_SERVER_IP;

        if (containsKey(key)) {
            String propertiesAddress = webmwProp.getProperty(key);

            if (propertiesAddress != null) {
                address = propertiesAddress;

                LOG.message("I/F Server Address(properties)=" + propertiesAddress);
            }
        } else if (WorkingConfig.isLive) {
            address = Constants.REQUEST_SERVER_IP;
        }

        com.kt.remotecontrol.WorkingConfig.REQUEST_SERVER = address;

        LOG.message("I/F Server Address=" + WorkingConfig.REQUEST_SERVER);
    }

    private void setControlServerIP(String key) {
        String address = Constants.TEST_SERVER_IP;

        if (containsKey(key)) {
            String propertiesAddress = webmwProp.getProperty(key);

            if (propertiesAddress != null) {
                address = propertiesAddress;

                LOG.message("Control Server Address(properties)=" + propertiesAddress);
            }
        } else if (WorkingConfig.isLive) {
            address = Constants.CONTROL_SERVER_IP;
        }

        WorkingConfig.CONTROL_SERVER = address;

        LOG.message("Control Server Address=" + WorkingConfig.CONTROL_SERVER);
    }

    private void setLocalServerIP(String key) {
        if (!containsKey(key)) {
            return;
        }

        String propertiesAddress = webmwProp.getProperty(key);
        if (propertiesAddress != null) {
            WorkingConfig.LOCAL_SERVER = propertiesAddress;

            LOG.message("Control Server Address(properties)=" + propertiesAddress);
        }
    }

    private boolean containsKey(String key) {
        return webmwProp.containsKey(key);
    }

    private boolean notContainsKey(String key) {
        return !containsKey(key);
    }

}
