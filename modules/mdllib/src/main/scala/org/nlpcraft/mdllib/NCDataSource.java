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
 * Descriptor of the data source. Data source logically acts as a specific instance of the
 * model. Data source are created by administrators either through REST API or through the webapp.
 */
public interface NCDataSource {
    /**
     * Gets name of the data source.
     *
     * @return Short, descriptive name of the data source.
     */
    String getName();

    /**
     * Gets optional configuration provided by data source to the model. Note that multiple data sources (typically
     * of the same type) can share the same model definition. This configuration allows a data source to provide its
     * specific configuration to the common model, e.g. JDBC connection string, user credentials, etc.
     *
     * @return Configuration provided by data source to the model, or {@code null} if not provided.
     */
    String getConfig();

    /**
     * Gets short data source description.
     *
     * @return Short data source description.
     */
    String getDescription();
}