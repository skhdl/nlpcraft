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

/**
 * Test sentence result. For each {@link NCTestSentence} the test framework returns an instance of this interface.
 */
public interface NCTestResult {
    /**
     * Gets data source ID.
     *
     * @return Data source ID that was used in testing.
     */
    long getDsId();
    
    /**
     * Gets test sentence text.
     *
     * @return Test sentence text.
     */
    String getText();
    
    /**
     * Gets optional intent ID.
     *
     * @return Intent ID or {@code null}.
     */
    String getIntentId();
    
    /**
     * Gets result response status. See constants in {@link NCTestClient} interface.
     *
     * @return Result response status.
     */
    int getResultStatus();
    
    /**
     * Gets sentence processing time in milliseconds.
     *
     * @return Processing time in milliseconds.
     */
    long getProcessingTime();
    
    /**
     * Gets optional error. Error is not {@code null} when:
     * <ul>
     * <li>expected intent ID is defined and doesn't correspond to the actual matched intent ID,</li>
     * <li>or expected response status doesn't correspond to the actual response one,</li>
     * <li>or user custom result validation failed the result validation.</li>
     * </ul>
     *
     * @return Optional error or {@code null}.
     */
    String getError();
    
    /**
     * Gets error flag.
     *
     * @return Error flag.
     */
    default boolean hasError() {
        return getError() != null;
    }
}
