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

package org.nlpcraft.examples.tests.echo;

import org.junit.jupiter.api.Test;
import org.nlpcraft.examples.tests.helpers.TestFactory;
import org.nlpcraft.examples.tests.helpers.TestRunner;
import org.nlpcraft.mdllib.tools.dev.NCTestClient;
import org.nlpcraft.mdllib.tools.dev.NCTestClientBuilder;

import java.util.Arrays;

/**
 * Echo model test.
 *
 * Note that server and {@link org.nlpcraft.examples.echo.EchoProbeRunner} must be started before.
 */
public class EchoTest {
    private static final TestFactory f = new TestFactory();
    private static final String mdlId = "nlpcraft.echo.ex"; // See oEchoProvider#MODEL_ID
    private static final NCTestClient client = new NCTestClientBuilder().newBuilder().build();
    
    @Test
    public void test() {
        TestRunner.process(
            client,
            Arrays.asList(
                // Empty parameter.
                f.mkFailedOnExecution(mdlId, ""),
                
                // Unsupported language.
                f.mkFailedOnExecution(mdlId, "El tiempo en España"),
                
                f.mkPassed(mdlId, "LA weather last Friday")
            )
        );
    }
}
