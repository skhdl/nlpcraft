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

package org.nlpcraft.mdllib;

import org.nlpcraft.mdllib.tools.builder.*;

import java.util.*;

/**
 * Tests model provider.
 */
@NCActiveModelProvider
public class NCModelTestProvider implements NCModelProvider {
    private NCModelDescriptor ds = NCModelDescriptorBuilder.newDescriptor(
        "Test-ID",
        "Test-Model",
        "1.0"
    ).build();

    @Override
    public List<NCModelDescriptor> getDescriptors() {
        return Collections.singletonList(ds);
    }

    @Override
    public NCModel makeModel(String id) {
        return NCModelBuilder.newModel()
            .setDescriptor(ds)
            .addMacro("<OF>", "{of|for|per}")
            .addMacro("<INFO>", "{metrics|report|analysis|analytics|info|information|statistics|stats}")
            .addMacro("<ID>", "{unique|*} {id|guid|identifier|identification} {number|*}")
            .addMacro("<NAME>", "{name|title}")
            .addMacro("<TOTAL>", "{overall|total|grand total|entire|full}")
            .addMacro("<NUM>", "{number|count|qty|quantity|amount}")
            .addElement(NCElementBuilder.newElement()
                .setId("ELM1")
                .setType("STRING")
                .setDescription("Element 1 description.")
                .addSynonyms(
                    "element {number|*} {<OF>|*} {one|1}",
                    "{some|*} {elm|elem} {n.|num|*} {one|1}",
                    "{fool|/[ab].+/}" // 'fool' or regex matching.
                )
                .build()
            )
            .addElement(NCElementBuilder.newElement("ELM2")
                .setDescription("Super element description.")
                .setType("LONG")
                .addSynonyms(
                    "super {elm|elem|element}",
                    "today",
                    "/[ab].+/"
                )
                .build()
            )
            .addTrivia(new NCTriviaGroup() {
                @Override public Collection<String> getInputs() {
                    return Collections.singletonList("hi");
                }

                @Override public Collection<String> getResponses() {
                    return Collections.singletonList("hi {a|b}!!!");
                }
            })
            .setQueryFunction((NCQueryContext ctx) -> NCQueryResult.html("OK result"))
            .build();
    }
}
