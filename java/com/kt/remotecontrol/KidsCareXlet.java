/*
 *  KidsCareXlet.java	$Revision: 1.8 $ $Date: 2014/04/03 08:54:06 $
 *
 *  Copyright (c) 2004 Alticast Corp.
 *  All rights reserved. http://www.alticast.com/
 *
 *  This software is the confidential and proprietary information of
 *  Alticast Corp. ("Confidential Information"). You shall not
 *  disclose such Confidential Information and shall use it only in
 *  accordance with the terms of the license agreement you entered into
 *  with Alticast.
 */
package com.kt.remotecontrol;

import com.kt.KidscareXlet;
import com.kt.navsuite.core.HostAppManager;
import com.kt.navsuite.service.ObserverAppManager;
import com.kt.remotecontrol.interlock.ProxyManager;
import com.kt.remotecontrol.util.Log;
import com.kt.service.UnboundApplicationID;

import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

/**
 * <code>KidsCareXlet</code>
 */
public class KidsCareXlet implements KidscareXlet {
    private static final Log LOG = new Log("KidsCareXlet");

    public final String RMI_APP_NAME = "KIDSCARE";

    public XletContext xletContext;

    public void initXlet(XletContext context) throws XletStateChangeException {
        xletContext = context;
        LOG.message("initXlet, XletContext: " + xletContext);
    }

    public void startXlet() throws XletStateChangeException {
        LOG.message("startXlet, XletContext : " + xletContext);

        try {
            RemoteControlAgent.getInstance().startService();

            startWebApp("nav_webapp/apps/kids/kids_ui.html");
        } catch (Exception re) {
            re.printStackTrace();
        }
    }

    private void startWebApp(final String relativeURL) {
        LOG.message("startWebApp, relative url=" + relativeURL);

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(10000);

                    HostAppManager.getInstance().startHostApp(HostAppManager.REMOTE_AGENT);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.start();
    }

    public void pauseXlet() {
        LOG.message("pauseXlet");
    }

    public void destroyXlet(boolean arg) throws XletStateChangeException {
        LOG.message("destroyXlet, arg=" + arg);
        RemoteControlAgent.getInstance().stopService();
    }

    public XletContext getXletContext() {
        return xletContext;
    }

    public int getAID() {
        return UnboundApplicationID.KIDS_CARE_AID;
    }

    public void notifyMessage(int from, String msg) {
        LOG.message("notifyMessage, from=" + from + " msg=[" + msg + "]");

        if (msg == null) {
            return;
        }

        if ("REMOCON_SUBSCRIBE".equalsIgnoreCase(msg)) { // UP ���� 2�� �ٲپ��ش�.
            subscribeRemoconService();
        } else if ("REMOCON_UNSUBSCRIBE".equalsIgnoreCase(msg)) { // 2 �� 0 �̳� 1�� �ٲپ��ش�.
            unsubscribeRemoconService();
        }
    }

    private void subscribeRemoconService() {
        LOG.message("subscribeRemoconService");
        setRemoconNotBoundApplication(true);
    }

    private void unsubscribeRemoconService() {
        LOG.message("unsubscribeRemoconService");
        setRemoconNotBoundApplication(false);

        if (ProxyManager.otherHandler().isSubscriber()) {
            return ;
        }
        // Ű���ɾ� �����ڰ� �ƴϸ� ������Ų��.
        ObserverAppManager.getInstance().stopUnboundApplication(UnboundApplicationID.ID_KIDSCARE, null);
    }

    private void setRemoconNotBoundApplication(boolean autoStart) {
        String aid = String.valueOf(UnboundApplicationID.KIDS_CARE_AID);
        setRemoconNotBoundApplication(aid, autoStart);
    }

    /**
     * Unbound App �� ���� ���θ� �����Ѵ�
     * <appID>:<[0|1]>;<appID>:<[0|1]> �������� �����Ѵ�.
     * 0 : ���ý� ����ȵ�
     * 1 : ���ý� ����� (Ű���ɾ� ������)
     * 2 : ���ý� ����� (������ ������)
     *
     * @param "16"���� aid (ex:3000:0)
     *
     */
    public void setRemoconNotBoundApplication(String aid, boolean autoStart) {
        LOG.message("setRemoconNotBoundApplication, aid=" + aid + ", autoStart=" + autoStart);
        LOG.message("setRemoconNotBoundApplication, webmw ignore this");
    }

    public String getVersion() {
        return WorkingConfig.VERSION;
    }
}
