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
 * Map-based container for user-provided metadata.
 *
 * @see NCModel#getMetadata()
 * @see NCElement#getMetadata()
 */
public interface NCMetadata extends Map<String, Serializable>, Serializable {
    /**
     * Gets optional string metadata value.
     *
     * @param key Metadata name.
     * @param dflt value to return if given name is not present.
     * @return Metadata value of value if given name is not present.
     */
    String getStringOrElse(String key, String dflt);

    /**
     * Gets mandatory string metadata value.
     *
     * @param key Metadata name.
     * @return Metadata value.
     * @throws IllegalArgumentException Thrown when given metadata key is not present.
     */
    String getString(String key);

    /**
     * Gets optional string metadata value.
     *
     * @param key Metadata name.
     * @return Metadata value.
     */
    Optional<String> getStringOpt(String key);

    /**
     * Gets mandatory integer metadata value.
     *
     * @param key Metadata name.
     * @return Metadata value.
     * @throws IllegalArgumentException Thrown when given metadata key is not present.
     */
    int getInteger(String key);

    /**
     * Gets optional integer metadata value.
     *
     * @param key Metadata name.
     * @return Metadata value.
     */
    Optional<Integer> getIntegerOpt(String key);

    /**
     * Gets optional integer metadata value.
     *
     * @param key Metadata name.
     * @param dflt Default non-null value to return if given name is not present.
     * @return Metadata value of value if given name is not present.
     */
    int getIntegerOrElse(String key, int dflt);

    /**
     * Gets mandatory double metadata value.
     *
     * @param key Metadata name.
     * @return Metadata value.
     * @throws IllegalArgumentException Thrown when given metadata key is not present.
     */
    double getDouble(String key);

    /**
     * Gets optional double metadata value.
     *
     * @param key Metadata name.
     * @return Metadata value.
     */
    Optional<Double> getDoubleOpt(String key);

    /**
     * Gets optional double metadata value.
     *
     * @param key Metadata name.
     * @param dflt Default non-null value to return if given name is not present.
     * @return Metadata value of value if given name is not present.
     */
    double getDoubleOrElse(String key, double dflt);

    /**
     * Gets mandatory long metadata value.
     *
     * @param key Metadata name.
     * @return Metadata value.
     * @throws IllegalArgumentException Thrown when given metadata key is not present.
     */
    long getLong(String key);

    /**
     * Gets optional long metadata value.
     *
     * @param key Metadata name.
     * @return Metadata value.
     */
    Optional<Long> getLongOpt(String key);

    /**
     * Gets optional long metadata value.
     *
     * @param key Metadata name.
     * @param dflt Default non-null value to return if given name is not present.
     * @return Metadata value of value if given name is not present.
     */
    long getLongOrElse(String key, long dflt);

    /**
     * Gets mandatory boolean metadata value.
     *
     * @param key Metadata name.
     * @return Metadata value.
     * @throws IllegalArgumentException Thrown when given metadata key is not present.
     */
    boolean getBoolean(String key);

    /**
     * Gets optional boolean metadata value.
     *
     * @param key Metadata name.
     * @return Metadata value.
     */
    Optional<Boolean> getBooleanOpt(String key);

    /**
     * Gets optional boolean metadata value.
     *
     * @param key Metadata name.
     * @param dflt Default non-null value to return if given name is not present.
     * @return Metadata value of value if given name is not present.
     */
    boolean getBooleanOrElse(String key, boolean dflt);

    /**
     * Gets typed mandatory metadata value.
     *
     * @param key Metadata key.
     * @param <T> Type of the metadata value.
     * @return Metadata value.
     * @throws IllegalArgumentException Thrown when given metadata key is not found.
     */
    <T> T getAs(String key);

    /**
     * Gets typed optional metadata value.
     *
     * @param key Metadata key.
     * @param <T> Type of the metadata value.
     * @return Metadata value.
     */
    <T> Optional<T> getOptAs(String key);
}
