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
                newModel("dl.time.ex", "Time Example Model", "1.0").
                addElement(dateTimeElem).
                setQueryFunction(solver::solve).
                build()
        );
    }
}
