/*
 *  HDSResponse.java
 *
 *  Copyright (c) 2015 Alticast Corp.
 *  All rights reserved. http://www.alticast.com/
 *
 *  This software is the confidential and proprietary information of
 *  Alticast Corp. ("Confidential Information"). You shall not
 *  disclose such Confidential Information and shall use it only in
 *  accordance with the terms of the license agreement you entered into
 *  with Alticast.
 */
package com.kt.remotecontrol.interlock.server.hds;

/**
 * <code>HDSResponse</code>
 * 
 * @author seoksangsik
 * @since 2015. 11. 20.
 */
public interface HDSResponse {

    /* (non-Javadoc)
     * @see com.kt.kidscare.hds.HDSResponse#isResult()
     */
    public boolean isResult();

    /* (non-Javadoc)
     * @see com.kt.kidscare.hds.HDSResponse#getResultMessage()
     */
    public String getResultMessage();
}
