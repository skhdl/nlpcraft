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

package org.nlpcraft.model;

import org.nlpcraft.model.intent.NCIntentSolver;

import java.util.List;
import java.util.function.Predicate;

/**
 * Conversation context.
 * <br><br>
 * Conversation management is based on idea of a short-term-memory (STM). STM can be viewed as a condensed
 * short-term history of the user input for a given user and data model - both acting as a compound key for the STM.
 * Every submitted user request that wasn't
 * rejected is added to the conversation STM as a list of {@link NCToken tokens}. Existing STM tokens with
 * the same {@link NCElement#getGroup() group} will be overridden by the more recent tokens from the same group.
 * Note also that tokens in STM automatically expire (i.e. context is "forgotten") after a certain period of time
 * and other internal logic. Note that you should not rely on a specific expiration behavior as its logic
 * is not deterministic and may be changed in the future.
 *
 * @see NCQueryContext#getConversationContext()
 */
public interface NCConversationContext {
    /**
     * Gets an ordered list of tokens stored in the current conversation STM for current
     * user and data model. Tokens in the returned list are ordered by their conversational depth, i.e.
     * the tokens from more recent requests appear before tokens from older requests.
     * <br><br>
     * Note that this list excludes free words and stopwords. Note also that specific rules
     * by which STM operates are undefined for the purpose of this function (i.e. callers should not rely on
     * any observed behavior of how STM stores and evicts its content).
     *
     * @return List of tokens for this conversation's STM.
     * @see NCIntentSolver
     */
    List<NCToken> getTokens();

    /**
     * Removes all tokens satisfying given predicate from the current conversation STM.
     * This is particularly useful when the logic processing the user input makes an implicit
     * assumption not present in the user input itself. Such assumption may alter the conversation context (without
     * having an explicit token responsible for it) and therefore this method can be used to remove "stale" tokens
     * from conversation STM.
     * <br><br>
     * For example, in some cases the intent logic can assume the user current location as an implicit geo
     * location and therefore all existing {@code nlp:geo} tokens should be removed from the conversation STM
     * to maintain correct context.
     *
     * @param filter Token remove filter.
     */
    void clear(Predicate<NCToken> filter);
}
