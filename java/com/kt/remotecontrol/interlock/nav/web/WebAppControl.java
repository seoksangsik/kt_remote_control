package com.kt.remotecontrol.interlock.nav.web;

import com.alticast.remotecontrol.RemoteControlEvent;
import com.alticast.rop.remotecontrol.NullRemoteControlEvent;
import com.kt.remotecontrol.interlock.nav.AppHandler;
import com.kt.remotecontrol.manager.StatusManager;
import com.kt.remotecontrol.util.Constants;
import com.kt.remotecontrol.util.KeyConstant;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.util.MethodConstant;
import com.kt.remotecontrol.util.TimeConstant;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class WebAppControl implements AppHandler  {
    private static final Log LOG = new Log("WebAppControl");

    private final String ORGANIZATION_ID = "4e30.";
    private Map resultMap = new HashMap();
    private Object lock = new Object();

    private int homeState = 0;
    private RemoteControlEvent event;

    public WebAppControl() {
        event = new NullRemoteControlEvent();
    }

    public void setRemoteControlEvent(RemoteControlEvent event) {
        LOG.message("setRemoteControlEvent, event=" + event);
        this.event = event;
    }

    public void talkToApp(HashMap params, String aid) {
        postMessageByEvent(params, aid);
    }

    public synchronized boolean sendKeyword(String keyword) {

        this.notifyAll();

        resultMap.put(Constants.CTL1004, Boolean.valueOf(false));
        sendKeywordByEvent(keyword);

        try {
            this.wait(TimeConstant.FOUR_SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return ((Boolean) resultMap.remove(Constants.CTL1004)).booleanValue();
    }

    public synchronized void notifyKeyword(boolean result) {
        LOG.message("notifyKeyword result=" + result);
        resultMap.put(Constants.CTL1004, Boolean.valueOf(result));

        this.notifyAll();
    }

    public synchronized String buyContents(Properties params) {
        String said = params.getProperty(Constants.SAID);
        String contentId = params.getProperty(Constants.CONTENTS_ID);
        String contentName = params.getProperty(Constants.CONTENTS_NAME);
        String price = params.getProperty(Constants.PRICE);
        Date now = new Date(System.currentTimeMillis());
        String buyDate = new SimpleDateFormat("yyyyMMddHHmmss").format(now);
        String categoryId = params.getProperty(Constants.CATEGORY_ID);

        HashMap hashMap = new HashMap();
        hashMap.put(KeyConstant.saId, said);
        hashMap.put(KeyConstant.contsId, contentId);
        hashMap.put(KeyConstant.contsName, contentName);
        hashMap.put(KeyConstant.buyingPrice, price);
        hashMap.put(KeyConstant.buyingDate, buyDate);
        hashMap.put(KeyConstant.catId, categoryId);
        hashMap.put(KeyConstant.LINK, Constants.LINK_AMOC);

        lock.notifyAll();

        String key = params.getProperty(Constants.CMD);
        resultMap.put(key, null);
        buyContentsByEvent(hashMap);

        try {
            lock.wait(TimeConstant.THREE_POINT_FIVE_SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return (String) resultMap.remove(key);
    }

    public synchronized void notifyRemotePurchase(String result) {
        LOG.message("notifyRemotePurchase(ETC1001) result=" + result);

        resultMap.put(Constants.ETC1001, result);

        this.notifyAll();
    }

    public HashMap execute(HashMap params) {

        final String method = (String) params.get(KeyConstant.METHOD);

        try {
            synchronized (lock) {
                LOG.message("execute, method=" + method);

                resultMap.put(method, new HashMap());

                long timeout = -1;
                if (params.containsKey(KeyConstant.WAIT_TIMEOUT)) {
                    timeout = ((Long) params.remove(KeyConstant.WAIT_TIMEOUT)).longValue();
                }

                LOG.message("execute, " + params);
                executeByEvent(params);

                if (timeout > 0) {
                    lock.wait(timeout);

                    LOG.message("execute(wait " + timeout + " done), target=" + method);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return (HashMap) resultMap.remove(method);
    }

    public void notifyExecuteResult(HashMap response) {
        synchronized (lock) {

            String method = (String) response.get(KeyConstant.METHOD);

            if (resultMap.containsKey(method)) {
                HashMap result = new HashMap();
                LOG.message("=======================");

                Set keySet = response.keySet();
                Iterator it = keySet.iterator();
                Object key, value;

                while (it.hasNext()) {
                    key = it.next();
                    value = response.get(key);

                    LOG.message("notifyExecuteResult, " + key + "=" + value);
                    result.put(key, value);
                }

                LOG.message("=======================");
                resultMap.put(method, result);
            } else if (MethodConstant.request.equals(method)) {
                LOG.message("notifyExecuteResult, request");

                StatusManager.getInstance().requestHttpPost(response);
            }

            lock.notifyAll();
        }
    }

    public int getHomeState() {
        return homeState;
    }

    public void setHomeState(int state) {
        this.homeState = state;
    }

    private void postMessageByEvent(HashMap args, String aid) {

        mapLog(args, "postMessage");

        try {
            event.postMessage(args, ORGANIZATION_ID + aid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendKeywordByEvent(String keyword) {
        LOG.message("sendKeyword, event=" + event);

        event.sendKeyword(keyword);
    }

    private void buyContentsByEvent(HashMap params) {
        LOG.message("buyContents, event=" + event);

        event.buyContents(params);
    }

    private void executeByEvent(HashMap request) {
        mapLog(request, "execute");

        event.execute(request);
    }

    private void mapLog(Map args, String callMethod) {
        if (!LOG.INCLUDE) {
            return;
        }

        LOG.message(callMethod + ", event=" + event);
        LOG.message("=======================");
        Set keySet = args.keySet();
        Iterator it = keySet.iterator();
        Object key, value;
        while (it.hasNext()) {
            key = it.next();
            value = args.get(key);

            LOG.message(key + "=" + value);
        }
        LOG.message("----------------");
    }
}
