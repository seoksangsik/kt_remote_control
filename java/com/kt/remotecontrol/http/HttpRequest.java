package com.kt.remotecontrol.http;

import com.kt.remotecontrol.WorkingConfig;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpRequest {

    private static final Log LOG = new Log("HttpRequest");

    private static final int REQUEST_TIME_OUT = 10000;
    public final String PROTOCOL_HTTP = "http";
    public static final String PROTOCOL_HTTPS = "https";

    public static ArrayList post(String spec, String data) {
        LOG.message("post, data=[" + data + "]");

        ArrayList result = null;
        BufferedReader br = null;
        OutputStreamWriter osw = null;

        com.kt.remotecontrol.http.URLConnectionProxy urlConnectionProxy = com.kt.remotecontrol.interlock.ProxyManager.urlConnection();

        try {
            HttpURLConnection.setFollowRedirects(false); // the protocol will not automatically follow redirects.
            HttpURLConnection conn = null;

            URL url = new URL(spec);
            if (PROTOCOL_HTTPS.equals(url.getProtocol())) {
                conn = openSSLConnection(url);
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.connect();

            osw = createAndWriteAtOutStream(conn.getOutputStream(), data);

            // Get the response
            br = getBufferedReader(conn.getInputStream());
            result = readResponseData(br);

            LOG.message("post, ======> RECEIVE Success~~~");
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("HTTP Error Address : '" + spec + "'");
//            Log.error("HTTP Error Time : " + Util.convertDate(new Date()));
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (osw != null) {
                try {
                    osw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    public static ArrayList get(String spec) {
        return get(spec, -1);
    }

    public static ArrayList get(String spec, int port) {

        BufferedReader bufferedReader = null;
//        URLConnectionProxy urlConnectionProxy = ProxyManager.urlConnection();

        URL url = null;

        try {
            HttpURLConnection.setFollowRedirects(false);

            url = new URL(spec);
            URLConnection conn;

            if (PROTOCOL_HTTPS.equals(url.getProtocol())) {
                conn = openSSLConnection(url);
            } else {
                conn = (HttpURLConnection) url.openConnection();
//                conn = (HttpsURLConnection) urlConnectionProxy.openConnection(spec, REQUEST_TIME_OUT, port);
            }

            conn.setConnectTimeout(1000);
            conn.setReadTimeout(5000);
            // Get the response
            bufferedReader = getBufferedReader(conn.getInputStream());

            ArrayList result = readResponseData(bufferedReader);

            bufferedReader.close();

            return result;
        } catch (Exception e) {
            e.printStackTrace();
//            printHttpError(WorkingConfig.REQUEST_SERVER, urlConnectionProxy.getURL());
            printHttpError(WorkingConfig.REQUEST_SERVER, url == null ? null : url.toString());
            return null;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static OutputStreamWriter createAndWriteAtOutStream(OutputStream outputStream, String data) throws IOException {
        if (data == null) {
            return null;
        }
        OutputStreamWriter osw = new OutputStreamWriter(outputStream, "EUC-KR");
        osw.write(data); // post
        osw.flush();

        LOG.message("post, ======> SEND Success~~~");

        return osw;
    }

    private static BufferedReader getBufferedReader(InputStream inputStream) throws UnsupportedEncodingException {
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
        return new BufferedReader(inputStreamReader);
    }

    private static HttpURLConnection openSSLConnection(URL url) {
        HttpURLConnection conn = null;

        try {
            SSLContext sc = SSLContext.getInstance("SSL"); // TLS

            TrustManager[] trustAllCerts = { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                }
            } };
            sc.init(null, trustAllCerts, new SecureRandom());
            SSLSocketFactory socketFactory = sc.getSocketFactory();

            HttpsURLConnection.setDefaultSSLSocketFactory(socketFactory);
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
            conn = (HttpsURLConnection) url.openConnection();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return conn;
    }

    private static ArrayList readResponseData(BufferedReader br) throws IOException {
        ArrayList result = new ArrayList();
        String line;

        while ((line = br.readLine()) != null) {
            LOG.message("[HttpRequest] receive:" + line);
            result.add(line);
        }

        LOG.message("[HttpRequest] ======> RECEIVE Success~~~");

        return result;
    }

    private static void printHttpError(String server, String fullAddress) {
        LOG.error("[RemoteControlRequest] HTTP Error Server : " + Util.convertIpAddress(server));
        LOG.error("[RemoteControlRequest] HTTP Error Address : " + fullAddress);
        LOG.error("[RemoteControlRequest] HTTP Error Time : " + Util.convertDate(new Date()));
    }
}
