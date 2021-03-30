package com.kt.remotecontrol.model;

import com.kt.remotecontrol.http.NanoHTTPD;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * HTTP response. Return one of these from serve().
 */
public class Response {
    private String debugText;
    /**
     * HTTP status code after processing, e.g. "200 OK", HTTP_OK
     */
    public String status;

    /**
     * MIME type of content, e.g. "text/html"
     */
    public String mimeType;

    /**
     * Data of the response, may be null.
     */
    public InputStream data;

    /**
     * Headers for the HTTP response. Use addHeader() to add lines.
     */
    public Properties header = new Properties();

    /**
     * Default constructor: response = HTTP_OK, data = mime = 'null'
     */
    public Response() {
        this.status = NanoHTTPD.HTTP_OK;
    }

    /**
     * Default constructor: response = HTTP_OK, data = mime = 'null'
     */
    public Response(String status, String mimeType) {
        this.status = status;
        this.mimeType = mimeType;
    }

    /**
     * Basic constructor.
     */
    public Response(String status, String mimeType, InputStream data) {
        this.status = status;
        this.mimeType = mimeType;
        this.data = data;
    }

    /**
     * Convenience method that makes an InputStream out of given text.
     */
    public Response(String status, String mimeType, String txt) {
        this.status = status;
        this.mimeType = mimeType;
        this.data = new ByteArrayInputStream(txt.getBytes());
    }

    /**
     * Adds given line to the header.
     */
    public void addHeader(String name, String value) {
        header.put(name, value);
    }


    /**
     *
     * @param txt
     */
    public void setData(String txt) {
        debugText = txt;
        this.data = new ByteArrayInputStream(txt.getBytes());
    }

    public String getData() {
        return debugText;
    }
}