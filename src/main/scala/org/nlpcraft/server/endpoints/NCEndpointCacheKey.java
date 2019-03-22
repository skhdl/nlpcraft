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
 * Cache key bean.
 */
public class NCEndpointCacheKey implements Serializable {
    private final String srvReqId;
    private final String endpoint;
    
    public NCEndpointCacheKey(String srvReqId, String endpoint) {
        this.srvReqId = srvReqId;
        this.endpoint = endpoint;
    }
    
    public String getSrvReqId() {
        return srvReqId;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        NCEndpointCacheKey that = (NCEndpointCacheKey) o;
        
        if (!srvReqId.equals(that.srvReqId)) return false;
        return endpoint.equals(that.endpoint);
    }
    
    @Override
    public int hashCode() {
        int result = srvReqId.hashCode();
        result = 31 * result + endpoint.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("Key [srvReqId=%s, endpoint=%s]", srvReqId, endpoint);
    }
}
