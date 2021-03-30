/*
 * HttpTimeoutHandler.java 2008. 10. 21
 *
 * Copyright (C) 2003 Alticast Corporation. All Rights Reserved.
 *
 * This software is the confidential and proprietary information
 * of Alticast Corporation. You may not use or distribute
 * this software except in compliance with the terms and conditions
 * of any applicable license agreement in writing between Alticast
 * Corporation and you.
 */
package com.kt.remotecontrol.http;

import java.io.IOException;
import java.net.URL;
import java.net.URLStreamHandler;

public class HttpTimeoutHandler extends URLStreamHandler {
//        extends sun.net.www.protocol.http.Handler {
    private int iSoTimeout = 0;
    private int port = 0;

    public HttpTimeoutHandler(int iSoTimeout, int port) {
        this.iSoTimeout = iSoTimeout;
        this.port = port;
    }

    protected java.net.URLConnection openConnection(URL u) throws IOException {
//        return null;
        return new HttpTimeoutURLConnection(u, port, this, iSoTimeout);
    }

    protected String getProxy() {
        return null;
//        return proxy;
    }

    protected int getProxyPort() {
        return -1;
//        return proxyPort;
    }
}
