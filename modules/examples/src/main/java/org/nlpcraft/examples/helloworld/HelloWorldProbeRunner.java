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
 * Software:    NlpCraft
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

package org.nlpcraft.examples.helloworld;

import org.nlpcraft.*;
import org.nlpcraft.probe.dev.*;

/**
 * In-process probe runner for this example.
 * <p>
 * Note that this probes requires no configuration (it uses all configuration defaults). However, if necessary,
 * you can set your own configuration properties (see <code>NCProbeConfigBuilder</code> class for details).
 */
public class HelloWorldProbeRunner {
    /**
     * In-process probe entry point.
     * 
     * @param args Command like arguments (none are required).
     */
    public static void main(String[] args) throws NCException {
        // Create probe configuration with the provider instance.
        NCProbeConfig cfg = NCProbeConfigBuilder.newConfig(new HelloWorldProvider()).build();

        // Start probe and wait synchronously for its exit code.
        int exitCode = NCProbeDevApp.start(cfg);

        System.exit(exitCode);
    }
}
