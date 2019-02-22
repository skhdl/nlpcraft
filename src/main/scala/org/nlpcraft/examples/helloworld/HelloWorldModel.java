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

package org.nlpcraft.examples.helloworld;

import org.nlpcraft.NCException;
import org.nlpcraft.mdllib.NCModelProvider;
import org.nlpcraft.mdllib.NCModelProviderAdapter;
import org.nlpcraft.mdllib.NCQueryResult;
import org.nlpcraft.mdllib.tools.builder.NCModelBuilder;

/**
 * Hello World example data model.
 * <p>
 * This trivial example simply responds with 'Hello World!' on any user input.
 * This is the simplest user model that can be defined.
 * <p>
 * Note that all models must be "wrapped" in {@link NCModelProvider} interface to be deployable
 * into data probes.
 *
 * @see HelloWorldTest
 */
public class HelloWorldModel extends NCModelProviderAdapter {
    /**
     * Initializes provider.
     *
     * @throws NCException If any errors occur.
     */
    public HelloWorldModel() throws NCException {
        // Initialize model provider adapter.
        setup(
            // Minimally defined model...
            NCModelBuilder.newModel("nlpcraft.helloworld.ex", "HelloWorld Example Model", "1.0")
                // Return the same HTML result for any user input.
                .setQueryFunction(ctx -> NCQueryResult.html(
                    "Hello World! This model returns the same result for any input..."
                ))
                .build()
        );
    }
}
