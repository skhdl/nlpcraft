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
 * Software:    NlpCraft
 * License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
 * Licensor:    DataLingvo, Inc. https://www.datalingvo.com
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private List<NCVariant> vars;
    
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
     * TODO: javadoc
     *
     * Creates curation exception with given error message and cause.
     *
     * @param msg Curation message. Although minimal HTML markup is supported it will only be rendered
     *      by the webapp or by compatible user REST applications. Other client devices like voice-based
     *      assistants may not support that. For cross-platform compatibility it is recommended to stick
     *      with a simple text.
     * @param vars Optional sentence variant this curation refers to.
     */
    public NCCuration(String msg, List<NCVariant> vars) {
        super(msg);

        this.vars = vars;
    }

    /**
     * TODO: javadoc
     *
     * Sets optional sentence variant this curation refers to.
     * <br><br>
     * Note that in general a user input can have more than one possible
     * parsing {@link NCSentence#variants() variants}. Setting the specific variant that was the cause of the curation
     * is optional but improves the self-learning capabilities of the system when provided. Note also that
     * sub-systems like {@link NCIntentSolver intent-based solver} will set the proper variant automatically.
     *
     * @param vars Sentence variant to set.
     * @return This instance of chaining calls.
     */
    public NCCuration setVariants(List<NCVariant> vars) {
        this.vars = vars;

        return this;
    }

    /**
     * TODO: javadoc
     *
     * Gets optional sentence variant associated with this curation.
     *
     * @return Sentence variant associated with this curation or {@code null}.
     */
    public List<NCVariant> getVariants() {
        return vars;
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
