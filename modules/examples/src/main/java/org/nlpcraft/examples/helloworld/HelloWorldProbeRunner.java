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

package org.nlpcraft.examples.helloworld;

import org.nlpcraft.NCException;
import org.nlpcraft.probe.dev.NCProbeConfig;
import org.nlpcraft.probe.dev.NCProbeDevApp;

/**
 * In-process probe runner for this example.
 * <p>
 * Make sure to setup these system properties:
 * <ul>
 *     <li>
 *         <code>NLPCRAFT_PROBE_ID</code>=<code>probe ID</code> (any user defined name).
 *     </li>
 *     <li>
 *         <code>NLPCRAFT_PROBE_TOKEN</code>=<code>probe token</code>.
 *     </li>
 * </ul>
 */
public class HelloWorldProbeRunner {
    /**
     * In-process probe entry point.
     * 
     * @param args Command like arguments (none are required).
     */
    public static void main(String[] args) throws NCException {
        // Create probe configuration with the provider instance.
        NCProbeConfig cfg = new NCProbeConfig(new HelloWorldProvider());

        // Start probe and wait synchronously for its exit code.
        int exitCode = NCProbeDevApp.start(cfg);

        System.exit(exitCode);
    }
}
