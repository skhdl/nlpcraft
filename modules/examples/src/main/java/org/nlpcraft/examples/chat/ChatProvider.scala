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

package org.nlpcraft.examples.chat

import org.nlpcraft.mdllib._
import org.nlpcraft.mdllib.tools.builder.NCModelBuilder
import org.nlpcraft.mdllib.tools.scala.NCScalaSupport._ /* NOTE: IDEA wrongly marks it as unused. */

/**
  * Chat example model provider.
  * 
  * This model sends any user requests to curation allowing human Linguist to "chat"
  * with user via talkback, curation or rejection functionality. This is primarily for
  * easy demonstration of Linguist operations.
  */
@NCActiveModelProvider
class ChatProvider extends NCModelProviderAdapter {
    // Any immutable user defined ID.
    private final val MODEL_ID = "dl.chat.ex"
    
    // Setting up provider adapter.
    setup(
        // Using inline JSON model.
        NCModelBuilder.newJsonStringModel(
            // Hand-rolled JSON for simplicity...
            s"""
               | {
               |    "id": "$MODEL_ID",
               |    "name": "Chat Example Model",
               |    "version": "1.0",
               |    "description": "Chat example model.",
               |    "vendorName": "NLPCraft",
               |    "vendorUrl": "https://www.nlpcraft.org",
               |    "vendorContact": "Support",
               |    "vendorEmail": "info@nlpcraft.org",
               |    "docsUrl": "https://www.nlpcraft.org",
               |    "allowNoUserTokens": true,
               |    "examples": [
               |        "Hey, any plans this evening?",
               |        "Wanna see Warriors tonight?"
               |    ],
               |    "defaultTrivia": "true"
               | }
            """.stripMargin)
            // Query function sends any user input to curation.
            .setQueryFunction((_: NCQueryContext) ⇒ { throw new NCCuration("Please curate!") }
        )
        .build()
    )
}