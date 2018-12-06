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

import java.io.*;
import java.util.*;

/**
 * NlpCraft model provider. Model provider is a deployment unit and acts as a factory for models.
 * Note that single provider can support one or more different models, and multiple providers can
 * be deployed.
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
