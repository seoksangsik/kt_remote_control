/*
 * HttpTimeoutFactory.java 2008. 10. 21
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

/**
 * @author Junit
 * 2008. 10. 21 ���� 4:08:40
 */
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class HttpTimeoutFactory implements URLStreamHandlerFactory {
    private int iSoTimeout = 0;
    private int port = 0;

    public HttpTimeoutFactory(int iSoTimeout) {
        this.iSoTimeout = iSoTimeout;
    }

    public URLStreamHandler createURLStreamHandler(String str) {
        return null;
//        return new HttpTimeoutHandler(iSoTimeout, port);
    }
}
