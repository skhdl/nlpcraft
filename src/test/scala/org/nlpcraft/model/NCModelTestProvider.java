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

package org.nlpcraft.model;

import org.nlpcraft.model.builder.NCElementBuilder;
import org.nlpcraft.model.builder.NCModelBuilder;
import org.nlpcraft.model.builder.NCModelDescriptorBuilder;

import java.util.Collections;
import java.util.List;

/**
 * Tests model provider.
 */
public class NCModelTestProvider implements NCModelProvider {
    private final NCModelDescriptor ds = NCModelDescriptorBuilder.newDescriptor(
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
                .addSynonyms(
                    "super {elm|elem|element}",
                    "today",
                    "/[ab].+/"
                )
                .build()
            )
            .setQueryFunction((NCQueryContext ctx) -> NCQueryResult.html("OK result"))
            .build();
    }
}
