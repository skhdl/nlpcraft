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
 * Licensor:    Copyright (C) 2018 DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.mdllib.tools.dev;

import org.nlpcraft.mdllib.NCQueryResult;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * TODO:
 * Sentence to test. This interface defines a sentence to be tested, its data source as well as its expected
 * response status and optional result checker. One or more test sentences are passed to
 * {@link NCTestClient#test(NCTestSentence...)} or {@link NCTestClient#test(List)} methods.
 */
public interface NCTestSentence {
    /**
     * Gets sentence text.
     *
     * @return Sentence text.
     */
    String getText();
    
    /**
     * TODO:
     * Gets data source ID. Note that it should return value or {@link NCTestSentence#getModelId()}
     *
     * @return Data source ID.
     */
    Optional<Long> getDatasourceId();
    
    /**
     * TODO:
     * Gets model ID. Note that it should return value or {@link NCTestSentence#getDatasourceId()}
     *
     * @return Model ID.
     */
    Optional<String> getModelId();
    
    /**
     * Gets excepted result type flag. TODO:
     *
     * @return Excepted result type flag.
     */
    boolean shouldPassed();
    
    /**
     * Gets optional result validation predicate.
     *
     * @return Validation predicate or {@code}.
     */
    Optional<Predicate<NCQueryResult>> getCheckResult();
    
    /**
     * Gets optional error validation predicate.
     *
     * @return Validation predicate or {@code}.
     */
    Optional<Predicate<String>> getCheckError();
}
