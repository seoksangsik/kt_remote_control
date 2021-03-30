/*
 *  Log.java
 *
 *  Copyright (c) 2018 Alticast Corp.
 *  All rights reserved. http://www.alticast.com/
 *
 *  This software is the confidential and proprietary informatioon of
 *  Alticast Corp. ("Confidential Information"). You shall not
 *  disclose such Confidential Information and shall use it only in
 *  accordance with the terms of the license agreement you entered into
 *  with Alticast.
 */
package com.kt.remotecontrol.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * The class on which applications make logging calls.
 * A Logger object is used to log messages for a specific system or
 * application component.
 */
public class Log {
    /**
     * Whether the log information will be printed out.
     * Note that the ERROR level debugging information shall always be printed out
     * regardless of the value of this variable.
     */
    public final static boolean INCLUDE = com.alticast.debug.Log.isDebug;

    private final static boolean ERROR = com.alticast.debug.Log.EL_REMOTE_CONTROL_ERR;
    private final static boolean WARNING = com.alticast.debug.Log.EL_REMOTE_CONTROL_WARN;
    private final static boolean MESSAGE = com.alticast.debug.Log.EL_REMOTE_CONTROL_MSG;
    private final static boolean DEBUG = com.alticast.debug.Log.EL_REMOTE_CONTROL_DBG;

    private final static int MAX_MESSAGE_LENGTH = 1000;

    private final String header;

    public Log(String category) {
        header = "[" + category + "] ";
    }

    public Log(String category, String subcategory) {
        header = "[" + category + "/" + subcategory + "] ";
    }

    /**
     * Print out the 'ERROR' information (1000) when
     * <blockquote>
     * 1. The thread reaches the code thought as unreachable.<br>
     * 2. It makes the system/application crash<br>
     * 3. The information that are wanted to be printed out event in the release binary.
     * </blockquote>
     * @param msg the information to be printed out.
     */
    public void error(String msg) {
        if (INCLUDE) {
            if (ERROR) {
                msg = cuttingMessage(msg);

                com.alticast.debug.Log.println(com.alticast.debug.Log.EL_ERR,
                    com.alticast.debug.Log.ED_REMOTE_CONTROL, msg);
            }
        }
    }

    /**
     * Print out the 'MESSAGE' reasonably significant messages (800) that will make sense to QA.
     * @param msg the information to be printed out.
     */
    public void warning(String msg) {
        if (INCLUDE) {
            if (WARNING) {
                msg = cuttingMessage(msg);

                com.alticast.debug.Log.println(com.alticast.debug.Log.EL_WARN,
                    com.alticast.debug.Log.ED_REMOTE_CONTROL, msg);
            }
        }
    }

    /**
     * Print out the 'MESSAGE' reasonably significant messages (800) that will make sense to QA.
     * @param msg the information to be printed out.
     */
    public void message(String msg) {
        if (INCLUDE) {
            if (MESSAGE) {
                msg = cuttingMessage(msg);
                com.alticast.debug.Log.println(com.alticast.debug.Log.EL_MSG,
                    com.alticast.debug.Log.ED_REMOTE_CONTROL, msg);
           }
        }
    }

    /**
     * Print out the 'DEBUG' information (500) when the information that can be
     * a potensitial problem.
     * @param msg the information to be printed out.
     */
    public void debug(String msg) {
        if (INCLUDE) {
            if (DEBUG) {
                msg = cuttingMessage(msg);
                com.alticast.debug.Log.println(com.alticast.debug.Log.EL_DBG,
                    com.alticast.debug.Log.ED_REMOTE_CONTROL, msg);
            }
        }
    }

    /**
     * Print out the 'DEBUG' information (500) that will provide tracing information.
     *
     * @param throwable the information to be printed out.
     */
    public void debug(Throwable throwable) {
        debug(getStackTraceString(throwable));
    }

    /**
     * Print out the 'MESSAGE' reasonably significant messages (800) that will make sense to QA.
     * @param throwable the information to be printed out.
     */
    public void message(Throwable throwable) {
        message(getStackTraceString(throwable));
    }

    /**
     * Print out the 'WARNING' information (900) when the information that can be
     * a potensitial problem.
     * @param throwable the information to be printed out.
     */
    public void warning(Throwable throwable) {
        warning(getStackTraceString(throwable));
    }

    /**
     * Print out the 'ERROR' information (1000) when
     * <blockquote>
     * 1. The thread reaches the code thought as unreachable.<br>
     * 2. It makes the system/application crash<br>
     * 3. The information that are wanted to be printed out event in the release binary.
     * </blockquote>
     * @param throwable the information to be printed out.
     */
    public void error(Throwable throwable) {
        error(getStackTraceString(throwable));
    }

    private String cuttingMessage(String msg) {
        msg = header + msg;
        if (msg.length() > MAX_MESSAGE_LENGTH) {
            msg = msg.substring(0, MAX_MESSAGE_LENGTH - 1);
        }
        return msg;
    }

    private String getStackTraceString(Throwable tr) {

        if (tr == null) {
            return "";
        }

        StringWriter sw = null;

        try {
            sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            tr.printStackTrace(pw);
            return sw.toString();
        } catch (Exception e) {
            if (INCLUDE) {
                debug(e);
            }
        } finally {
            if (sw != null) {
                try {
                    sw.close();
                } catch (IOException e) {
                    if (INCLUDE) {
                        debug(e);
                    }
                }
            }
        }

        return "";
    }

}
