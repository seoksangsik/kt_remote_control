package com.kt.remotecontrol.manager;

import com.alticast.main.Config;
import com.kt.remotecontrol.http.URLConnectionProxy;
import com.kt.remotecontrol.http.dalvik.URLConnectionByDalvikVM;
import com.kt.remotecontrol.http.jvm.URLConnectionByJVM;
import com.kt.remotecontrol.interlock.mw.EventGenerator;
import com.kt.remotecontrol.interlock.nav.web.AndroidControl;
import com.kt.remotecontrol.interlock.nav.web.KeyControl;
import com.kt.remotecontrol.interlock.spec.NullableSIRetrieval;
import com.kt.remotecontrol.interlock.spec.SIRetrieval;
import com.kt.remotecontrol.interlock.spec.ServiceSIRetrieval;
import com.kt.remotecontrol.WorkingConfig;
import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.interlock.nav.KeyHandler;
import com.kt.remotecontrol.interlock.nav.NavHandlers;
import com.kt.remotecontrol.interlock.nav.Navigator;
import com.kt.remotecontrol.interlock.nav.web.WebControl;
import com.kt.remotecontrol.service.Service;
import com.kt.remotecontrol.service.command.*;

import java.util.HashMap;

public class ConfigManager {

    public static void init() {

        if (Config.AC_CF_REMOTE_CONTROL_AI_SPEAKER) {
            WorkingConfig.VERSION = "1.5.002_20200204";
        } else {
            WorkingConfig.VERSION = "2.1.001_20210329";
        }

        initNavigator();
        newSIRetrieval();
        newURLConnection();

        new PropertiesManager();
    }

    public static HashMap createServices() {
        Service queryService = null;
        Service controlService = null;

        System.out.println("constructor " + Config.AC_CF_REMOTE_CONTROL_AI_SPEAKER);

        if (Config.AC_CF_REMOTE_CONTROL_AI_SPEAKER) {
            queryService = new AISpeakerQueryService();
            controlService = new AISpeakerControlService();
        } else {
            queryService = new QueryService();
            controlService = new ControlService();
        }

        HashMap services = new HashMap();
        services.put("QRY", queryService);
        services.put("CTL", controlService);
        services.put("CFG", new ConfigService());
        services.put("SSO", new SSOService());
        services.put("ETC", new EtcService());

        // TODO: 설정에 따라 동작되도록
        services.put("ENV", new EnvService());

        return services;
    }

    private static void initNavigator() {
        KeyHandler keyHandler;

        if (Config.AC_CF_NAV_ANDROID) {
            keyHandler = new AndroidControl();
        } else {
            keyHandler = new KeyControl();
        }

        NavHandlers navHandlers = new WebControl(keyHandler);
        ProxyManager.setNavigator(new Navigator(navHandlers));
        ProxyManager.setEventGenerator(new EventGenerator());
    }

    private static void newSIRetrieval() {
        SIRetrieval siRetrieval;
        if (Config.AC_CF_NS_CAK) {
            siRetrieval = new NullableSIRetrieval();
        } else {
            siRetrieval = new ServiceSIRetrieval();
        }
        ProxyManager.setSIRetrieval(siRetrieval);
    }

    private static void newURLConnection() {
        URLConnectionProxy urlConnectionProxy;
        if (Config.AL_CF_JVM == Config.AL_JVM_DALVIK) {
            urlConnectionProxy = new URLConnectionByDalvikVM();
        } else {
            urlConnectionProxy = new URLConnectionByJVM();
        }

        ProxyManager.setURLConnection(urlConnectionProxy);
    }
}
