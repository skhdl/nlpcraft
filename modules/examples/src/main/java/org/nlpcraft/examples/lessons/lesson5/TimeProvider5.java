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

package org.nlpcraft.examples.lessons.lesson5;

import org.nlpcraft.NCException;
import org.nlpcraft.examples.lessons.utils.LessonsUtils;
import org.nlpcraft.examples.misc.geo.cities.CitiesDataProvider;
import org.nlpcraft.examples.misc.geo.cities.City;
import org.nlpcraft.examples.misc.geo.cities.CityData;
import org.nlpcraft.mdllib.*;
import org.nlpcraft.mdllib.intent.*;
import org.nlpcraft.mdllib.intent.NCIntentSolver.AND;
import org.nlpcraft.mdllib.intent.NCIntentSolver.NON_CONV_INTENT;
import org.nlpcraft.mdllib.intent.NCIntentSolver.TERM;
import org.nlpcraft.mdllib.tools.builder.NCModelBuilder;

import java.time.ZoneId;
import java.util.Map;
import static org.nlpcraft.mdllib.utils.NCTokenUtils.*;

/**
 * `Lesson 5` model provider.
 */
@NCActiveModelProvider
public class TimeProvider5 extends NCModelProviderAdapter {
    static private Map<City, CityData> citiesData = CitiesDataProvider.get();

    /**
     * Gets formatted query result.
     *
     * @param tmz Timezone ID.
     */
    private static NCQueryResult formatResult(String tmz) {
        return NCQueryResult.text(LessonsUtils.now(ZoneId.of(tmz)));
    }
    
    /**
     * Callback on intent match.
     *
     * @param ctx Token solver context.
     * @return Query result.
     */
    private NCQueryResult onMatch(NCIntentSolverContext ctx) {
        // 'nlp:geo' is optional here.
        if (ctx.getIntentTokens().get(1).isEmpty())
            // Get user's timezone from sentence metadata.
            return formatResult(
                ctx.getQueryContext().getSentence().getTimezoneName().orElse("America/Los_Angeles")
            );

        // Note that only one 'nlp:geo' token is allowed per model metadata.
        NCToken geoTok = ctx.getIntentTokens().get(1).get(0);

        // Country and city are is mandatory metadata of 'nlp:geo' token.
        String city = getGeoCity(geoTok);
        String cntry = getGeoCountry(geoTok);

        CityData data = citiesData.get(new City(city, cntry));

        // We don't have timezone mapping for parsed GEO location.
        if (data == null)
            throw new NCRejection(String.format("No timezone mapping for %s, %s.", city, cntry));

        return formatResult(data.getTimezone());
    }

    /**
     * Initializes provider.
     *
     * @throws NCException If any errors occur.
     */
    TimeProvider5() throws NCException {
        String path = NCModelBuilder.classPathFile("lessons/time_model5.json");

        NCIntentSolver solver =
            new NCIntentSolver(
                "time-solver",
                () -> {
                    // Custom not-found function with tailored rejection message.
                    throw new NCRejection("I can't understand your question.");
                }
            );

        // Check for exactly one 'x:time' token. 'nlp:geo' is optional.
        solver.addIntent(
            new NON_CONV_INTENT(
                "time|city?",
                new TERM("id == x:time", 1, 1),
                new TERM(
                    new AND(
                        "id == nlp:geo",
                        "~GEO_KIND == CITY"
                    ),
                    0,
                    1
                )
            ),
            this::onMatch
        );

        NCModel model = NCModelBuilder.newJsonModel(path).setQueryFunction(solver::solve).build();

        // Initialize adapter.
        setup(model);
    }
}
