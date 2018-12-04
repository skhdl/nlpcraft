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
 * When thrown indicates that human curation is required.
 * <p>
 * This exception should indicates that user has likely provided all necessary information
 * in the input string but the model can't figure out a definitive query for it and therefore requires a human
 * curation to resolve the "last mile". In most cases it is used to have human operator resolve in real time voice
 * input irregularities, complex grammar ambiguities, misspellings, colloquialisms or unsupported slang.
 * <p>
 * Note that during curation the human operator can not only correct (i.e. normalize) input sentence but
 * can also provide curation hint that can further facilitate model in determining the appropriate query.
 *
 * @see NCRejection
 * @see NCModel#query(NCQueryContext)
 */
public class NCCuration extends RuntimeException {
    /** */
    private NCVariant var;
    
    /** */
    private Map<String, Object> metadata;

    /**
     * Creates curation exception without a message.
     */
    public NCCuration() {
        // No-op.
    }

    /**
     * Creates curation exception with given message.
     *
     * @param msg Curation message. Although minimal HTML markup is supported it will only be rendered
     *      by the webapp or by compatible user REST applications. Other client devices like voice-based
     *      assistants may not support that. For cross-platform compatibility it is recommended to stick
     *      with a simple text.
     */
    public NCCuration(String msg) {
        super(msg);
    }

    /**
     * Creates curation exception with given error message and cause.
     *
     * @param msg Curation message. Although minimal HTML markup is supported it will only be rendered
     *      by the webapp or by compatible user REST applications. Other client devices like voice-based
     *      assistants may not support that. For cross-platform compatibility it is recommended to stick
     *      with a simple text.
     * @param var Optional sentence variant this curation refers to.
     */
    public NCCuration(String msg, NCVariant var) {
        super(msg);

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
    public NCCuration setVariant(NCVariant var) {
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
