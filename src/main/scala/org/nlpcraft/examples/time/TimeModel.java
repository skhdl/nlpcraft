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

package org.nlpcraft.examples.time;

import com.google.gson.Gson;
import org.apache.commons.lang3.text.WordUtils;
import org.nlpcraft.common.NCException;
import org.nlpcraft.examples.misc.geo.cities.CitiesDataProvider;
import org.nlpcraft.examples.misc.geo.cities.City;
import org.nlpcraft.examples.misc.geo.cities.CityData;
import org.nlpcraft.examples.misc.geo.keycdn.GeoManager;
import org.nlpcraft.examples.misc.geo.keycdn.beans.GeoDataBean;
import org.nlpcraft.model.NCModelProviderAdapter;
import org.nlpcraft.model.NCQueryResult;
import org.nlpcraft.model.NCRejection;
import org.nlpcraft.model.NCToken;
import org.nlpcraft.model.builder.NCModelBuilder;
import org.nlpcraft.model.intent.NCIntentSolver;
import org.nlpcraft.model.intent.NCIntentSolver.AND;
import org.nlpcraft.model.intent.NCIntentSolver.CONV_INTENT;
import org.nlpcraft.model.intent.NCIntentSolver.NON_CONV_INTENT;
import org.nlpcraft.model.intent.NCIntentSolver.TERM;
import org.nlpcraft.model.intent.NCIntentSolverContext;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.time.format.FormatStyle.MEDIUM;
import static org.nlpcraft.model.utils.NCTokenUtils.getGeoCity;
import static org.nlpcraft.model.utils.NCTokenUtils.getGeoCountry;

/**
 * Time example model provider.
 * <p>
 * This example answers the questions about current time, either local or at some city.
 * It provides HTML response with time and timezone information as well as Google map
 * of the location (default or provided by the user).
 */
public class TimeModel extends NCModelProviderAdapter {
    // Medium data formatter.
    static private final DateTimeFormatter FMT = DateTimeFormatter.ofLocalizedDateTime(MEDIUM);

    // Map of cities and their geo and timezone information.
    static private final Map<City, CityData> citiesData = CitiesDataProvider.get();
    
    // Geo manager.
    static private final GeoManager geoMrg = new GeoManager();
    
    static private final Gson gson = new Gson();

    /**
     * Gets JSON query result.
     *
     * @param city Detected city.
     * @param cntry Detected country.
     * @param tmz Timezone ID.
     * @param lat City latitude.
     * @param lon City longitude.
     */
    private static NCQueryResult mkResult(String city, String cntry, String tmz, double lat, double lon) {
        Map<String, Object> res = new HashMap<>();
        
        res.put("city", WordUtils.capitalize(city));
        res.put("country", WordUtils.capitalize(cntry));
        res.put("timezone", tmz);
        res.put("lat", lat);
        res.put("lon", lon);
        res.put("localTime", ZonedDateTime.now(ZoneId.of(tmz)).format(FMT));
    
        return NCQueryResult.json(gson.toJson(res));
    }

    /**
     * Callback on local time intent match.
     *
     * @param ctx Token solver context.
     * @return Query result.
     */
    private NCQueryResult onLocalMatch(NCIntentSolverContext ctx) {
        if (!ctx.isExactMatch())
            throw new NCRejection("Not exact match.");
        
        Optional<GeoDataBean> geoOpt = geoMrg.get(ctx.getQueryContext().getSentence());
        
        // Get local geo data from sentence metadata defaulting to
        // Silicon Valley location in case we are missing that info.
        GeoDataBean geo = geoOpt.orElseGet(geoMrg::getSiliconValley);
    
        return mkResult(
            geo.getCityName(),
            geo.getCountryName(),
            geo.getTimezoneName(),
            geo.getLatitude(),
            geo.getLongitude()
        );
    }

    /**
     * Callback on remote time intent match.
     *
     * @param ctx Token solver context.
     * @return Query result.
     */
    private NCQueryResult onRemoteMatch(NCIntentSolverContext ctx) {
        // 'nlp:geo' is mandatory.
        // Only one 'nlp:geo' token is allowed, so we don't have to check for it.
        NCToken geoTok = ctx.getIntentTokens().get(1).get(0);

        // Country and city are is mandatory metadata of 'nlp:geo' token.
        String city = getGeoCity(geoTok);
        String cntry = getGeoCountry(geoTok);

        CityData data = citiesData.get(new City(city, cntry));

        if (data != null)
            return mkResult(city, cntry, data.getTimezone(), data.getLatitude(), data.getLongitude());
        else
            // We don't have timezone mapping for parsed GEO location.
            // Instead of defaulting to a local time - we reject with a specific error message for cleaner UX.
            throw new NCRejection(String.format("No timezone mapping for %s, %s.", city, cntry));
    }

    /**
     * Initializes provider.
     *
     * @throws NCException If any errors occur.
     */
    public TimeModel() throws NCException {
        NCIntentSolver solver = new NCIntentSolver(
            "time-solver",
            // Custom not-found function with tailored rejection message.
            () -> { throw new NCRejection("Are you asking about time?<br>Check spelling and city name too."); }
        );

        // NOTE:
        // We need to have two intents vs. one intent with an optional GEO. The reason is that
        // first intent isn't using the conversation context to make sure we can always ask
        // for local time **no matter** what was asked before... Note that non-conversational
        // intent always "wins" over the conversational one given otherwise equal weight because
        // non-conversational intent is more specific (i.e. using only the most current user input).

        // Check for exactly one 'x:time' token **without** looking into conversation context.
        // That's an indication of asking for local time only.
        solver.addIntent(
            new NON_CONV_INTENT("time", "id == x:time", 1, 1), // Term idx=0.
            this::onLocalMatch
        );

        // Check for exactly one 'x:time' and one 'nlp:geo' CITY token **including** conversation
        // context. This is always remote time.
        solver.addIntent(
            new CONV_INTENT(
                "c^time|city",
                new TERM("id == x:time", 1, 1), // Term idx=0.
                new TERM(
                    new AND(                    // Term idx=1.
                        "id == nlp:geo",
                        // GEO locations can only be city (we can't get time for country or region or continent).
                        "~GEO_KIND == CITY"
                    ), 1, 1
                )
            ),
            this::onRemoteMatch
        );

        // Initialize adapter.
        setup(
            NCModelBuilder.
                newJsonModel(TimeModel.class.
                    getClassLoader().
                    getResourceAsStream("org/nlpcraft/examples/time/time_model.json")
                ).setQueryFunction(solver::solve).build()
        );
    }
}
