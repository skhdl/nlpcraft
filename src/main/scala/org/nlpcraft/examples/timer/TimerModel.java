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

package org.nlpcraft.examples.timer;

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
import org.nlpcraft.mdllib.utils.NCTokenUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.nlpcraft.mdllib.utils.NCTokenUtils.getNumFrom;
import static org.nlpcraft.mdllib.utils.NCTokenUtils.getNumUnit;

/**
 * Timer example model provider.
 * <p>
 * This example provides a simple "alarm clock" interface where you can ask to set the timer
 * for a specific duration from now expressed in hours, minutes and/or seconds. You can say "ping me in 3 minutes",
 * "buzz me in an hour and 15 minutes", or "set my alarm for 30 secs". When the timers is up it will
 * simply print out "BEEP BEEP BEEP" in the probe console.
 * <p>
 * As an additional exercise you can quickly add support for settings the alarm to a specific
 * time (and not only for a duration) and can play with the way the system reacts when the timer is up.
 */
public class TimerModel extends NCModelProviderAdapter {
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("HH'h' mm'm' ss's'").withZone(ZoneId.systemDefault());
    
    private final Timer timer = new Timer();
    
    public TimerModel() {
        NCIntentSolver solver = new NCIntentSolver();

        // Add a wide-catch intent. Note that terms in the intent will be matched
        // in any order and this intent can match some unusual grammar input
        // like "2 secs and 3mins set the timer". For the sake of simplicity
        // we allow such idiosyncratic input.
        solver.addIntent(
            new NON_CONV_INTENT(
                "timer|num{1+}",
                new TERM("id == x:timer", 1, 1),
                new TERM(
                    new AND("id == nlp:num", "~NUM_UNITTYPE == datetime", "~NUM_ISEQUALCONDITION == true"),
                    0,
                    7 // Up to 7 numeric `datetime` units.
                )
            ),
            this::onMatch
        );
    
        setup(NCModelBuilder.newJsonModel(
            "src/main/scala/org/nlpcraft/examples/timer/timer_model.json").
            setQueryFunction(solver::solve).build());
    }
    
    /**
     * Callback on intent match.
     *
     * @param ctx Token solver context.
     * @return Query result.
     */
    private NCQueryResult onMatch(NCIntentSolverContext ctx) {
        if (!ctx.isExactMatch())
            throw new NCRejection("Not exact match.");
        
        List<NCToken> nums = ctx.getIntentTokens().get(1);
    
        long unitsCnt = nums.stream().map(NCTokenUtils::getNumUnit).distinct().count();
        
        if (unitsCnt != nums.size())
            throw new NCRejection("Ambiguous time units.");
    
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dt = now;
    
        for (NCToken num : nums) {
            String unit = getNumUnit(num);
    
            // Skips possible fractional to simplify.
            long v = (long)getNumFrom(num);
            
            if (v <= 0)
                throw new NCRejection("Value must be positive: " + unit);
    
            switch (unit) {
                case "second": { dt = dt.plusSeconds(v); break; }
                case "minute": { dt = dt.plusMinutes(v); break; }
                case "hour": { dt = dt.plusHours(v); break; }
                case "day": { dt = dt.plusDays(v); break; }
                case "week": { dt = dt.plusWeeks(v); break; }
                case "month": { dt = dt.plusMonths(v); break; }
                case "year": { dt = dt.plusYears(v); break; }
        
                default:
                    // It shouldn't be assert, because 'datetime' unit can be extended.
                    throw new NCRejection("Unsupported time unit: " + unit);
            }
        }
    
        long ms = now.until(dt, MILLIS);
        
        assert ms >= 0;
    
        timer.schedule(
            new TimerTask() {
                @Override
                public void run() {
                    System.out.println(
                        "BEEP BEEP BEEP for: " + ctx.getQueryContext().getSentence().getNormalizedText() + ""
                    );
                }
            },
            ms
        );
    
        return NCQueryResult.text("Timer set for: " + FMT.format(dt));
    }
}
