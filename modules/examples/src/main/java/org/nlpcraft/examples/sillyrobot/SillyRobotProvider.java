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
 * Software:    NlpCraft
 * License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
 * Licensor:    DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.examples.sillyrobot;

import org.nlpcraft.*;
import org.nlpcraft.mdllib.*;
import org.nlpcraft.mdllib.intent.*;
import org.nlpcraft.mdllib.intent.NCIntentSolver.*;
import org.nlpcraft.mdllib.tools.builder.*;
import org.apache.commons.lang3.*;
import java.util.*;
import java.util.function.*;

import static org.nlpcraft.mdllib.utils.NCTokenUtils.*;

/**
 * Silly Robot example model provider.
 * <p>
 * This simple example provides the model that allows you to "start" and "stop"
 * arbitrary objects. You can say "start the car", "stop the car", or "can you turn on a washing machine?".
 * The model will voice-reply the acknowledgement of the operation: "car has been started", etc.
 */
@NCActiveModelProvider
public class SillyRobotProvider extends NCModelProviderAdapter {
    // Robot's memory.
    private final Set<String> mem = new HashSet<>();

    /**
     * A shortcut to string capitalization.
     *
     * @param s String to capitalize.
     * @return Capitalized string.
     */
    private String cap(String s) {
        return StringUtils.capitalize(s);
    }

    /**
     * Gets the subject out of the solver context (1st token in the 2nd term - see solver definition).
     *
     * @param ctx Solver context.
     * @return Subject of the input string.
     */
    private String getSubject(NCIntentSolverContext ctx) {
        return getNormalizedText(ctx.getIntentTokens().get(1).get(0));
    }

    /**
     * Callback on state inquiry.
     *
     * @param ctx Solver context.
     * @return Query result.
     */
    private NCQueryResult doState(NCIntentSolverContext ctx) {
        // Subject of the sentence.
        String subj = getSubject(ctx);

        // US English voice reply.
        return NCQueryResult.enUsSpeak(cap(subj) + (mem.contains(subj) ? " is started." : " is not started."));
    }

    /**
     * Callback on start inquiry.
     *
     * @param ctx Solver context.
     * @return Query result.
     */
    private NCQueryResult doStart(NCIntentSolverContext ctx) {
        // Subject of the sentence.
        String subj = getSubject(ctx);

        // US English voice reply.
        return NCQueryResult.enUsSpeak(cap(subj) + (!mem.add(subj) ? " is already started." : " has been started."));
    }

    /**
     * Callback on stop inquiry.
     *
     * @param ctx Solver context.
     * @return Query result.
     */
    private NCQueryResult doStop(NCIntentSolverContext ctx) {
        // Subject of the sentence.
        String subj = getSubject(ctx);

        // US English voice reply.
        return NCQueryResult.enUsSpeak(cap(subj) + (!mem.remove(subj) ? " has not been started." : " has been stopped."));
    }

    /**
     * Initializes provider.
     *
     * @throws NCException If any errors occur.
     */
    SillyRobotProvider() throws NCException {
        String path = NCModelBuilder.classPathFile("silly_robot_model.json");

        // Create default token solver for intent-based matching.
        NCIntentSolver solver = new NCIntentSolver();

        // Lambda for adding intent to the solver.
        BiConsumer<String, IntentCallback> intentMaker =
            (id, f/* Callback. */) ->
                solver.addIntent(
                    new CONV_INTENT(
                        id + "|subject",
                        // Term idx=0:
                        // A non-interactive term that is either 'state', 'start' or 'stop'.
                        // ID of the element should be 'ctrl:start', 'ctrl:state', or 'ctrl:stop'.
                        new TERM("id == ctrl:" + id, 1, 1),
                        // Term idx=1:
                        // An interactive object term. If it's missing the system will ask for it.
                        // ID of the element should be 'ctrl:subject'
                        new TERM("an object to " + id, "id == ctrl:subject", 1, 1)
                    ),
                    f
                );

        // Add three intents for 'state', 'start' and 'stop' commands.
        intentMaker.accept("state", this::doState);
        intentMaker.accept("start", this::doStart);
        intentMaker.accept("stop", this::doStop);

        // Load model form JSON configuration and set query function implementation based
        // on intent-based token solver. Initialize adapter with constructed model.
        setup(NCModelBuilder.newJsonModel(path).setQueryFunction(solver::solve).build());
    }
}
