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

import org.nlpcraft.*;
import org.nlpcraft.examples.misc.geo.cities.*;
import org.nlpcraft.examples.misc.geo.keycdn.GeoManager;
import org.nlpcraft.examples.misc.geo.keycdn.beans.GeoDataBean;
import org.nlpcraft.mdllib.*;
import org.nlpcraft.mdllib.intent.*;
import org.nlpcraft.mdllib.intent.NCIntentSolver.*;
import org.nlpcraft.mdllib.tools.builder.*;
import org.apache.commons.lang3.text.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

import static org.nlpcraft.mdllib.utils.NCTokenUtils.*;
import static java.time.format.FormatStyle.*;

/**
 * Time example model provider.
 * <p>
 * This example answers the questions about current time, either local or at some city.
 * It provides HTML response with time and timezone information as well as Google map
 * of the location (default or provided by the user).
 */
public class TimeProvider extends NCModelProviderAdapter {
    // Medium data formatter.
    static private final DateTimeFormatter FMT = DateTimeFormatter.ofLocalizedDateTime(MEDIUM);

    // Map of cities and their geo and timezone information.
    static private final Map<City, CityData> citiesData = CitiesDataProvider.get();
    
    // Geo manager.
    static private final GeoManager geoMrg = new GeoManager();

    /**
     * Gets multipart query result.
     *
     * @param city Detected city.
     * @param cntry Detected country.
     * @param tmz Timezone ID.
     * @param lat City latitude.
     * @param lon City longitude.
     */
    private static NCQueryResult formatResult(String city, String cntry, String tmz, double lat, double lon) {
        String cityFmt = WordUtils.capitalize(city);
        String cntrFmt = WordUtils.capitalize(cntry);

        String css1 = "style='display: inline-block; min-width: 100px'";
        String css2 = "style='font-weight: 200'";

        // Multipart result consists of HTML fragment and Google static map fragment.
        return NCQueryResult.jsonMultipart(
            // HTML block with CSS formatting.
            NCQueryResult.html(
                String.format(
                    "<b %s>Time:</b> <span style='color: #F1C40F'>%s</span><br/>" +
                    "<b %s>City:</b> <span %s>%s</span><br/>" +
                    "<b %s>Country:</b> <span %s>%s</span><br/>" +
                    "<b %s>Timezone:</b> <span %s>%s</span><br/>" +
                    "<b %s>Local Time:</b> <span %s>%s</span>",
                    css1, ZonedDateTime.now(ZoneId.of(tmz)).format(FMT),
                    css1, css2, cityFmt,
                    css1, css2, cntrFmt,
                    css1, css2, tmz,
                    css1, css2, ZonedDateTime.now().format(FMT)
                )
            ),
            // Google static map fragment.
            NCQueryResult.jsonGmap(
                String.format(
                    "{" +
                        "\"cssStyle\": {" +
                            "\"width\": \"600px\", " +
                            "\"height\": \"300px\"" +
                        "}," +
                        "\"gmap\": {" +
                            "\"center\": \"%f,%f\"," +
                            "\"zoom\": 4," +
                            "\"scale\": 2," +
                            "\"size\": \"600x300\", " +
                            "\"maptype\": \"terrain\", " +
                            "\"markers\": \"color:red|%f,%f\"" +
                        "}" +
                    "}",
                    lat,
                    lon,
                    lat,
                    lon
                )
            )
        );
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
    
        return formatResult(
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
            return formatResult(city, cntry, data.getTimezone(), data.getLatitude(), data.getLongitude());
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
    public TimeProvider() throws NCException {
        String path = "modules/examples/src/main/java/org/nlpcraft/examples/time/time_model.json";

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
        setup(NCModelBuilder.newJsonModel(path).setQueryFunction(solver::solve).build());
    }
}
