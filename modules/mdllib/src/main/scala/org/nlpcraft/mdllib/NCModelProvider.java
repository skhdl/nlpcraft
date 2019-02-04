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

import java.io.*;
import java.util.*;

/**
 * Model provider. Model provider is a deployment unit and acts as a factory for models.
 * Note that single provider can support one or more different models, and multiple providers can
 * be deployed into the probe.
 * <p>
 * In most cases users should use {@link NCModelProviderAdapter} for convenience.
 *
 * @see NCActiveModelProvider
 * @see NCModelProviderAdapter
 */
public interface NCModelProvider extends Serializable {
    /**
     * Gets fully initialized model instance for given model ID. Note that this method
     * can either create new model on demand or reuse the existing (cached) one if it is safe to do so.
     *
     * @param id Unique, <i>immutable</i> ID of the model to make.
     * @return Fully initialized model instance or {@code null} if provider does not support model with given ID.
     * @see NCModelDescriptor#getId()
     */
    NCModel makeModel(String id);

    /**
     * Gets the list, possibly empty but never {@code null}, of model descriptors supported by this provider.
     * Note that getting list of descriptors separately from the models themselves allows to query the provider
     * without actually creating any models which can be an expensive process with side effects.
     *
     * @return List of model descriptors supported by this provider (possibly empty but never {@code null}).
     */
    List<NCModelDescriptor> getDescriptors();
}
