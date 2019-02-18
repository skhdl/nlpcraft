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

package org.nlpcraft.mdllib.tools.dev;

import java.util.*;

/**
 * Test sentence result.
 */
public interface NCTestResult {
    /**
     * Gets test sentence text.
     *
     * @return Test sentence text.
     */
    String getText();
    
    /**
     * Gets total sentence processing time in milliseconds.
     *
     * @return Processing time in milliseconds.
     */
    long getProcessingTime();
    
    /**
     * Gets data source ID.
     *
     * @return Data source ID.
     */
    long getDataSourceId();
    
    /**
     * Gets model ID.
     *
     * @return Model ID.
     */
    String getModelId();
    
    /**
     * Gets optional execution result.
     *
     * @return Optional execution result.
     * @see #isFailed()
     * @see #isSuccessful()
     */
    Optional<String> getResult();
    
    /**
     * Gets optional execution result type.
     *
     * @return Optional execution result type.
     * @see #isFailed()
     * @see #isSuccessful()
     */
    Optional<String> getResultType();
    
    /**
     * Gets optional execution error.
     *
     * @return Optional execution error.
     * @see #isFailed()
     * @see #isSuccessful()
     */
    Optional<String> getResultError();

    /**
     * Tests whether or not this result corresponds to a failed execution. This is identical to:
     * <pre class="brush: java">
     *      return getResultError().isPresent();
     * </pre>
     *
     * @return {@code true} if result corresponds to a failed execution, {@code false} otherwise.
     */
    default boolean isFailed() {
        return getResultError().isPresent();
    }

    /**
     * Tests whether or not this result corresponds to a successful execution. This is identical to:
     * <pre class="brush: java">
     *      return getResult().isPresent() &amp;&amp; getResultType().isPresent();
     * </pre>
     *
     * @return {@code true} if result corresponds to a successful execution, {@code false} otherwise.
     */
    default boolean isSuccessful() {
        return getResult().isPresent() && getResultType().isPresent();
    }
}
