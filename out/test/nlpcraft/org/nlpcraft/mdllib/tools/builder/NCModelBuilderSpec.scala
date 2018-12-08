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

package org.nlpcraft.mdllib.tools.builder

import org.nlpcraft.mdllib._
import org.nlpcraft._
import org.scalatest.FlatSpec

import scala.collection.JavaConverters._

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
        val mdl = NCModelBuilder.newJsonModel(G.getStream("time_model.json"))

        println(s"Model loaded: $mdl")
    }
}
