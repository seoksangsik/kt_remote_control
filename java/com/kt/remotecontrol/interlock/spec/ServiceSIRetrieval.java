package com.kt.remotecontrol.interlock.spec;

import com.kt.navsuite.core.Channel;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.util.TimeConstant;

import org.dvb.si.SIEvent;
import org.dvb.si.SIInformation;
import org.dvb.si.SIIterator;
import org.dvb.si.SIRequest;
import org.dvb.si.SIRetrievalEvent;
import org.dvb.si.SIRetrievalListener;
import org.dvb.si.SIService;
import org.dvb.si.SISuccessfulRetrieveEvent;

import java.util.Date;

public class ServiceSIRetrieval implements SIRetrieval, SIRetrievalListener {
    private static final Log LOG = new Log("ServiceSIRetrieval");

    private Object SIEventRequest = new Object();
    private String retrieveProgramName = null;
    private SIRequest siRequest = null;

    public String getCurrentProgramName(Channel channel) {
        if (channel == null) {
            LOG.error("[ServiceSIRetrieval] getCurrentProgramName, channel is NULL!");
            return null;
        }

        String name = null;

        synchronized (SIEventRequest) {

            LOG.message("[ServiceSIRetrieval] getCurrentProgramName, channel=" + channel);

            SIService service = channel.getSIService();
            long time = System.currentTimeMillis() + 1000l;
            retrieveProgramName = null;

            try {
                siRequest = service.retrieveScheduledSIEvents(SIInformation.FROM_CACHE_OR_STREAM,
                        null, this,null, new Date(time), new Date(time + 500));

                LOG.message("[ServiceSIRetrieval] getCurrentProgramName, wait(1500)");

                SIEventRequest.wait(TimeConstant.ONE_POINT_FIVE_SECONDS);

                name = retrieveProgramName;

                LOG.message("[ServiceSIRetrieval] getCurrentProgramName, wait(1500) done! name='" + name + "'");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        cancelSIRequest();

        LOG.message("[ServiceSIRetrieval] getCurrentProgramName, name=" + name);

        return name;
    }

    private void cancelSIRequest() {
        synchronized (SIEventRequest) {
            if (siRequest == null) {
                return ;
            }

            LOG.message("[ServiceSIRetrieval] cancelSIRequest");

            siRequest.cancelRequest();
            siRequest = null;
        }
    }

    public void postRetrievalEvent(SIRetrievalEvent event) {

        LOG.message("[ServiceSIRetrieval] ### SIEvent postRetrievalEvent");

        synchronized (SIEventRequest) {
            if (event instanceof SISuccessfulRetrieveEvent) {
                SISuccessfulRetrieveEvent success = (SISuccessfulRetrieveEvent) event;

                SIIterator iterator = success.getResult();
                SIEvent[] results = new SIEvent[iterator.numberOfRemainingObjects()];
                if (results != null && results.length > 0) {
                    for (int i = 0; i < results.length; i++) {
                        results[i] = (SIEvent) iterator.nextElement();
                    }

                    retrieveProgramName = results[0].getName();

                    LOG.message("[ServiceSIRetrieval] SIEvent[0] getName='" + retrieveProgramName + "'");
                } else {
                    LOG.message("[ServiceSIRetrieval] no event : result null");
                }
            } else {
                LOG.message("[ServiceSIRetrieval] no event : " + event.getClass().getName());
            }

            LOG.message("[ServiceSIRetrieval] ### SIEvent postRetrievalEvent, notifyAll()");

            SIEventRequest.notifyAll();
        }
    }
}
