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

package org.nlpcraft.nlp.enrichers.geo

// GEO model which contains common GEO data.
case class NCGeoModel(
    synonyms: Map[String, Set[NCGeoEntry]],
    continents: Set[NCGeoContinent],
    subContinents: Set[NCGeoSubContinent],
    countries: Set[NCGeoCountry],
    regions: Set[NCGeoRegion],
    cities: Set[NCGeoCity],
    metros: Set[NCGeoMetro],
    topWorldCities: Set[NCTopGeoCity],
    topUsaCities: Set[NCTopGeoCity]
)

/**
  * Base trait for the classes representing GEO entities.
  */
sealed trait NCGeoEntry {
    def name: String
}

// Main GEO model components.
case class NCGeoContinent(name: String) extends NCGeoEntry
case class NCGeoSubContinent(name: String, continent: NCGeoContinent) extends NCGeoEntry
case class NCGeoCountry(name: String, subContinent: NCGeoSubContinent) extends NCGeoEntry
case class NCGeoRegion(name: String, country: NCGeoCountry) extends NCGeoEntry
case class NCGeoCity(name: String, latitude: Double, longitude: Double, region: NCGeoRegion) extends NCGeoEntry
case class NCGeoMetro(name: String) extends NCGeoEntry
case class NCTopGeoCity(name: String, region: NCGeoRegion) extends NCGeoEntry

// GEO synonym.
case class NCGeoSynonym(
    city: Option[String],
    latitude: Option[Double],
    longitude: Option[Double],
    region: Option[String],
    country: Option[String],
    subcontinent: Option[String],
    continent: Option[String],
    metro: Option[String],
    var synonyms: List[String]
)

// Type of GEO locations.
object NCGeoLocationKind extends Enumeration {
    type NCGeoLocationKind = Value
    
    val CONTINENT, SUBCONTINENT, COUNTRY, METRO, REGION, CITY = Value
}

