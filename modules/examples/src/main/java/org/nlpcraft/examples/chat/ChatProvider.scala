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
 * Licensor:    DataLingvo, Inc. https://www.datalingvo.com
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
    private final val MODEL_ID = "nlpcraft.chat.ex"
    
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
               |    "vendorName": "NlpCraft",
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