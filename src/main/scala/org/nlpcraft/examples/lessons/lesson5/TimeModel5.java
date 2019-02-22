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

package org.nlpcraft.examples.lessons.lesson5;

import org.nlpcraft.NCException;
import org.nlpcraft.examples.lessons.utils.LessonsUtils;
import org.nlpcraft.examples.misc.geo.cities.CitiesDataProvider;
import org.nlpcraft.examples.misc.geo.cities.City;
import org.nlpcraft.examples.misc.geo.cities.CityData;
import org.nlpcraft.examples.misc.geo.keycdn.GeoManager;
import org.nlpcraft.examples.misc.geo.keycdn.beans.GeoDataBean;
import org.nlpcraft.mdllib.NCModel;
import org.nlpcraft.mdllib.NCModelProviderAdapter;
import org.nlpcraft.mdllib.NCQueryResult;
import org.nlpcraft.mdllib.NCRejection;
import org.nlpcraft.mdllib.NCToken;
import org.nlpcraft.mdllib.intent.NCIntentSolver;
import org.nlpcraft.mdllib.intent.NCIntentSolver.AND;
import org.nlpcraft.mdllib.intent.NCIntentSolver.NON_CONV_INTENT;
import org.nlpcraft.mdllib.intent.NCIntentSolver.TERM;
import org.nlpcraft.mdllib.intent.NCIntentSolverContext;
import org.nlpcraft.mdllib.tools.builder.NCModelBuilder;

import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static org.nlpcraft.mdllib.utils.NCTokenUtils.getGeoCity;
import static org.nlpcraft.mdllib.utils.NCTokenUtils.getGeoCountry;

/**
 * `Lesson 5` model provider.
 */
public class TimeModel5 extends NCModelProviderAdapter {
    static private final Map<City, CityData> citiesData = CitiesDataProvider.get();
    // Geo manager.
    static private final GeoManager geoMrg = new GeoManager();

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
        if (ctx.getIntentTokens().get(1).isEmpty()) {
            Optional<GeoDataBean> geoOpt = geoMrg.get(ctx.getQueryContext().getSentence());
    
            // Get user's timezone from sentence metadata.
            return formatResult(geoOpt.isPresent() ? geoOpt.get().getTimezoneName() : "America/Los_Angeles");
        }

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
    TimeModel5() throws NCException {
        String path = "src/main/scala/org/nlpcraft/examples/lessons/lesson5/time_model5.json";

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
