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

package org.nlpcraft.model.tools.dev;

import java.io.IOException;

/**
 * Model testing client. This client can be used for convienient unit testing of the models together
 * with any popular unit testing framework such as <a href="http://www.testng.org">TestNG</a> or
 * <a href="https://junit.org">JUnit</a>. The instance of test client should be obtained
 * via {@link NCTestClientBuilder}.
 *
 * @see NCTestClientBuilder
 */
public interface NCTestClient {
    /**
     * Tests single sentence and returns its result.
     *
     * @param txt Text sentence to test.
     * @return Sentence result.
     * @throws NCTestClientException Thrown if any test system errors occur.
     * @throws IOException Thrown in case of I/O errors.
     */
    NCTestResult ask(String txt) throws NCTestClientException, IOException;

    /**
     * Connects test client to the server for testing with given data source ID.
     *
     * @param dsId Data source ID to open this client for.
     * @throws NCTestClientException Thrown if any test system errors occur.
     * @throws IOException Thrown in case of I/O errors.
     */
    void openForDataSourceId(long dsId) throws NCTestClientException, IOException;
    
    /**
     * Connects test client to the server for testing with given model ID. Note that
     * a temporary data source will be automatically created for this model ID.
     *
     * @param mdlId Model ID to open this client for.
     * @throws NCTestClientException Thrown if any test system errors occur.
     * @throws IOException Thrown in case of I/O errors.
     */
    void openForModelId(String mdlId) throws NCTestClientException, IOException;

    /**
     * Closes test client connection to the server. Removes temporary data source
     * if one was created.
     *
     * @throws NCTestClientException Thrown if any test system errors occur.
     * @throws IOException Thrown in case of I/O errors.
     */
    void close() throws NCTestClientException, IOException;

    /**
     * Clears conversation for this test client. This method will clear conversation for
     * its configured user.
     *
     * @throws NCTestClientException Thrown if any test system errors occur.
     * @throws IOException Thrown in case of I/O errors.
     */
    void clearConversation() throws NCTestClientException, IOException;
}
