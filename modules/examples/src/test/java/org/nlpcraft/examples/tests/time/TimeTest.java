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

package org.nlpcraft.examples.tests.time;

import org.junit.jupiter.api.Test;
import org.nlpcraft.examples.tests.helpers.TestFactory;
import org.nlpcraft.examples.tests.helpers.TestRunner;
import org.nlpcraft.mdllib.tools.dev.NCTestClient;
import org.nlpcraft.mdllib.tools.dev.NCTestClientBuilder;

import java.util.Arrays;

/**
 * Time model test.
 *
 * Note that server and {@link org.nlpcraft.examples.time.TimeProbeRunner} must be started before.
 */
public class TimeTest {
    private static final TestFactory f = new TestFactory();
    private static final String mdlId = "nlpcraft.time.ex"; // See time_model.json
    private static final NCTestClient client = new NCTestClientBuilder().newBuilder().build();
    
    @Test
    public void test() {
        TestRunner.test(
            client,
            Arrays.asList(
                // Empty parameter.
                f.mkFailedOnExecution(mdlId, ""),
                
                // Unsupported language.
                f.mkFailedOnExecution(mdlId, "El tiempo en España"),
                
                f.mkPassed(mdlId, "What time is it now in New York City?"),
                f.mkPassed(mdlId, "What's the time in Moscow?"),
                f.mkPassed(mdlId, "Show me time of the day in London."),
                f.mkPassed(mdlId, "Give me San Francisco's current date and time."),
                f.mkPassed(mdlId, "What's the local time?")
            )
        );
    }
}
