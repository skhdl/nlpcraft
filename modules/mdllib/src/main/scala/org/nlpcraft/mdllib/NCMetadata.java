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
 * Convenient map-based container for user-provided metadata.
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
