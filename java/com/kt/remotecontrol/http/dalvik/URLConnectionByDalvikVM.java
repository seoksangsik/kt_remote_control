package com.kt.remotecontrol.http.dalvik;

import com.kt.remotecontrol.http.URLConnectionProxy;
import com.kt.remotecontrol.util.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;

public class URLConnectionByDalvikVM implements URLConnectionProxy {

    private static final Log LOG = new Log("URLConnectionByDalvikVM");

    private URL url;

    public URLConnection openConnection(String spec, int timeout, int port) throws IOException {
        url = new URL(spec);
        URLConnection conn = url.openConnection();

        try {
            Method method = conn.getClass().getMethod("setReadTimeout", new Class[] { int.class });
            method.invoke(conn, new Object[] { new Integer(timeout) });

            method = conn.getClass().getMethod("setConnectTimeout", new Class[] { int.class });
            method.invoke(conn, new Object[] { new Integer(timeout) });
        } catch (Exception e) {
            e.printStackTrace();
            LOG.message("[URLConnection(Dalvik)] method can not set time-out");
        }

        return conn;
    }

    public String getURL() {
        return url == null ? null : url.toString();
    }
}