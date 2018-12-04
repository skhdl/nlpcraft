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
