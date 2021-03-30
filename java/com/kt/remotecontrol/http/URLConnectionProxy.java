package com.kt.remotecontrol.http;

import java.io.IOException;
import java.net.URLConnection;

public interface URLConnectionProxy {
    public URLConnection openConnection(String spec, int timeout, int port) throws IOException;
    public String getURL();
}
