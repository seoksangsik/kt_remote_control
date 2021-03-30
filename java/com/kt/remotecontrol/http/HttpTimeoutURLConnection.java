/*
 * HttpTimeoutURLConnection.java 2008. 10. 21
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
import java.net.HttpURLConnection;
import java.net.URL;

import sun.net.www.http.HttpClient;

public class HttpTimeoutURLConnection extends HttpURLConnection {

    private int port;

    public HttpTimeoutURLConnection(URL url, int port, HttpTimeoutHandler handler, int soTimeout) throws IOException {
        super(url);
//        super(u, handler);

        this.port = port;

        HttpTimeoutClient.setSoTimeout(soTimeout);
        HttpTimeoutClient.setPort(port);
    }

    public void connect() throws IOException {
        if (connected) {
            return;
        }

        try {
            if ("http".equals(url.getProtocol())) // && !failedOnce <- PRIVATE
            {
                // for safety's sake, as reported by KLGroup
                synchronized (url) {
//                    http = HttpTimeoutClient.getNew(url, port);
                }
            } else {
                /*
                if (handler instanceof HttpTimeoutHandler) {
                    http = new HttpTimeoutClient(super.url,
                            port,
                            ((HttpTimeoutHandler) handler).getProxy(),
                            ((HttpTimeoutHandler) handler).getProxyPort());
                } else {
                    throw new IOException("HttpTimeoutHandler expected");
                }
                 */
            }

//            ps = (PrintStream) http.getOutputStream();
            url.openConnection();
       } catch (IOException e) {
            throw e;
        }

        connected = true;
    }

    protected HttpClient getNewClient(URL url) throws IOException {
        HttpTimeoutClient httpTimeoutClient = new HttpTimeoutClient(url, port, (String) null,-1);
//        return httpTimeoutClient;
        return null;
    }

    protected HttpClient getProxiedClient(URL url, String s, int i) throws IOException {
//        return new HttpTimeoutClient(url, port, s, i);

        return null;
    }

    public void disconnect() {

    }

    public boolean usingProxy() {
        return false;
    }
}
