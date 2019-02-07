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

import com.google.gson.*;
import com.google.gson.reflect.*;
import org.junit.jupiter.api.*;
import org.nlpcraft.mdllib.tools.dev.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TODO: add description.
 */
public class Weather2Test2 {
    private static final String mdlId = "nlpcraft.weather2.ex"; // See weather2_model.json
    private static final Gson GSON = new Gson();
    private static final Type MAP_RESP = new TypeToken<HashMap<String, Object>>() {}.getType();
    private static NCTestClient cli = null;

    /**
     * Utility to check intent ID in JSON result from 'weather2' example.
     *
     * @param jsonRes
     * @param intentId
     */
    private static boolean checkIntentId(String jsonRes, String intentId) {
        Map<String, Object> map = GSON.fromJson(jsonRes, MAP_RESP);

        return intentId.equals(map.get("intentId"));
    }

    @Test
    public void testIndividuals() throws IOException {
        cli = new NCTestClientBuilder().newBuilder().setClearConversation(true).build();

        assertTrue(cli.test(new NCTestSentence("", mdlId)).isFailed());
        assertTrue(cli.test(new NCTestSentence("El tiempo en España", mdlId)).isFailed());
        assertTrue(checkIntentId(
            cli.test(new NCTestSentence("What's the local weather forecast?", mdlId)).getResult().get(),
            "fcast|date?|city?"
        ));
    }
}
