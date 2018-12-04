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

package org.nlpcraft.mdllib;

/**
 * Probe runtime context. Instance of this interface is passed to the model via
 * {@link NCModel#initialize(NCProbeContext)} method when model is initialized for the first time
 * by the probe.
 *
 * @see NCModel#initialize(NCProbeContext)
 */
public interface NCProbeContext {
    /**
     * Asynchronously reloads model with given ID without restarting the probe.
     * This method will return immediately and model will be reloaded from a separate thread.
     * <br><br>
     * Note that after calling this method the specified model will be {@link NCModel#discard() discarded}
     * and will no longer be valid. It is important that {@link NCModel#discard()} method implementation would
     * properly de-initialize the model and perform all necessary clean up and end-of-life operations such as
     * closing files, networks and database connections, flushing caches, etc.
     * 
     * @param modelId Unique, <i>immutable</i> ID of the model to reload.
     */
    void reloadModel(String modelId);

    /**
     * Gets ID of the probe.
     *
     * @return ID of the probe.
     */
    String getId();

    /**
     * Gets company specific probe token. All probes belonging to one company should have
     * the same token. This token should be kept secure. See account page on the website
     * to see your company's token.
     *
     * @return Company specific probe token.
     */
    String getToken();

    /**
     * Gets uplink endpoint for this probe.
     *
     * @return Uplink endpoint for this probe.
     */
    String getUpLink();

    /**
     * Gets downlink endpoint for this probe.
     *
     * @return Downlink endpoint for this probe.
     */
    String getDownLink();

    /**
     * Get optional user email. It should be used during development and debugging of the model
     * to ensure that unfinished model isn't exposed to other users.
     *
     * @return Optional user email.
     */
    String getEmail();

    /**
     * Gets optional folder to scan for model JARs.
     *
     * @return Optional folder to scan for model JARs.
     */
    String getJarsFolder();
}
