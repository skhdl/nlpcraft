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

package org.nlpcraft2.cache

/**
 * NLP cache key type.
 */
object NCCacheKeyType extends Enumeration {
    type NCCacheKeyType = Value

    /**
     * Stem key composed of all stems in NLP sentence minus the stop-words.
     * There is only one stem key for each NLP sentence.
     *
     * Stem key is the most specific key for given NLP sentence (additionally to lemma key).
     * Only the NLP sentences that differ only by stop-words will resolve into the same stem key.
     */
    val KT_STEM: Value = Value("STEM")

    /**
     * Lemma key composed of all lemmas in NLP sentence minus the stop-words.
     * There is only one lemma key for each sentence.
     *
     * Lemma key is the most specific key for given NLP sentence (additionally to stem key).
     * Only the NLP sentences that differ only by stop-words and lemma variations will
     * resolve into the same lemma key.
     */
    val KT_LEMMA: Value = Value("LEMMA")

    /**
     * Token key composed of free words as stems and detected tokens.
     */
    val KT_TOKEN: Value = Value("TOKEN")

    /**
     * Token key composed of free words' Penn POSes and detected tokens.
     */
    val KT_SYN_TOKEN: Value = Value("SYN_TOKEN")

    /**
     * Token key composed of shortened free words' Penn POSes (first letter) and detected tokens.
     */
    val KT_SYN_BASE_TOKEN: Value = Value("SYN_BASE_TOKEN")
}