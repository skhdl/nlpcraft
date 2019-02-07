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

package org.nlpcraft.examples.tests;

import org.nlpcraft.mdllib.tools.dev.NCTestSentence;

import java.util.Optional;
import java.util.function.Function;

/**
 * Test behaviour expectation.
 * See {@link TestFactory}.
 */
public interface TestExpectation {
    /**
     * Gets test sentence.
     *
     * @return Test sentence.
     */
    NCTestSentence getTest();
    
    /**
     * Should test be passed or not expectation flag.
     *
     * @return Flag.
     */
    boolean shouldPassed();
    
    /**
     * Gets optional result checker.
     *
     * This function should return error message if its argument (test sentence execution result) is not expected
     * or empty value if result validated successfully.
     *
     * @return Optional result checker.
     */
    Optional<Function<String, Optional<String>>> getResultChecker();
    
    /**
     * Gets optional error checker.
     *
     * This function should return error message if its argument (test sentence execution error message) is not expected
     * or empty value if error message validated successfully.
     *
     * @return Optional expected checker.
     */
    Optional<Function<String, Optional<String>>> getErrorChecker();
}
