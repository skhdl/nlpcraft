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

package org.nlpcraft.model.tools.builder

import org.nlpcraft.common._
import org.nlpcraft.common.NCException
import org.nlpcraft.model._
import org.scalatest.FlatSpec

/**
  * Model builder test.
  */
class NCModelBuilderSpec extends FlatSpec with NCModelSpecBase {
    behavior of "model"
    
    it should "properly build a valid model" in {
        try {
            println(makeValidModelProvider.makeModel(""))
            
            assert(true)
        }
        catch {
            case e: NCException ⇒ e.printStackTrace(); assert(false)
        }
    }

    it should "load model from JSON file" in {
        val mdl = NCModelBuilder.newJsonModel(U.getStream("time_model.json"))

        println(s"Model loaded: $mdl")
    }
}
