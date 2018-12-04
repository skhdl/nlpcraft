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
import java.util.*;
import java.util.function.*;

/**
 * Conversation context.
 * <br><br>
 * Conversation management is based on idea of a short-term-memory (STM). STM can be viewed as a condensed
 * short-term history of the user input for a given user and data source. Every submitted user request that wasn't
 * rejected or curated and for which the specific sentence {@link NCVariant variant} was determined is added to the
 * conversation STM. Existing STM tokens with the same {@link NCElement#getGroup() group} will be overridden by
 * the more recent tokens from the same group.
 *
 * @see NCQueryContext#getConversationContext()
 */
public interface NCConversationContext {
    /**
     * Gets unordered set of tokens stored in the current conversation STM for current
     * user and data source. Note that this set excludes free words and stopwords. Note also that specific rules
     * by which STM operates are undefined for the purpose of this function (i.e. callers should not rely on
     * any observed behavior of how STM stores and evicts its content).
     *
     * @return Unordered list of tokens for this conversation's STM.
     * @see NCIntentSolver
     */
    Set<NCToken> getTokens();

    /**
     * Removes all tokens satisfying given predicate from the current conversation STM.
     * This is particularly useful when the logic processing the user input makes an implicit
     * assumption not present in the user input itself. Such assumption may affect the conversation context and
     * therefore this method can be used to remove "stale" tokens from conversation STM.
     * <br><br>
     * For example, in some cases the intent logic can assume the user current location as an implicit geo
     * location and therefore all existing {@code nlp:geo} tokens should be removed from the conversation STM
     * to maintain proper context. 
     *
     * @param filter Token remove filter.
     */
    void clear(Predicate<NCToken> filter);
}
