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

package org.nlpcraft.mdllib.tools.builder;

import org.nlpcraft.*;
import com.google.gson.*;
import java.io.*;

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
