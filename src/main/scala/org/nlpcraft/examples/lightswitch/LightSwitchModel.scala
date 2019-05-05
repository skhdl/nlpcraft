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

package org.nlpcraft.examples.lightswitch

import java.util.Optional

import org.nlpcraft.model._
import org.nlpcraft.common._
import org.nlpcraft.model.builder.NCModelBuilder
import org.nlpcraft.model.intent.{NCIntentSolver, NCIntentSolverContext}
import org.nlpcraft.model.intent.NCIntentSolver._
import org.nlpcraft.model.utils.NCTokenUtils._

import scala.collection.JavaConverters._

/**
  * This example provides very simple implementation for NLI-powered light switch.
  * You can say something like this:
  * <ul>
  *     <li><code>Turn the lights off in the entire house.</code></li>
  *     <li><code>Switch on the illumination in the master bedroom closet.</code></li>
  * </ul>
  * You can easily modify intent callbacks to perform the actual light switching using
  * HomeKit or Arduino-based controllers.
  */
class LightSwitchModel extends NCModelProviderAdapter {
    val solver = new NCIntentSolver
    
    // Add two intents for on and off operations.
    // Each intent expects one action ('ls:action-on' or 'ls:action-off' token)
    // and the optional set of locations (defaults to the entire house if empty).
    
    solver.addIntent(
        new CONV_INTENT(
            "on-intent",
            new TERM("id == ls:action-on", 1, 1), // Term #1 (index=0).
            new TERM("id == ls:location", 0, 10)  // Term #2 (index=1).
        ),
        onMatch(_: NCIntentSolverContext, true)
    )
    solver.addIntent(
        new CONV_INTENT(
            "off-intent",
            new TERM("id == ls:action-off", 1, 1), // Term #1 (index=0).
            new TERM("id == ls:location", 0, 10)   // Term #2 (index=1).
        ),
        onMatch(_: NCIntentSolverContext, false)
    )
    
    /**
      * Intent matching callback.
      *
      * @param ctx Intent solver context with specifics of the winning match.
      * @param onOff On or off operation.
      * @return Query result to be sent to the REST caller.
      */
    def onMatch(ctx: NCIntentSolverContext, onOff: Boolean): NCQueryResult = {
        // Get all the tokens from the winning intent for the locations.
        val locationToks = ctx.getIntentTokens.get(1).asScala // Term #2 (index=1).
        
        val status = if (onOff) "on" else "off"
        val location =
            if (locationToks == null || locationToks.isEmpty)
                // Default to the entire house.
                "entire house"
            else
                // Make a comma-separate list of all the locations.
                locationToks.map(getOriginalText).mkString(", ")
        
        NCQueryResult.text(s"Lights '$status' in '${location.toLowerCase}'.")
    }
    
    setup(
        // Read the static configuration from YAML file in the same directory.
        NCModelBuilder.newYamlModel(
            classOf[LightSwitchModel].getClassLoader.
                getResourceAsStream("org/nlpcraft/examples/lightswitch/lightswitch_model.yaml")
        )
        .setSolver(solver)
        .build
    )
}
