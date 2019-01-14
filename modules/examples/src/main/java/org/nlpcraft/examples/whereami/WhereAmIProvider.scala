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
 * Licensor:    Copyright (C) 2018 DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.examples.whereami

import org.nlpcraft.mdllib.intent.NCIntentSolver._
import org.nlpcraft.mdllib._
import org.nlpcraft.mdllib.intent.{NCIntentSolver, NCIntentSolverContext}
import org.nlpcraft.mdllib.tools.builder.NCModelBuilder
import org.nlpcraft.mdllib.tools.scala.NCScalaSupport._

/**
  * "Where Am I" example model provider.
  * <p>
  * Simple Scala example that answers "where am I" question about user's
  * current location responding with a Google map.
  */
@NCActiveModelProvider
class WhereAmIProvider extends NCModelProviderAdapter {
    /**
      * Callback on matching intent.
      *
      * @param ctx Token solver context.
      * @return Static Google map query result.
      */
    private def onMatch(ctx: NCIntentSolverContext): NCQueryResult = {
        val sen = ctx.getQueryContext.getSentence
        
        // Default to some Silicon Valley location in case user's coordinates
        // cannot be determines.
        val lat = sen.getLatitude.orElse(37.7749)
        val lon = sen.getLongitude.orElse(122.4194)
    
        NCQueryResult.jsonGmap(
            s"""
                |{
                |   "cssStyle": {
                |        "width": "600px", 
                |        "height": "300px"
                |   },
                |   "gmap": {
                |        "center": "$lat,$lon",
                |        "zoom": 14,
                |        "scale": 2,
                |        "size": "600x300",
                |        "maptype": "terrain",
                |        "markers": "color:red|$lat,$lon"
                |    }
                |}
            """.stripMargin
        )
    }
    
    private val solver = new NCIntentSolver().addIntent(
        new CONV_INTENT(
            "where",
            "id == wai:where", 1, 1
        ),
        onMatch _ // Callback on match.
    )
    
    setup(
        NCModelBuilder.newJsonModel(NCModelBuilder.classPathFile("whereami_model.json")).
            setQueryFunction(solver.solve _).
            build()
    )
}