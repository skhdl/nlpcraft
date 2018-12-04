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

import org.nlpcraft.mdllib.NCQueryResult;

import java.util.*;
import java.util.function.Predicate;

/**
 * Sentence to test. This class defines a sentence to be tested, its data source as well as its expected
 * response status and optional result checker. One or more test sentences are passed to
 * {@link NCTestClient#test(NCTestSentence...)} or {@link NCTestClient#test(List)} methods.
 */
public class NCTestSentence {
    private String dsName;
    private String text;
    private String expIntentId;
    private int expStatus;
    private Predicate<NCQueryResult> check;
    
    /**
     * Creates new test sentence with given parameters.
     *
     * @param dsName Data source name. This data source (and its probe) should be deployed and accessible for
     *      testing user account. See {@link NCTestClientConfig#getEmail()} to see how to configure testing account.
     * @param text Test sentence text.
     * @param expIntentId Optional expected intent ID. Supply {@code null} if intent based matching isn't used
     *      or intent ID check isn't required. Note that intent ID cannot be specified together with the
     *      following result response statuses: {@link NCTestClient#RESP_TRIVIA}, {@link NCTestClient#RESP_ERROR},
     *      {@link NCTestClient#RESP_TALKBACK}, or {@link NCTestClient#RESP_VALIDATION}.
     * @param expStatus Expected result status. See constants in {@link NCTestClient} interface.
     * @param check Optional result validation predicate. Supply {@code null} if not required.
     */
    public NCTestSentence(
        String dsName,
        String text,
        String expIntentId,
        int expStatus,
        Predicate<NCQueryResult> check
    ) {
        if (expIntentId != null) {
            switch (expStatus) {
                case NCTestClient.RESP_TRIVIA:
                case NCTestClient.RESP_ERROR:
                case NCTestClient.RESP_TALKBACK:
                case NCTestClient.RESP_VALIDATION:
                    throw new IllegalArgumentException(
                        "Intent ID can only be specified for the following responses: RESP_OK, RESP_REJECT, RESP_CURATION"
                    );
                default: // No-op.
            }
        }
        
        
        this.dsName = dsName;
        this.text = text;
        this.expIntentId = expIntentId;
        this.expStatus = expStatus;
        this.check = check;
    }
    
    /**
     * Creates new test sentence with given parameters (without optional result check predicate).
     *
     * @param dsName Data source name. This data source (and its probe) should be deployed and accessible for
     *      testing user account. See {@link NCTestClientConfig#getEmail()} to see how to configure testing account.
     * @param text Test sentence text.
     * @param expIntentId Optional expected intent ID. Supply {@code null} if intent based matching isn't used
     *      or intent ID check isn't required. Note that intent ID cannot be specified together with the
     *      following result response statuses: {@link NCTestClient#RESP_TRIVIA}, {@link NCTestClient#RESP_ERROR},
     *      {@link NCTestClient#RESP_TALKBACK}, or {@link NCTestClient#RESP_VALIDATION}.
     * @param expStatus Expected result status. See constants in {@link NCTestClient} interface.
     */
    public NCTestSentence(String dsName, String text, String expIntentId, int expStatus) {
        this(dsName, text, expIntentId, expStatus, null);
    }

    /**
     * Creates new test sentence with given parameters (without intent ID and optional result check predicate).
     *
     * @param dsName Data source name. This data source (and its probe) should be deployed and accessible for
     *      testing user account. See {@link NCTestClientConfig#getEmail()} to see how to configure testing account.
     * @param text Test sentence text.
     * @param expStatus Expected result status. See constants in {@link NCTestClient} interface.
     */
    public NCTestSentence(String dsName, String text, int expStatus) {
        this(dsName, text, null, expStatus, null);
    }

    /**
     * Gets data source name.
     *
     * @return Data source name.
     */
    public String getDsName() {
        return dsName;
    }
    
    /**
     * Gets sentence text.
     *
     * @return Sentence text.
     */
    public String getText() {
        return text;
    }
    
    /**
     * Gets expected response status.
     *
     * @return Expected status. See constants in {@link NCTestClient} interface.
     */
    public int getExpectedStatus() {
        return expStatus;
    }
    
    /**
     * Gets optional expected intent ID.
     *
     * @return Intent ID or {@code null}.
     */
    public String getExpectedIntentId() {
        return expIntentId;
    }
    
    /**
     * Gets optional result validation predicate.
     *
     * @return Validation predicate or {@code}.
     */
    public Predicate<NCQueryResult> getCheck() {
        return check;
    }
}
