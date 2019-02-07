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

import java.io.IOException;
import java.util.List;

/**
 * Model testing client. This client can be used for convienient unit testing of the models together
 * with any popular unit testing framework like <a href="http://www.testng.org">TestNG</a> or
 * <a href="https://junit.org">JUnit</a>. The instance of test client should be obtained
 * via {@link NCTestClientBuilder}.
 *
 * @see NCTestClientBuilder
 */
public interface NCTestClient {
    /**
     * Tests all given sentences and returns corresponding list of results.
     *
     * @param tests List of sentences to test.
     * @return Tests results.
     * @throws NCTestClientException Thrown if any test system errors occur.
     * @throws IOException Thrown in case of I/O errors.
     */
    List<NCTestResult> test(List<NCTestSentence> tests) throws NCTestClientException, IOException;
    
    /**
     * Tests all given sentences and returns corresponding list of results.
     *
     * @param tests List of sentences to test.
     * @return Tests results.
     * @throws NCTestClientException Thrown if any test system errors occur.
     * @throws IOException Thrown in case of I/O errors.
     */
    List<NCTestResult> test(NCTestSentence... tests) throws NCTestClientException, IOException;

    /**
     * Tests single sentence and returns its result.
     *
     * @param sen Sentence to test.
     * @return Sentence result.
     * @throws NCTestClientException Thrown if any test system errors occur.
     * @throws IOException Thrown in case of I/O errors.
     */
    NCTestResult test(NCTestSentence sen) throws NCTestClientException, IOException;
}
