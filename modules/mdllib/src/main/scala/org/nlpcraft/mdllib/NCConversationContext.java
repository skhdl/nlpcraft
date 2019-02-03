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

package org.nlpcraft.mdllib;

import org.nlpcraft.mdllib.intent.*;
import java.util.*;
import java.util.function.*;

/**
 * Conversation context.
 * <br><br>
 * Conversation management is based on idea of a short-term-memory (STM). STM can be viewed as a condensed
 * short-term history of the user input for a given user and data source. Every submitted user request that wasn't
 * rejected is added to the conversation STM as a list of {@link NCToken tokens}. Existing STM tokens with
 * the same {@link NCElement#getGroup() group} will be overridden by the more recent tokens from the same group.
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
     * assumption not present in the user input itself. Such assumption may alter the conversation context and
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
