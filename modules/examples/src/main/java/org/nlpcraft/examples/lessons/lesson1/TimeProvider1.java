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

package org.nlpcraft.examples.lessons.lesson1;

import org.nlpcraft.NCException;
import org.nlpcraft.examples.lessons.utils.LessonsUtils;
import org.nlpcraft.mdllib.NCActiveModelProvider;
import org.nlpcraft.mdllib.NCQueryResult;
import org.nlpcraft.mdllib.NCModelProviderAdapter;
import org.nlpcraft.mdllib.tools.builder.NCModelBuilder;

/**
 * `Lesson 1` model provider.
 */
@NCActiveModelProvider
public class TimeProvider1 extends NCModelProviderAdapter {
    /**
     * Initializes provider.
     *
     * @throws NCException If any errors occur.
     */
    TimeProvider1() throws NCException {
        // Initialize adapter.
        setup(
            NCModelBuilder.newModel("nlpcraft.time.ex", "Time Example Model", "1.0").
                setQueryFunction(ctx -> NCQueryResult.text(LessonsUtils.now())).
                build()
        );
    }
}
