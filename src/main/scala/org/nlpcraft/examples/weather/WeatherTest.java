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

package org.nlpcraft.examples.weather;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nlpcraft.common.NCException;
import org.nlpcraft.model.test.NCTestClient;
import org.nlpcraft.model.test.NCTestClientBuilder;
import org.nlpcraft.model.test.NCTestResult;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Weather2 model test.
 *
 */
public class WeatherTest {
    private static final Gson GSON = new Gson();
    private static final Type TYPE_MAP_RESP = new TypeToken<HashMap<String, Object>>() {}.getType();
    
    private NCTestClient client;
    
    /**
     *
     * @param txt
     * @param intentId
     * @param shouldBeSame
     * @throws NCException
     * @throws IOException
     */
    private void checkIntent(String txt, String intentId, boolean shouldBeSame) throws NCException, IOException {
        NCTestResult res = client.ask(txt);
        
        assertTrue(res.isSuccessful());
        
        assert res.getResult().isPresent();
        
        Map<String, Object> map = GSON.fromJson(res.getResult().get(), TYPE_MAP_RESP);
        
        if (shouldBeSame)
            assertEquals(intentId, map.get("intentId"));
        else
            assertNotEquals(intentId, map.get("intentId"));
    }
    
    @BeforeEach
    void setUp() throws NCException, IOException {
        client = new NCTestClientBuilder().newBuilder().build();
        
        client.openForModelId("nlpcraft.weather.ex");  // See weather_model.json
    }
    
    @AfterEach
    void tearDown() throws NCException, IOException {
        client.close();
    }
    
    @Test
    public void test() throws NCException, IOException {
        // Empty parameter.
        assertTrue(client.ask("").isFailed());
    
        // Only latin charset is supported.
        assertTrue(client.ask("El tiempo en España").isFailed());
    
        // Unexpected intent ID.
        checkIntent("LA weather", "INVALID-INTENT-ID", false);
        
        // Should be passed.
        checkIntent("What's the local weather forecast?", "fcast|date?|city?", true);
        checkIntent("What's the weather in Moscow?", "curr|date?|city?", true);
    
        // Can be answered with conversation.
        checkIntent("Moscow", "curr|date?|city?", true);
        
        client.clearConversation();
    
        // Cannot be answered without conversation.
        assertTrue(client.ask("Moscow").isFailed());
    }
}
