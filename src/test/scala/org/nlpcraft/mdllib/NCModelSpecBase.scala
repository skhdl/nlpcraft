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

package org.nlpcraft.mdllib

import java.util
import java.util.{Collections, List => JList}

import org.nlpcraft.mdllib.tools.builder._
import org.nlpcraft.mdllib.tools.scala.NCScalaSupport._

/**
  * Base model test trait.
  */
trait NCModelSpecBase {
    /**
      * Creates new valid test model.
      *
      * @return
      */
    def makeInvalidModelProvider: NCModelProvider =
        new NCModelProvider {
            private val ds = NCModelDescriptorBuilder.newDescriptor("id", "Test-Model", "1.0").build()
    
            override def makeModel(id: String): NCModel =
                NCModelBuilder.newModel()
                    .setDescriptor(ds)
                    .addMacro("<OF>", "{of|for|per}")
                    .addMacro("<INFO>", "{metrics|report|analysis|analytics|info|information|statistics|stats}")
                    .addMacro("<INFO>", "duplicate")
                    .addMacro("<ID>", "{unique|*} {id|guid|identifier|identification} {number|*}")
                    .addMacro("<NAME>", "{name|title}")
                    .addMacro("<TOTAL>", "{overall|total|grand total|entire|full}")
                    .addMacro("<NUM>", "{number|count|qty|quantity|amount}")
                    .addElement(NCElementBuilder.newElement("ELM1")
                        .setDescription("Element 1 description.")
                        .addSynonyms(
                            "element {number|*} {<OF>|*} {one|1}",
                            "{some|*} {elm|elem} {n.|num|*} {one|1}",
                            "foo {bar|*}",
                            "{fool|/[ab].+/}", // 'fool' or regex matching.
                            "worked" // Stem 'work'.
                        )
                        .build()
                    )
                    .addElement(NCElementBuilder.newElement("ELM2")
                        .setDescription("Super element description.")
                        .addSynonyms(
                            "foo bar",
                            "/[ab].+/", // Regex.
                            "working" // Stem 'work'.
                        )
                        .build()
                    )
                    .setQueryFunction((_: NCQueryContext) ⇒ null)
                    .build()
    
            override def getDescriptors: JList[NCModelDescriptor] = new util.ArrayList()
        }

    /**
      * Creates new valid test model.
      * 
      * @return
      */
    def makeValidModelProvider: NCModelProvider =
        new NCModelProvider {
            private val ds = NCModelDescriptorBuilder.newDescriptor("id", "Test-Model", "1.0").build()
    
            override def makeModel(id: String): NCModel =
                NCModelBuilder.newModel()
                    .setDescriptor(ds)
                    .addMacro("<OF>", "{of|for|per}")
                    .addMacro("<INFO>", "{metrics|report|analysis|analytics|info|information|statistics|stats}")
                    .addMacro("<ID>", "{unique|*} {id|guid|identifier|identification} {number|*}")
                    .addMacro("<NAME>", "{name|title}")
                    .addMacro("<TOTAL>", "{overall|total|grand total|entire|full}")
                    .addMacro("<NUM>", "{number|count|qty|quantity|amount}")
                    .addElement(NCElementBuilder.newElement("ELM1")
                        .setDescription("Element 1 description.")
                        .addSynonyms(
                            "element {number|*} {<OF>|*} {one|1}",
                            "{some|*} {elm|elem} {n.|num|*} {one|1}"
                        )
                        .build()
                    )
                    .addElement(NCElementBuilder.newElement("ELM2")
                        .setDescription("Super element description.")
                        .addSynonyms(
                            "super {elm|elem|element}"
                        )
                        .build()
                    )
                    .setQueryFunction((_: NCQueryContext) => null)
                    .build()
    
            /**
              * Gets the list, potentially empty, of deployment descriptors supported by this provider.
              *
              * @return List of deployment descriptor.
              */
            override def getDescriptors: JList[NCModelDescriptor] = Collections.singletonList(ds)
        }
}
