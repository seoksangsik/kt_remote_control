/*
 * HttpConnectionUtil.java 2009. 03. 07
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

import com.kt.remotecontrol.util.Log;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

public class HttpConnectionUtil {

    private static final Log LOG = new Log("HttpConnectionUtil");

    private int timeout;

    public HttpConnectionUtil(int timeout) {
        this.timeout = timeout;
    }

    /**
     * ���� ���� �������°�
     * @param strURL
     * @param port
     * @return
     */
    public String getResponse(String strURL, int port) {
        HttpURLConnection connection = (HttpURLConnection) getConnection(strURL, port);

        if (connection == null) {
            LOG.message("[HttpConnectionUtil] connection is failed.");
            return null;
        }

        BufferedReader bufferReader = null;
        String response = null;

        try {
            bufferReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            response = bufferReader.readLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if(bufferReader != null) {
                    bufferReader.close();
                }

                if(connection != null) {
                    connection.disconnect();
                }
            } catch (IOException ex) {
                ;
            }
        }

        return response;
    }

    public String getResponseAll(String strURL, int port) {
        HttpURLConnection connection = (HttpURLConnection) getConnection(strURL, port);

        if (connection == null) {
            LOG.message("[HttpConnectionUtil] connection is failed.");
            return null;
        }

        BufferedReader bufferReader = null;
        StringBuffer allResponse = new StringBuffer();

        try {
            bufferReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            
            while(true) {
                String str = bufferReader.readLine();
                if(str == null) {
                    break;
                }
                allResponse.append(str);
            }
            
        } catch (IOException ex) {
//            Log.error(ex);
        } finally {
            try {
                if(bufferReader != null) {
                    bufferReader.close();
                }

                if(connection != null) {
                    connection.disconnect();
                }
            } catch (IOException ex) {
                ;
            }
        }

        return allResponse.toString();
    }

    /**
     * Http �� Zip file �ȿ� �� �̹������� <�̸�, �̹���> �� ������ ���̺��� �����ش�.
     * @param strURL
     * @param port
     * @return
     */
    public Hashtable getResponseZip(String strURL, int port) {
        HttpURLConnection connection = (HttpURLConnection) getConnection(strURL, port);
        
        ByteArrayOutputStream bais = new ByteArrayOutputStream();
        
        BufferedInputStream bufferInput = null;

        if (connection == null) {
            LOG.message("[HttpConnectionUtil] connection is failed.");
            return null;
        }

        byte[] buf = new byte[4096];
        
        try {
            bufferInput = new BufferedInputStream(connection.getInputStream());
            while(true) {
                int read = bufferInput.read(buf);
                if(read == -1) {
                    break;
                }
                bais.write(buf, 0, read);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            try {
                if(bufferInput != null) {
                    bufferInput.close();
                }

                if(connection != null) {
                    connection.disconnect();
                }
            } catch (IOException ex) {
                ;
            }
        }

        LOG.message("[HttpConnectionUtil] getResponseZip, make hashtable");
        Hashtable/*<String, Image>*/ imageHash = new Hashtable();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bais.toByteArray()));
        
        try {
            ZipEntry ze = zis.getNextEntry();
            while(ze != null) {
                String name = ze.getName();
                long size = ze.getSize();
                
                LOG.message("[HttpConnectionUtil] getResponseZip, ze=" + name + ", size=" + size);

                byte[] imgbuf = new byte[(int)size];
                zis.read(imgbuf);
                
                Image image = toolkit.createImage(imgbuf);
                
                if(name != null && image != null) {
                    imageHash.put(name, image);
                }
                
                ze = zis.getNextEntry();
            }
            zis.close();
        } catch (ZipException e) {
            LOG.message("[HttpConnectionUtil] getResponseZip, zipException");
            e.printStackTrace();
        } catch (IOException e) {
            LOG.message("[HttpConnectionUtil] getResponseZip, IOException");
            e.printStackTrace();
        }

        LOG.message("[HttpConnectionUtil] getResponseZip, hashtable size =" + imageHash.size());

        return imageHash;
    }

    private URLConnection getConnection(String strURL, int port) {
        URLConnection urlConnection = null;
        URL url = null;

        try {
            LOG.message("[HttpConnectionUtil] connect, connection start");

            url = new URL(strURL);
//            url = new URL((URL) null, strURL, new HttpTimeoutHandler(timeout, port));
            urlConnection = url.openConnection();

            LOG.message("[HttpConnectionUtil] getConnection, connection end");

            return urlConnection;
        } catch(UnknownHostException ex) {
            LOG.error("[HttpConnectionUtil] can not found hostname, " + strURL);
        } catch(SocketTimeoutException ex) {
            LOG.error("[HttpConnectionUtil] connection time out.");
        } catch(IOException ex) {
            LOG.error("[HttpConnectionUtil] connection error.");
        }

        return null;
    }
}
