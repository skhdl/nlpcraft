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

package org.nlpcraft.nlp.dict

/**
 * Dictionary type.
 *
 * `MOBY` dictionary contains several databases, each of them is set of words.
 * This type defines databases, which are used in the Dictionary service.
 */
object NCDictionaryType extends Enumeration {
    type NCDictionaryType = Value

    // Database which contains the most common names used in the United States and Great Britain.
    val WORD_PROPER_NOUN: Value = Value

    // Database which contains single words, excluding proper names, acronyms, or compound words and phrases.
    val WORD: Value = Value

    // Database which contains common dictionary words.
    val WORD_COMMON: Value = Value

    // Database which contains common acronyms & abbreviations.
    val WORD_ACRONYM: Value = Value

    // Database which contains the 1,000 most frequently used English words.
    val WORD_TOP_1000: Value = Value
}
