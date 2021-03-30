/*
 * HttpTimeoutClient.java 2008. 10. 21
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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;

//import sun.net.www.http.HttpClient;

public class HttpTimeoutClient { //extends HttpClient {
    private static int iSoTimeout = 0;
    private static int port = 0;

    public HttpTimeoutClient(URL url, int port, String proxy, int proxyPort)
            throws IOException {
//        super(url, proxy, proxyPort);
        this.port = port;
    }

    public HttpTimeoutClient(URL url, int port) throws IOException {
        this(url, port, null, -1);
    }

    public static HttpTimeoutClient getNew(URL url, int port) throws IOException {
        HttpTimeoutClient httpTimeoutClient = null;
//        HttpTimeoutClient httpTimeoutClient = (HttpTimeoutClient) kac.get(url);

        if (httpTimeoutClient == null) {
            return new HttpTimeoutClient(url, port); // CTOR called openServer()
        }
//            httpTimeoutClient.url = url;


        return httpTimeoutClient;
    }

    public static void setSoTimeout(int iNewSoTimeout) {
        iSoTimeout = iNewSoTimeout;
    }

    public static void setPort(int newPort) {
        port = newPort;
    }

    public static int getSoTimeout() {
        return iSoTimeout;
    }

    // Override doConnect in NetworkClient

    protected Socket doConnect(String s, int i)
            throws IOException, UnknownHostException, SocketException {

       Socket socket = new Socket();
        socket.connect(new InetSocketAddress(s, port), iSoTimeout);

        // This is the important bit
        socket.setSoTimeout(iSoTimeout);
        return socket;
    }

}
