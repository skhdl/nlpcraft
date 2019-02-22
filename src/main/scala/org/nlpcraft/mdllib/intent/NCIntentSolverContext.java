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

package org.nlpcraft.mdllib.intent;

import org.nlpcraft.mdllib.NCModel;
import org.nlpcraft.mdllib.NCQueryContext;
import org.nlpcraft.mdllib.NCToken;
import org.nlpcraft.mdllib.NCVariant;

import java.io.Serializable;
import java.util.List;

/**
 * A context that is passed into {@link NCIntentSolver.IntentCallback callback} of the matched intent.
 *
 * @see NCIntentSolver.IntentCallback
 * @see NCIntentSolver#addIntent(NCIntentSolver.INTENT, NCIntentSolver.IntentCallback)
 */
public interface NCIntentSolverContext extends Serializable {
    /**
     * Gets ID of the matched intent.
     *
     * @return ID of the matched intent.
     */
    String getIntentId();

    /**
     * Query context from the original {@link NCModel#query(NCQueryContext)} method.
     *
     * @return Original query context.
     */
    NCQueryContext getQueryContext();

    /**
     * Gets a subset of tokens representing matched intent. This subset is grouped by the matched terms
     * where {@code null} sub-list defines an optional term. Order and index of sub-lists corresponds
     * to the order and index of terms in the matching intent. Note that unlike {@link #getVariant()} method
     * this method returns only subset of the tokens that were part of the matched intent. Specifically, it will
     * not return tokens for free words, stopwords or unmatched ("dangling") tokens.
     *
     * @return List of list of tokens representing matched intent.
     * @see #getVariant() 
     */
    List<List<NCToken>> getIntentTokens();

    /**
     * Gets sentence variant that produced the matching for this intent.
     * 
     * @return Sentence variant that produced the matching for this intent.
     * @see #getIntentTokens() 
     */
    NCVariant getVariant();

    /**
     * Indicates whether or not the intent match was exact.
     * <br><br>
     * An exact match means that for the intent to match it has to use all non-free word tokens
     * in the user input, i.e. only free word tokens can be left after the match. Non-exact match
     * doesn't have this restriction. Note that non-exact match should be used with a great care.
     * Non-exact match completely ignores extra found user or system tokens (which are not part
     * of the intent template) which could have altered the matching outcome had they been included.
     * <br><br>
     * Intent callbacks can check this property and provide custom rejection message.
     *
     * @return {@code True} if the intent match was exact, {@code false} otherwise.
     */
    boolean isExactMatch();
}
