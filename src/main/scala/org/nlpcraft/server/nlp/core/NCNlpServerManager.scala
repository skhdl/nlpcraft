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

package org.nlpcraft.server.nlp.core

import org.nlpcraft.common.nlp.core.NCNlpCoreManager
import org.nlpcraft.common.{NCE, NCLifecycle}

import scala.reflect.runtime.universe._

/**
  * Server NLP manager.
  */
object NCNlpServerManager extends NCLifecycle("Server NLP manager") {
    @volatile var parser: NCNlpParser = _

    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        val mirror = runtimeMirror(getClass.getClassLoader)

        def mkInstance(name: String): NCNlpParser =
            try
                mirror.reflectModule(mirror.staticModule(name)).instance.asInstanceOf[NCNlpParser]
            catch {
                case e: Throwable ⇒ throw new NCE(s"Error initializing class: $name", e)
            }

        parser =
            NCNlpCoreManager.getEngine match {
                case "stanford" ⇒ mkInstance("org.nlpcraft.server.nlp.core.stanford.NCStanfordParser")
                // NCOpenNlpParser added via reflection just for symmetry.
                case "opennlp" ⇒ mkInstance("org.nlpcraft.server.nlp.core.opennlp.NCOpenNlpParser")

                case _ ⇒ throw new AssertionError(s"Unexpected engine: ${NCNlpCoreManager.getEngine}")
            }

        parser.start()

        super.start()
    }

    /**
      * Parses given sentence.
      *
      * @param sen Sentence text.
      * @return Parsed tokens.
      */
    def parse(sen: String): Seq[NCNlpWord] = {
        ensureStarted()

        parser.parse(sen)
    }
}
