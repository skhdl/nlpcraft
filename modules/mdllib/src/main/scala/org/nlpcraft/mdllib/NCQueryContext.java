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
import java.io.*;

/**
 * Model query context. This context defines fully processed user input and its associated data that the model
 * needs to process and return the result in {@link NCModel#query(NCQueryContext)} method.
 * <br><br>
 * See {@link NCIntentSolver} for intent base matching utility.
 *
 * @see NCIntentSolver
 * @see NCIntentSolverContext
 */
public interface NCQueryContext extends Serializable {
    /**
     * Gets ID of the current request.
     * <p>
     * Server request is defined as a processing of a one user input sentence (a session).
     * Note that model can be accessed multiple times during processing of a single user sentence
     * and therefor that ID can appear in multiple invocations of {@link NCModel#query(NCQueryContext)} method.
     * In fact, users of this interfaces can use this fact by using this ID,
     * for example, as a map key for a session scoped storage.
     * 
     * @return Server request ID.
     * @see NCModel#query(NCQueryContext)
     */
    String getServerRequestId();

    /**
     * Gets model instance for this query.
     * .
     * @return Model.
     */
    NCModel getModel();

    /**
     * Gets descriptor of data source associated with this context.
     *
     * @return Descriptor of data source for the user sentence associated with this context.
     */
    NCDataSource getDataSource();

    /**
     * During human curation an operator can provide a hint that can be used by
     * {@link NCModel#query(NCQueryContext)} method. A hint can be anything that can be expressed in text:
     * SQL query, REST call, property set, JavaScript code, etc.
     *
     * @return Curation hint or {@code null} if not provided.
     * @see NCCuration
     */
    String getHint();

    /**
     * Gets fully parsed, canonical representation of user input.
     *
     * @return Fully parsed, canonical representation of user input.
     */
    NCSentence getSentence();

    /**
     * Gets current conversation context.
     *
     * @return Current conversation context.
     */
    NCConversationContext getConversationContext();
}
