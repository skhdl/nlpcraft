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

package org.nlpcraft.examples.lessons.lesson2;

import org.nlpcraft.*;
import org.nlpcraft.examples.lessons.utils.*;
import org.nlpcraft.mdllib.*;
import org.nlpcraft.mdllib.intent.*;
import org.nlpcraft.mdllib.intent.NCIntentSolver.*;
import org.nlpcraft.mdllib.tools.builder.*;

/**
 * `Lesson 2` model provider.
 */
@NCActiveModelProvider
public class TimeProvider2 extends NCModelProviderAdapter {
    /**
     * Initializes provider.
     *
     * @throws NCException If any errors occur.
     */
    TimeProvider2() throws NCException {
        NCElement dateTimeElem =
            NCElementBuilder.newElement("x:time").addSynonyms("time", "date").build();
    
        NCIntentSolver solver =
            new NCIntentSolver(
                "time-solver",
                () -> {
                    // Custom not-found function with tailored rejection message.
                    throw new NCRejection("I can't understand your question.");
                }
            );
        
        solver.addIntent(
            new INTENT(null, false, false, new NCIntentSolver.TERM("id == x:time", 1, 1)),
            ctx -> NCQueryResult.text(LessonsUtils.now())
        );
    
        // Initialize adapter.
        setup(
            NCModelBuilder.
                newModel("nlpcraft.time.ex", "Time Example Model", "1.0").
                addElement(dateTimeElem).
                setQueryFunction(solver::solve).
                build()
        );
    }
}
