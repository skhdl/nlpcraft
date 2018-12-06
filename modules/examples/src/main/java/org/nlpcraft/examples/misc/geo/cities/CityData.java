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

/**
 * City data holder.
 */
public class CityData {
    private String timezone;
    private double latitude;
    private double longitude;

    /**
     * Creates new city data holder.
     *
     * @param timezone City timezone
     * @param latitude City latitude.
     * @param longitude City longitude.
     */
    public CityData(String timezone, double latitude, double longitude) {
        this.timezone = timezone;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Gets timezone.
     *
     * @return City timezone.
     */
    public String getTimezone() {
        return timezone;
    }

    /**
     * Gets latitude.
     *
     * @return City latitude.
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Gets longitude.
     *
     * @return City longitude.
     */
    public double getLongitude() {
        return longitude;
    }
}
