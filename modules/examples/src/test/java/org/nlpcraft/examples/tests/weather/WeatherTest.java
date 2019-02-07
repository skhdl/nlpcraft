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

package org.nlpcraft.examples.tests.weather;

import org.junit.jupiter.api.Test;
import org.nlpcraft.examples.tests.helpers.TestFactory;
import org.nlpcraft.examples.tests.helpers.TestRunner;
import org.nlpcraft.mdllib.tools.dev.NCTestClientBuilder;

import java.util.Arrays;

/**
 * Weather model test.
 *
 * Note that server and {@link org.nlpcraft.examples.weather.WeatherProbeRunner} must be started before.
 */
public class WeatherTest {
    private static final TestFactory f = new TestFactory();
    private static final String mdlId = "nlpcraft.weather.ex"; // See weather_model.json
    private static final NCTestClientBuilder builder = new NCTestClientBuilder().newBuilder();
    
    @Test
    public void testConversation() {
        TestRunner.test(
            builder.setClearConversation(false).build(),
            Arrays.asList(
                // Empty parameter.
                f.mkFailedOnExecution(mdlId, ""),
                
                // Unsupported language.
                f.mkFailedOnExecution(mdlId, "El tiempo en España"),
                
                f.mkPassed(mdlId, "What's the local weather forecast?"),
                f.mkPassed(mdlId, "What's the weather in Moscow?"),
    
                // Missed user defined tokens, but it should be accessible from conversation context.
                f.mkPassed(mdlId, "Moscow")
            )
        );
    }
    
    @Test
    public void testNoConversation() {
        TestRunner.test(
            builder.setClearConversation(true).build(),
            Arrays.asList(
                // Empty parameter.
                f.mkFailedOnExecution(mdlId, ""),
                
                // Unsupported language.
                f.mkFailedOnExecution(mdlId, "El tiempo en España"),
                
                f.mkPassed(mdlId, "What's the local weather forecast?"),
                f.mkPassed(mdlId, "What's the weather in Moscow?"),
    
                // Missed user defined tokens.
                f.mkFailedOnExecution(mdlId, "Moscow")
            )
        );
    }
}
