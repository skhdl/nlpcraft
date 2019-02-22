/*
 * “Commons Clause” License, https://commonsclause.com/
 *
 * The Software is provided to you by the Licensor under the License,
 * as defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software:    NLPCraft
 * License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
 * Licensor:    Copyright (C) 2018 DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.server.endpoints;

import org.nlpcraft.server.mdo.NCQueryStateMdo;

import java.io.Serializable;

/**
 * Cache value bean.
 */
public class NCEndpointCacheValue implements Serializable  {
    private final NCQueryStateMdo state;
    private final String endpoint;
    private final long sendTime;
    private final int attempts;
    private final long createdOn;
    private final long userId;
    private final String srvReqId;
    
    public NCEndpointCacheValue(
        NCQueryStateMdo state,
        String endpoint,
        long sendTime,
        int attempts,
        long createdOn,
        long userId,
        String srvReqId
    ) {
        this.state = state;
        this.endpoint = endpoint;
        this.sendTime = sendTime;
        this.attempts = attempts;
        this.createdOn = createdOn;
        this.userId = userId;
        this.srvReqId = srvReqId;
    }
    
    public NCQueryStateMdo getState() {
        return state;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public long getSendTime() {
        return sendTime;
    }
    
    public int getAttempts() {
        return attempts;
    }
    
    public long getCreatedOn() {
        return createdOn;
    }
    
    public long getUserId() {
        return userId;
    }
    
    public String getSrvReqId() {
        return srvReqId;
    }
}
