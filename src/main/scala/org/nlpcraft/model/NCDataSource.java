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

package org.nlpcraft.model;

/**
 * Descriptor of the data source. Data source logically acts as a specific instance of the
 * data model.
 */
public interface NCDataSource {
    /**
     * Gets the name of the data source.
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
     * Gets optional short data source description. Short description can be used in management tools.
     *
     * @return Short data source description, or {@code null} if not provided.
     */
    String getDescription();
}