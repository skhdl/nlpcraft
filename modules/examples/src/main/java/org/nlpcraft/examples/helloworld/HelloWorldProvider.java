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

package org.nlpcraft.examples.helloworld;

import org.nlpcraft.NCException;
import org.nlpcraft.mdllib.NCActiveModelProvider;
import org.nlpcraft.mdllib.NCQueryResult;
import org.nlpcraft.mdllib.NCModelProviderAdapter;
import org.nlpcraft.mdllib.tools.builder.NCModelBuilder;

/**
 * Hello World example model provider.
 * <p>
 * This example simply responds with 'Hello World!' on any user input. This is the simplest
 * user model that can be defined.
 */
@NCActiveModelProvider
public class HelloWorldProvider extends NCModelProviderAdapter {
    /**
     * Initializes provider.
     *
     * @throws NCException If any errors occur.
     */
    HelloWorldProvider() throws NCException {
        // Initialize adapter.
        setup(
            // Minimally defined model...
            NCModelBuilder.newModel("dl.helloworld.ex", "HelloWorld Example Model", "1.0")
                // Return HTML result.
                .setQueryFunction(ctx -> NCQueryResult.html(
                    "Hello World!<br/>" +
                    "See more <a target=_new href='https://youtu.be/zecueq-mo4M'>examples</a> of Hello World!"
                ))
                .build()
        );
    }
}
