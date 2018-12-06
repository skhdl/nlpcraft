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

package org.nlpcraft.examples.misc.geo.cities;

import org.nlpcraft.NCException;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * City-timezone map provider.
 */
public class CitiesDataProvider {
    private static final String DATA_FILE = "cities_timezones.txt";

    /**
     * Creates and returns cities timezone map for all cities with a population > 15000 or capitals.
     *
     * @return Cities timezone map.
     */
    public static Map<City, CityData> get() throws NCException {
        URL url = CitiesDataProvider.class.getClassLoader().getResource(DATA_FILE);

        if (url == null)
            throw new IllegalArgumentException("File not found: " + DATA_FILE);

        System.out.println();

        try {
            return Files.
                lines(Paths.get(new File(url.getFile()).getAbsolutePath())).
                filter(p -> !p.startsWith("#")).
                map(String::trim).
                filter(p -> !p.isEmpty()).
                map(p -> p.split("\t")).
                map(p -> Arrays.stream(p).map(String::trim).toArray(String[]::new)).
                map(arr ->
                    Pair.of(
                        new City(arr[0], arr[1]),
                        new CityData(arr[2], Double.parseDouble(arr[3]), Double.parseDouble(arr[4])))
                ).
                collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        }
        catch (IOException e) {
            throw new NCException("Failed to read data file: " + DATA_FILE, e);
        }
    }
}
