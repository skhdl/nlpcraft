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

import org.nlpcraft.common.NCException;
import org.nlpcraft.examples.lessons.utils.LessonsUtils;
import org.nlpcraft.model.NCElement;
import org.nlpcraft.model.NCModelProviderAdapter;
import org.nlpcraft.model.NCQueryResult;
import org.nlpcraft.model.NCRejection;
import org.nlpcraft.model.intent.NCIntentSolver;
import org.nlpcraft.model.intent.NCIntentSolver.INTENT;
import org.nlpcraft.model.tools.builder.NCElementBuilder;
import org.nlpcraft.model.tools.builder.NCModelBuilder;

/**
 * `Lesson 2` model provider.
 */
public class TimeModel2 extends NCModelProviderAdapter {
    /**
     * Initializes provider.
     *
     * @throws NCException If any errors occur.
     */
    TimeModel2() throws NCException {
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