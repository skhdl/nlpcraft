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

package org.nlpcraft.examples.timer;

import org.nlpcraft.mdllib.*;
import org.nlpcraft.mdllib.intent.*;
import org.nlpcraft.mdllib.intent.NCIntentSolver.*;
import org.nlpcraft.mdllib.tools.builder.*;
import org.nlpcraft.mdllib.utils.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

import static org.nlpcraft.mdllib.utils.NCTokenUtils.*;
import static java.time.temporal.ChronoUnit.MILLIS;

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
@NCActiveModelProvider
public class TimerProvider extends NCModelProviderAdapter {
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("'<b>'HH'</b>h' '<b>'mm'</b>m' '<b>'ss'</b>s'").withZone(ZoneId.systemDefault());
    
    private Timer timer = new Timer();
    
    TimerProvider() {
        // Add a wide-catch intent. Note that terms in the intent will be matched
        // in any order and this intent can match some unusual grammar input
        // like "2 secs and 3mins set the timer". For the sake of simplicity
        // we allow such idiosyncratic input.
        NCIntentSolver solver = new NCIntentSolver(null, () -> { throw new NCCuration(); });
        
        solver.addIntent(
            new NON_CONV_INTENT(
                "timer|num{1+}",
                new TERM("id == x:timer", 1, 1),
                new TERM(
                    new AND("id == nlp:num", "~NUM_UNITTYPE == datetime", "~NUM_ISEQUALCONDITION == true"),
                    0,
                    7 // Supported numeric `datetime` unit types.
                )
            ),
            this::onMatch
        );
    
        setup(NCModelBuilder.newJsonModel(NCModelBuilder.classPathFile("timer_model.json")).
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
            throw new NCRejection("Ambiguous units.");
    
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
    
        return NCQueryResult.html("Timer set for: " + FMT.format(dt));
    }
}
