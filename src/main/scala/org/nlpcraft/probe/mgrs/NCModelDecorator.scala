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

package org.nlpcraft.probe.mgrs

import org.nlpcraft.model.{NCElement, NCModel}

/**
  *
  * @param model Decorated model.
  * @param synonyms Fast-access synonyms map.
  * @param excludedSynonyms Fast-access excluded synonyms map.
  * @param additionalStopWordsStems Stemmatized additional stopwords.
  * @param excludedStopWordsStems Stemmatized excluded stopwords.
  * @param suspiciousWordsStems Stemmatized suspicious stopwords.
  * @param elements Map of model elements.
  */
case class NCModelDecorator(
    model: NCModel,
    synonyms: Map[String/*Element ID*/, Map[Int/*Synonym length*/, Seq[NCSynonym]]], // Fast access map.
    excludedSynonyms: Map[String/*Element ID*/, Map[Int/*Synonym length*/, Seq[NCSynonym]]], // Fast access map.
    additionalStopWordsStems: Set[String],
    excludedStopWordsStems: Set[String],
    suspiciousWordsStems: Set[String],
    elements: Map[String/*Element ID*/, NCElement]
) extends java.io.Serializable {
    override def toString: String = {
        val ds = model.getDescriptor
        
        s"Probe model decorator [id=${ds.getId}, name=${ds.getName}, version=${ds.getVersion}]"
    }
}
