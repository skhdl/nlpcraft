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
     * Gets probe token. This token should be kept secure.
     *
     * @return Probe token.
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
     * Gets optional folder to scan for model JARs.
     *
     * @return Optional folder to scan for model JARs.
     */
    String getJarsFolder();
}
