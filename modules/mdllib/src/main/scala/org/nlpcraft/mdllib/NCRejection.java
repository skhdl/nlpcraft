/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.mdllib;

import org.nlpcraft.mdllib.intent.*;

import java.util.Collections;
import java.util.Map;

/**
 * When thrown indicates that user input cannot be processed as is.
 * <p>
 * This exception typically indicates that user has not provided enough of information in the input string
 * to have it processed automatically or with human curation. In most cases this means that the user's input is
 * either too short or too simple, too long or too complex, missing required context, or unrelated to selected
 * data source.
 *
 * @see NCCuration
 * @see NCModel#query(NCQueryContext)
 */
public class NCRejection extends RuntimeException {
    /** */
    private NCVariant var;
    
    /** */
    private Map<String, Object> metadata;
    
    /**
     * Creates new rejection exception with given message.
     *
     * @param msg Rejection message. Although minimal HTML markup is supported it will only be rendered
     *      by the webapp or by compatible user REST applications. Other client devices like voice-based
     *      assistants may not support that. For cross-platform compatibility it is recommended to stick
     *      with a simple text.
     */
    public NCRejection(String msg) {
        super(msg);
    }

    /**
     * Creates new rejection exception with given message and cause.
     *
     * @param msg Rejection message. Although minimal HTML markup is supported it will only be rendered
     *      by the webapp or by compatible user REST applications. Other client devices like voice-based
     *      assistants may not support that. For cross-platform compatibility it is recommended to stick
     *      with a simple text.
     * @param cause Cause of this exception.
     */
    public NCRejection(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Creates new rejection exception with given message and cause.
     *
     * @param msg Rejection message. Although minimal HTML markup is supported it will only be rendered
     *      by the webapp or by compatible user REST applications. Other client devices like voice-based
     *      assistants may not support that. For cross-platform compatibility it is recommended to stick
     *      with a simple text.
     * @param cause Cause of this exception.
     * @param var Optional sentence variant this curation refers to.
     */
    public NCRejection(String msg, Throwable cause, NCVariant var) {
        super(msg, cause);

        this.var = var;
    }

    /**
     * Sets optional sentence variant this curation refers to.
     * <br><br>
     * Note that in general a user input can have more than one possible
     * parsing {@link NCSentence#variants() variants}. Setting the specific variant that was the cause of the curation
     * is optional but improves the self-learning capabilities of the system when provided. Note also that
     * sub-systems like {@link NCIntentSolver intent-based solver} will set the proper variant automatically.
     *
     * @param var Sentence variant to set.
     * @return This instance of chaining calls.
     */
    public NCRejection setVariant(NCVariant var) {
        this.var = var;

        return this;
    }

    /**
     * Gets optional sentence variant associated with this curation.
     *
     * @return Sentence variant associated with this curation or {@code null}.
     */
    public NCVariant getVariant() {
        return var;
    }
    
    
    /**
     * Gets metadata.
     *
     * @return Metadata.
     */
    public Map<String, Object> getMetadata() {
        return metadata != null ? metadata : Collections.emptyMap();
    }
    
    /**
     * Sets metadata.
     *
     * @param metadata Metadata
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
