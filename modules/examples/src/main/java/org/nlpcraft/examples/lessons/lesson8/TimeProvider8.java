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

package org.nlpcraft.examples.lessons.lesson8;

import org.nlpcraft.*;
import org.nlpcraft.examples.misc.geo.cities.*;
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
 * `Lesson 8` model provider.
 */
@NCActiveModelProvider
public class TimeProvider8 extends NCModelProviderAdapter {
    static private Map<City, CityData> citiesData = CitiesDataProvider.get();

    // Medium data formatter.
    static private final DateTimeFormatter FMT = DateTimeFormatter.ofLocalizedDateTime(MEDIUM);

    // CSS formatting styles.
    static private final String CSS1= "style='display: inline-block; min-width: 100px'";
    static private final String CSS2 = "style='font-weight: 200'";

    /**
     * Gets formatted query result.
     *
     * @param city Detected city.
     * @param cntry Detected country.
     * @param tmz Timezone ID.
     * @param lat City latitude.
     * @param lon City longitude.
     */
    private static NCQueryResult formatResult(
        String city,
        String cntry,
        String tmz,
        double lat,
        double lon
    ) {
        String cityFmt = WordUtils.capitalize(city);
        String cntrFmt = WordUtils.capitalize(cntry);

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
                    CSS1, ZonedDateTime.now(ZoneId.of(tmz)).format(FMT),
                    CSS1, CSS2, cityFmt,
                    CSS1, CSS2, cntrFmt,
                    CSS1, CSS2, tmz,
                    CSS1, CSS2, ZonedDateTime.now().format(FMT)
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
     * Callback on local intent match.
     *
     * @param ctx Token solver context.
     * @return Query result.
     */
    private NCQueryResult onLocalMatch(NCIntentSolverContext ctx) {
        NCSentence sen = ctx.getQueryContext().getSentence();

        // Get local geo data from sentence metadata defaulting to
        // Silicon Valley location in case we are missing that info.
        return formatResult(
            sen.getCityName().orElse(""),
            sen.getCountryName().orElse("United States"),
            sen.getTimezoneName().orElse("America/Los_Angeles"),
            sen.getLatitude().orElse(37.7749),
            sen.getLongitude().orElse(122.4194)
        );
    }

    /**
     * Callback on remote intent match.
     *
     * @param ctx Token solver context.
     * @return Query result.
     */
    private NCQueryResult onRemoteMatch(NCIntentSolverContext ctx) {
        // Note that 'dl:geo' is mandatory token in this example and only one is allowed.
        NCToken geoTok = ctx.getIntentTokens().get(1).get(0);

        // Country and city are is mandatory metadata of 'dl:geo' token.
        String city = getGeoCity(geoTok);
        String cntry = getGeoCountry(geoTok);

        CityData data = citiesData.get(new City(city, cntry));

        // We don't have timezone mapping for parsed GEO location.
        if (data == null)
            // Ask for human curation.
            throw new NCCuration(String.format("No timezone mapping for %s, %s.", city, cntry));

        return formatResult(
            city,
            cntry,
            data.getTimezone(),
            data.getLatitude(),
            data.getLongitude()
        );
    }

    /**
     * Initializes provider.
     *
     * @throws NCException If any errors occur.
     */
    TimeProvider8() throws NCException {
        String path = NCModelBuilder.classPathFile("lessons/time_model8.json");

        NCIntentSolver solver =
            new NCIntentSolver(
                "time-solver",
                () -> {
                    // Custom not-found function with tailored rejection message.
                    throw new NCCuration("Input is ambiguous.");
                }
            );

        // Check for exactly one 'x:time' token **without** looking into conversation context.
        // That's an indication of asking for local time only.
        solver.addIntent(
            new NON_CONV_INTENT("time", "id == x:time", 1, 1),
            this::onLocalMatch
        );

        // Check for exactly one 'x:time' token and one 'dl:geo' token.
        solver.addIntent(
            new CONV_INTENT( // --=== THIS IS CHANGED FROM PREVIOUS EXAMPLE ===---
                "c^time|city",
                new TERM("id == x:time", 1, 1),
                new TERM(
                    new AND(
                        "id == nlp:geo",
                        "~GEO_KIND == CITY"
                    ),
                    1, // --=== THIS IS CHANGED FROM ZERO IN PREVIOUS EXAMPLE ===---
                    1
                )
            ),
            this::onRemoteMatch
        );

        NCModel model = NCModelBuilder.newJsonModel(path).setQueryFunction(solver::solve).build();

        // Initialize adapter.
        setup(model);
    }
}
