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

package org.nlpcraft.mdllib.intent.impl

import java.util.{List ⇒ JList}

import org.nlpcraft.mdllib.intent.NCIntentSolver.IntentCallback
import org.nlpcraft.mdllib._

/**
  * Intent solver engine result. Using basic case class for easier Java interop.
  */
case class NCIntentSolverResult(
    intentId: String,
    fn: IntentCallback, // For Java interop.
    toks: JList[JList[NCToken]], // For Java interop.
    termNouns: JList[String], // For Java interop.
    isExactMatch: Boolean,
    variant: NCVariant
)