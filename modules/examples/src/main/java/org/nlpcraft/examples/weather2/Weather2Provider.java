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

package org.nlpcraft.examples.weather2;

import com.google.gson.Gson;
import org.apache.commons.lang3.tuple.Pair;
import org.nlpcraft.examples.misc.apixu.ApixuPeriodException;
import org.nlpcraft.examples.misc.apixu.ApixuWeatherService;
import org.nlpcraft.examples.misc.apixu.beans.Current;
import org.nlpcraft.examples.misc.apixu.beans.CurrentResponse;
import org.nlpcraft.examples.misc.apixu.beans.Location;
import org.nlpcraft.examples.misc.apixu.beans.RangeResponse;
import org.nlpcraft.examples.misc.geo.keycdn.GeoManager;
import org.nlpcraft.examples.misc.geo.keycdn.beans.GeoDataBean;
import org.nlpcraft.mdllib.NCActiveModelProvider;
import org.nlpcraft.mdllib.NCModelProviderAdapter;
import org.nlpcraft.mdllib.NCQueryResult;
import org.nlpcraft.mdllib.NCRejection;
import org.nlpcraft.mdllib.NCToken;
import org.nlpcraft.mdllib.intent.NCIntentSolver;
import org.nlpcraft.mdllib.intent.NCIntentSolver.AND;
import org.nlpcraft.mdllib.intent.NCIntentSolver.CONV_INTENT;
import org.nlpcraft.mdllib.intent.NCIntentSolver.INTENT;
import org.nlpcraft.mdllib.intent.NCIntentSolver.TERM;
import org.nlpcraft.mdllib.intent.NCIntentSolverContext;
import org.nlpcraft.mdllib.tools.builder.NCModelBuilder;
import org.nlpcraft.mdllib.utils.NCTokenUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.nlpcraft.mdllib.utils.NCTokenUtils.*;

/**
 * Weather example model provider.
 * <p>
 * This is relatively complete weather service with JSON output and a non-trivial
 * intent matching logic. It uses https://www.apixu.com REST service for the actual
 * weather information.
 * <p>
 * Note that this class is mostly identical to {@link org.nlpcraft.examples.weather.WeatherProvider}
 * except for the output formatting. This implementation uses JSON. Note also that it also returns intent ID
 * together with execution result which can be used in testing.
 */
@SuppressWarnings("Duplicates")
@NCActiveModelProvider
public class Weather2Provider extends NCModelProviderAdapter {
    // It is demo token and its usage has some restrictions (history data contains one day only, etc).
    // Please register your own account at https://www.apixu.com/pricing.aspx and
    // replace this demo token with your own.
    private final ApixuWeatherService srv = new ApixuWeatherService("3f9b7de2d3894eb6b27150825171909");

    // Geo manager.
    private final GeoManager geoMrg = new GeoManager();
    
    private static final Gson gson = new Gson();
    
    // Maximum free words left before rejection.
    private static final int MAX_FREE_WORDS = 4;
    
    // Keywords for 'local' weather.
    private static final Set<String> LOCAL_WORDS = new HashSet<>(Arrays.asList("my", "local", "hometown"));
    
    /**
     * A shortcut to convert millis to the local date object.
     *
     * @param ms Millis to convert.
     * @return Local date object.
     */
    private LocalDate toLocalDate(long ms) {
        return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * Makes JSON result for the given date range weather.
     *
     * @param res Weather holder for the range of dates.
     * @param intentId Intent ID.
     * @return Query result.
     */
    private NCQueryResult makeRangeResult(RangeResponse res, String intentId) {
        Location loc = res.getLocation();

        if (loc == null)
            throw new NCRejection("Weather service doesn't recognize this location.");
    
        return NCQueryResult.json(gson.toJson(new Weather2ResultWrapper<>(intentId, res)));
    }

    /**
     * Makes JSON result for a single date.
     *
     * @param res Weather holder for a single date.
     * @param intentId Intent ID.
     * @return Query result.
     */
    private NCQueryResult makeCurrentResult(CurrentResponse res, String intentId) {
        Location loc = res.getLocation();
        Current cur = res.getCurrent();

        if (loc == null)
            throw new NCRejection("Weather service doesn't recognize this location.");
        if (cur == null)
            throw new NCRejection("Weather service doesn't support this location.");
        
        return NCQueryResult.json(gson.toJson(new Weather2ResultWrapper<>(intentId, res)));
    }

    /**
     * Extracts date range from given solver context.
     *
     * @param ctx Solver context.
     * @return Pair of dates or {@code null} if not found.
     */
    private Pair<LocalDate, LocalDate> prepDate(NCIntentSolverContext ctx) {
        List<NCToken> toks = ctx.getIntentTokens().get(1);

        if (toks.size() > 1)
            throw new NCRejection("Only one date is supported.");

        if (toks.size() == 1) {
            NCToken tok = toks.get(0);

            return Pair.of(toLocalDate(getDateFrom(tok)), toLocalDate(getDateTo(tok)));
        }

        // No date found - return 'null'.
        return null;
    }

    /**
     * Extracts geo location (city) from given solver context that is suitable for APIXU service.
     *
     * @param ctx Solver context.
     * @return Geo location.
     */
    private String prepGeo(NCIntentSolverContext ctx) throws NCRejection {
        List<NCToken> geoToks = ctx.getIntentTokens().get(2); // Can be empty...
        List<NCToken> allToks = ctx.getVariant().getTokens();
    
        Optional<GeoDataBean> geoOpt = geoMrg.get(ctx.getQueryContext().getSentence());
        
        if (!geoOpt.isPresent())
            throw new NCRejection("City cannot be determined.");
    
        GeoDataBean geo = geoOpt.get();
    
        // Common lambda for getting current user city.
        Supplier<String> curCityFn = () -> {
            // Try current user location.
            // APIXU weather service understands this format too.
            return geo.getLatitude() + "," + geo.getLongitude();
        };

        // Manually process request for local weather. We need to separate between 'local Moscow weather'
        // and 'local weather' which are different. Basically, if there is word 'local/my/hometown' in the user
        // input and there is no city in the current sentence - this is a request for the weather at user's
        // current location, i.e. we should implicitly assume user's location and clear conversion context.
        // In all other cases - we take location from either current sentence or conversation STM.

        // NOTE: we don't do this separation on intent level as it is easier to do it here instead of
        // creating more intents with almost identical callbacks.

        boolean hasLocalWord = allToks.stream().anyMatch(t -> LOCAL_WORDS.contains(getNormalizedText(t)));

        if (hasLocalWord && geoToks.isEmpty()) {
            // Because we implicitly assume user's current city at this point we need to clear
            // 'nlp:geo' tokens from conversation context since they would no longer be valid.
            ctx.getQueryContext().getConversationContext().clear(NCTokenUtils::isGeo);

            // Return user current city.
            return curCityFn.get();
        }
        else
            return geoToks.size() == 1 ? getGeoCity(geoToks.get(0)) : curCityFn.get();
    }

    /**
     * Makes query result for given date range.
     *
     * @param ctx Token solver context.
     * @param from Default from date.
     * @param to Default to date.
     * @return Query result.
     */
    private NCQueryResult onRangeMatch(NCIntentSolverContext ctx, LocalDate from, LocalDate to) {
        Pair<LocalDate, LocalDate> date = prepDate(ctx);

        if (date == null)
            // If we don't have the date in the sentence or conversation STM - use default range.
            date = Pair.of(from, to);

        String geo = prepGeo(ctx);

        try {
            return makeRangeResult(srv.getWeather(geo, date), ctx.getIntentId());
        }
        catch (ApixuPeriodException e) {
            throw new NCRejection(e.getLocalizedMessage());
        }
    }

    /**
     * Strict check for an exact match (i.e. no dangling unused system or user defined tokens) and
     * maximum number of free words left unmatched. In both cases user input will be rejected.
     *
     * @param ctx Solver context.
     */
    private void checkMatch(NCIntentSolverContext ctx) {
        // Reject if intent match is not exact ("dangling" tokens remain) or too many free words left unmatched.
        if (!ctx.isExactMatch() || ctx.getVariant().getTokens().stream().filter(NCTokenUtils::isFreeWord).count() > MAX_FREE_WORDS)
            throw new NCRejection("Please simplify your request.");
    }

    /**
     * Callback on forecast intent match.
     *
     * @param ctx Token solver context.
     * @return Query result.
     */
    private NCQueryResult onForecastMatch(NCIntentSolverContext ctx) {
        checkMatch(ctx);
        
        try {
            // Look 5 days ahead by default.
            return onRangeMatch(ctx, LocalDate.now(), LocalDate.now().plusDays(5));
        }
        catch (NCRejection e) {
            throw e;
        }
        catch (Exception e) {
            throw new NCRejection("Weather provider error.", e);
        }
    }

    /**
     * Callback on history intent match.
     *
     * @param ctx Token solver context.
     * @return Query result.
     */
    private NCQueryResult onHistoryMatch(NCIntentSolverContext ctx) {
        checkMatch(ctx);

        try {
            // Look 5 days back by default.
            return onRangeMatch(ctx, LocalDate.now().minusDays(5), LocalDate.now());
        }
        catch (NCRejection e) {
            throw e;
        }
        catch (Exception e) {
            throw new NCRejection("Weather provider error.", e);
        }
    }

    /**
     * Callback on current date intent match.
     *
     * @param ctx Token solver context.
     * @return Query result.
     */
    private NCQueryResult onCurrentMatch(NCIntentSolverContext ctx) {
        checkMatch(ctx);

        try {
            Pair<LocalDate, LocalDate> date = prepDate(ctx);
            String geo = prepGeo(ctx);
    
            return date != null ?
                makeRangeResult(srv.getWeather(geo, date), ctx.getIntentId()) :
                makeCurrentResult(srv.getCurrentWeather(geo), ctx.getIntentId());
        }
        catch (ApixuPeriodException e) {
            throw new NCRejection(e.getLocalizedMessage());
        }
        catch (NCRejection e) {
            throw e;
        }
        catch (Exception e) {
            throw new NCRejection("Weather provider error.", e);
        }
    }

    /**
     * Shortcut for creating a conversational intent with given weather token.
     *
     * @param weatherTokId Specific weather token ID.
     * @return Newly created intent.
     */
    private INTENT mkIntent(String id, String weatherTokId) {
        return new CONV_INTENT(
            id,
            new TERM("id == " + weatherTokId, 1, 1),     // Index 0: mandatory 'weather' token.
            new TERM("id == nlp:date", 0, 1),           // Index 1: optional date.
            new TERM(                                  // Index 2: optional city.
                new AND("id == nlp:geo", "~GEO_KIND == CITY"),
                0, 1
            )
        );
    }

    /**
     * Initializes model provider.
     */
    Weather2Provider() {
        String modelPath = NCModelBuilder.classPathFile("weather_model2.json");

        // If no intent is matched respond with some helpful message...
        NCIntentSolver solver = new NCIntentSolver("solver", () -> {
            throw new NCRejection("Weather request is ambiguous.");
        });

        // Match exactly one of weather tokens and optional 'nlp:geo' and 'nlp:date' tokens.
        solver.addIntent(mkIntent("hist|date?|city?", "wt:hist"), this::onHistoryMatch);
        solver.addIntent(mkIntent("fcast|date?|city?", "wt:fcast"), this::onForecastMatch);
        solver.addIntent(mkIntent("curr|date?|city?", "wt:curr"), this::onCurrentMatch);

        setup(NCModelBuilder
            .newJsonModel(modelPath)
            .setQueryFunction(solver::solve)
            .build()
        );
    }
}
