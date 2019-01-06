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
 * Software:    NlpCraft
 * License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
 * Licensor:    DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.geo

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

