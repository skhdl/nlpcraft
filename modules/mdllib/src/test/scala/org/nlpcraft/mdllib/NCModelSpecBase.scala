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

package org.nlpcraft.mdllib

import java.util
import java.util.Collections
import java.util.{List ⇒ JList}

import org.nlpcraft.mdllib.tools.builder._
import org.nlpcraft.mdllib.tools.scala.NCScalaSupport._
import org.nlpcraft.mdllib._
import org.nlpcraft.mdllib.tools.NCSerializableFunction
import org.nlpcraft.mdllib.tools.builder.{NCElementBuilder, NCModelBuilder, NCModelDescriptorBuilder}

import scala.collection.JavaConverters._

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
                    .addTrivia(List("hi", "howdy").asJava, List("hello!").asJava)
                    //.addTrivia(List("hi <X>", "howdy2"), List("hello!"))
                    .addTrivia(List("hi1", "howdy2").asJava, List("hello!").asJava)
                    //.addRestriction("BLA", 10)
                    .setQueryFunction((_: NCQueryContext) => null)
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
                    .setQueryFunction(
                        new NCSerializableFunction[NCQueryContext, NCQueryResult]() {
                            override def apply(t: NCQueryContext): NCQueryResult = null
                        }
                    )
                    .build()
    
            /**
              * Gets the list, potentially empty, of deployment descriptors supported by this provider.
              *
              * @return List of deployment descriptor.
              */
            override def getDescriptors: JList[NCModelDescriptor] = Collections.singletonList(ds)
        }
}
