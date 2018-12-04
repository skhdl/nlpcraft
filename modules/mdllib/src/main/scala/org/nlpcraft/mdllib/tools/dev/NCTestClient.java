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

package org.nlpcraft.mdllib.tools.dev;

import java.io.IOException;
import java.util.List;

/**
 * The main entry API for model testing framework. The instance of test client should be obtained
 * via {@link NCTestClientBuilder}.
 *
 * @see NCTestClientBuilder
 */
public interface NCTestClient {
    /** Response status: request successfully answered. */
    int RESP_OK = 1;
    
    /** Response status: request rejected from user code. */
    int RESP_REJECT = 2;
    
    /** Response status: request forwarded to curation from user code.*/
    int RESP_CURATION = 3;
    
    /** Response status: request successfully answered with trivia response. */
    int RESP_TRIVIA = 4;
    
    /** Response status: request successfully answered with talkback response. */
    int RESP_TALKBACK = 5;
    
    /** Response status: request returned with error due to model validation check. */
    int RESP_VALIDATION = 6;
    
    /** Response status: request returned with error due to user code or system errors. */
    int RESP_ERROR = 7;
    
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
}
