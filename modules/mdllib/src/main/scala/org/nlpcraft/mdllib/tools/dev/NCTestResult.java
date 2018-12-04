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

/**
 * Test sentence result. For each {@link NCTestSentence} the test framework returns an instance of this interface.
 */
public interface NCTestResult {
    /**
     * Gets data source name.
     *
     * @return Data source name that was used in testing.
     */
    String getDsName();
    
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
