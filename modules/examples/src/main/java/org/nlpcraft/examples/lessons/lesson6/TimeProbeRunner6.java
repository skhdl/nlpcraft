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

package org.nlpcraft.examples.lessons.lesson6;

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
public class TimeProbeRunner6 {
    /**
     *
     * @param args Command like arguments (none are required).
     */
    public static void main(String[] args) throws NCException {
        NCProbeConfig cfg = new NCProbeConfig(new TimeProvider6());

        int exitCode = NCProbeDevApp.start(cfg);

        System.exit(exitCode);
    }
}
