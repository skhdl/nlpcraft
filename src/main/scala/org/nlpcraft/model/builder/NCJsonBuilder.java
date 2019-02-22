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

package org.nlpcraft.model.builder;

import com.google.gson.Gson;
import org.nlpcraft.common.NCException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

/**
 * Base builder class with JSON reading utilities.
 */
class NCJsonBuilder {
    /** */
    protected static final Gson gson = new Gson();
    
    /**
     * Reads JSON file and creates JSON representation object of given type.
     *
     * @param filePath File path.
     * @param claxx JSON representation class.
     * @param <T> Object type.
     * @return Initialized object.
     * @throws NCException In case of any errors loading JSON.
     */
    static <T> T readFile(String filePath, Class<T> claxx) throws NCException {
        try (Reader reader = new BufferedReader(new FileReader(filePath))) {
            return gson.fromJson(reader, claxx);
        }
        catch (Exception e) {
            throw new NCException("Failed to load JSON from: " + filePath, e);
        }
    }
    
    /**
     * Reads JSON file and creates JSON representation object of given type.
     *
     * @param in Input stream.
     * @param claxx JSON representation class.
     * @param <T> Object type.
     * @return Initialized object.
     * @throws NCException In case of any errors loading JSON.
     */
    static <T> T readFile(InputStream in, Class<T> claxx) throws NCException {
        try (Reader reader = new BufferedReader(new InputStreamReader(in))) {
            return gson.fromJson(reader, claxx);
        }
        catch (Exception e) {
            throw new NCException("Failed to load JSON from stream.", e);
        }
    }
    
    /**
     * Reads JSON string and creates JSON representation object of given type.
     *
     * @param jsonStr JSON string to read from.
     * @param claxx JSON representation class.
     * @param <T> Object type.
     * @return Initialized object.
     * @throws NCException In case of any errors loading JSON.
     */
    static <T> T readString(String jsonStr, Class<T> claxx) throws NCException {
        try (Reader reader = new BufferedReader(new StringReader(jsonStr))) {
            return gson.fromJson(reader, claxx);
        }
        catch (Exception e) {
            throw new NCException("Failed to load JSON from string.", e);
        }
    }
}
