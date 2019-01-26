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

import java.util.*;
import java.util.function.Predicate;

/**
 * Sentence to test. This class defines a sentence to be tested, its data source as well as its expected
 * response status and optional result checker. One or more test sentences are passed to
 * {@link NCTestClient#test(NCTestSentence...)} or {@link NCTestClient#test(List)} methods.
 */
public class NCTestSentence {
    private long dsId;
    private String text;
    private String expIntentId;
    private int expStatus;
    private Predicate<NCQueryResult> check;
    
    /**
     * Creates new test sentence with given parameters.
     *
     * @param dsId Data source UD.
     * @param text Test sentence text.
     * @param expIntentId Optional expected intent ID. Supply {@code null} if intent based matching isn't used
     *      or intent ID check isn't required.
     * @param expStatus Expected result status. See constants in {@link NCTestClient} interface.
     * @param check Optional result validation predicate. Supply {@code null} if not required.
     */
    public NCTestSentence(
        long dsId,
        String text,
        String expIntentId,
        int expStatus,
        Predicate<NCQueryResult> check
    ) {
        if (expIntentId != null) {
            switch (expStatus) {
                case NCTestClient.RESP_VALIDATION:
                    throw new IllegalArgumentException(
                        "Intent ID can only be specified for the following responses: RESP_OK, RESP_REJECT."
                    );
                default: // No-op.
            }
        }
        
        this.dsId = dsId;
        this.text = text;
        this.expIntentId = expIntentId;
        this.expStatus = expStatus;
        this.check = check;
    }
    
    /**
     * Creates new test sentence with given parameters (without optional result check predicate).
     *
     * @param dsId Data source ID.
     * @param text Test sentence text.
     * @param expIntentId Optional expected intent ID. Supply {@code null} if intent based matching isn't used
     *      or intent ID check isn't required.
     * @param expStatus Expected result status. See constants in {@link NCTestClient} interface.
     */
    public NCTestSentence(long dsId, String text, String expIntentId, int expStatus) {
        this(dsId, text, expIntentId, expStatus, null);
    }

    /**
     * Creates new test sentence with given parameters (without intent ID and optional result check predicate).
     *
     * @param dsId Data source ID.
     * @param text Test sentence text.
     * @param expStatus Expected result status. See constants in {@link NCTestClient} interface.
     */
    public NCTestSentence(long dsId, String text, int expStatus) {
        this(dsId, text, null, expStatus, null);
    }

    /**
     * Gets data source ID.
     *
     * @return Data source ID.
     */
    public long getDsId() {
        return dsId;
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
