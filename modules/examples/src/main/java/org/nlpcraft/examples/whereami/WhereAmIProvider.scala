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