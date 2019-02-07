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

package org.nlpcraft.examples.tests.weather2;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.nlpcraft.examples.tests.TestFactory;
import org.nlpcraft.examples.tests.TestRunner;
import org.nlpcraft.examples.weather2.Weather2ProbeRunner;
import org.nlpcraft.mdllib.tools.dev.NCTestClient;
import org.nlpcraft.mdllib.tools.dev.NCTestClientBuilder;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Weather2 model test.
 *
 * Note that server and {@link Weather2ProbeRunner} must be started before.
 */
public class Weather2Test {
    private final Gson GSON = new Gson();
    private final Type MAP_RESP = new TypeToken<HashMap<String, Object>>() {}.getType();
    
    private Function<String, Optional<String>> mkIntentIdChecker(String intentId) {
        return (js) -> {
            Map<String, Object> m = GSON.fromJson(js, MAP_RESP);
    
            Object resIntentId = m.get("intentId");
            
            return intentId.equals(resIntentId) ?
                Optional.empty() :
                Optional.of(
                    String.format("Unexpected intent ID [expected=%s, resultIntentId=%s]", intentId, resIntentId)
                );
        };
    }
    
    @Test
    public void test() {
        NCTestClient client = new NCTestClientBuilder().newBuilder().build();
        TestFactory f = new TestFactory();
    
        TestRunner.process(
            client,
            Arrays.asList(
                f.mkFailed("nlpcraft.weather2.ex", ""),
                f.mkPassed("nlpcraft.weather2.ex", "LA weather", mkIntentIdChecker("curr|date?|city?")
                )
            )
        );
    }
}
