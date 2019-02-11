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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nlpcraft.NCException;
import org.nlpcraft.mdllib.tools.dev.NCTestClient;
import org.nlpcraft.mdllib.tools.dev.NCTestClientBuilder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Weather model test.
 *
 * Note that server and {@link org.nlpcraft.examples.weather.WeatherProbeRunner} must be started before.
 */
public class WeatherTest {
    private NCTestClient client;
    
    @BeforeEach
    void setUp() throws NCException, IOException {
        client = new NCTestClientBuilder().newBuilder().build();
        
        client.open("nlpcraft.weather.ex"); // See weather_model.json
    }
    
    @AfterEach
    void tearDown() throws NCException, IOException {
        client.close();
    }
    
    @Test
    public void test() throws NCException, IOException {
        // Empty parameter.
        assertTrue(client.ask("").isFailed());
        
        // Unsupported language.
        assertTrue(client.ask("El tiempo en España").isFailed());
        
        // Should be passed.
        assertTrue(client.ask("What's the local weather forecast?").isSuccessful());
        assertTrue(client.ask("What's the weather in Moscow?").isSuccessful());

        // Can be answered with conversation.
        assertTrue(client.ask("Moscow").isSuccessful());
        
        client.clearConversation();
    
        // Cannot be answered without conversation.
        assertTrue(client.ask("Moscow").isFailed());
    }
}
