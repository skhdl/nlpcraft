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

package org.nlpcraft.examples.misc.geo.cities;

import org.apache.commons.lang3.tuple.Pair;
import org.nlpcraft.common.NCException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * City-timezone map provider.
 */
public class CitiesDataProvider {
    /**
     * Creates and returns cities timezone map for all cities with a population > 15000 or capitals.
     *
     * @return Cities timezone map.
     */
    public static Map<City, CityData> get() throws NCException {
        try {
            List<String> lines = new ArrayList<>();
            
            try (BufferedReader reader =
                 new BufferedReader(new InputStreamReader(
                     Objects.requireNonNull(
                        CitiesDataProvider.class.
                        getClassLoader().
                        getResourceAsStream("org/nlpcraft/examples/misc/geo/cities/cities_timezones.txt"))
                ))) {
                String line = reader.readLine();
                
                while (line != null) {
                    lines.add(line);
                    
                    line = reader.readLine();
                }
            }
            
            return
                lines.stream().
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
            throw new NCException("Failed to read data file.", e);
        }
    }
}
