package com.kt.remotecontrol.http.jvm;

import com.kt.remotecontrol.http.URLConnectionProxy;
import com.kt.remotecontrol.util.Log;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class URLConnectionByJVM implements URLConnectionProxy {

    private static final Log LOG = new Log("URLConnectionByJVM");

    private URL url;

    public URLConnection openConnection(String spec, int timeout, int port) throws IOException {
        Object handler = null;

        try {
            Class cls = Class.forName("com.kt.remotecontrol.http.HttpTimeoutHandler");
            Constructor cons = cls.getDeclaredConstructor(new Class[] { int.class, int.class });

            handler = cons.newInstance(new Object[] { new Integer(timeout), new Integer(port) });
        } catch (Exception e) {
            e.printStackTrace();
            LOG.message("[URLConnection(JVM)] constructor can not set time-out");
        }

        if (handler == null) {
            url = new URL(spec);
        } else {
            url = new URL(null, spec, (URLStreamHandler) handler);
        }

        return url.openConnection();
    }

    public String getURL() {
        return url == null ? null : url.toString();
    }
}
