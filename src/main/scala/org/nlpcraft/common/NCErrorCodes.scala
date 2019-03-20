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

package org.nlpcraft.common

/**
  * Rejection error codes.
  */
object NCErrorCodes {
    /** Undefined system error. */
    final val MODEL_REJECTION = 1
    /** Unexpected system error. */
    final val UNEXPECTED_ERROR = 100
    /** Model's result is too big. */
    final val RESULT_TOO_BIG = 101
    /** Expected system error. */
    final val SYSTEM_ERROR = 102
    /** Too many unknown words. */
    final val MAX_UNKNOWN_WORDS = 10001
    /** Sentence is too complex. */
    final val MAX_FREE_WORDS = 10002
    /** Too many suspicious or unrelated words. */
    final val MAX_SUSPICIOUS_WORDS = 10003
    /** Swear words are not allowed. */
    final val ALLOW_SWEAR_WORDS = 10004
    /** Sentence contains no nouns. */
    final val ALLOW_NO_NOUNS = 10005
    /** Only latin charset is supported. */
    final val ALLOW_NON_LATIN_CHARSET = 10006
    /** Only english language is supported. */
    final val ALLOW_NON_ENGLISH = 10007
    /** Sentence seems unrelated to data source. */
    final val ALLOW_NO_USER_TOKENS = 10008
    /** Sentence is too short. */
    final val MIN_WORDS = 10009
    /** Sentence is ambiguous. */
    final val MIN_NON_STOPWORDS = 10010
    /** Sentence is too short. */
    final val MIN_TOKENS = 10011
    /** Sentence is too long. */
    final val MAX_TOKENS = 10012
    /** Too many geographical locations detected. */
    final val MAX_GEO_TOKENS = 10013
    /** Too few geographical locations detected. */
    final val MIN_GEO_TOKENS = 10014
    /** Too many dates detected. */
    final val MAX_DATE_TOKENS = 10015
    /** Too few dates detected. */
    final val MIN_DATE_TOKENS = 10016
    /** Too many numbers detected. */
    final val MAX_NUM_TOKENS = 10017
    /** Too few numbers detected. */
    final val MIN_NUM_TOKENS = 10018
    /** Too many functions detected. */
    final val MAX_FUNCTION_TOKENS = 10019
    /** Too few functions detected. */
    final val MIN_FUNCTION_TOKENS = 10020
    
}
