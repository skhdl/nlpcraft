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

package org.nlpcraft.server.geo

import org.nlpcraft.common.nlp.dict.{NCDictionaryManager, NCDictionaryType}
import org.nlpcraft.server.json.NCJson
import org.nlpcraft.common._
import org.nlpcraft.common.NCLifecycle

import scala.collection.{immutable, mutable}

/**
  * Geo manager.
  */
object NCGeoManager extends NCLifecycle("Geo manager") {
    // Config files.
    private final val CTRY_DIR = "geo/countries"
    private final val CONT_PATH = "geo/continents.json"
    private final val METRO_PATH = "geo/metro.json"
    private final val SYNONYMS_DIR_PATH = "geo/synonyms"
    private final val CASE_SENSITIVE_DIR_PATH = s"$SYNONYMS_DIR_PATH/case_sensitive"
    
    // Special file, the data of which are not filtered by common dictionary words.
    private final val SYNONYMS_MANUAL_FILES = Seq("manual.json", "states.json")
    
    private var model: NCGeoModel = _
    
    // Auxiliary words for GEO names. Example: CA state, Los Angeles city.
    private final val CITY_AUX = Seq("city", "town", "metropolis")
    private final val REGION_AUX = Seq("region", "state", "area")
    
    /**
      * Starts manager.
      */
    @throws[NCE]
    override def start(): NCLifecycle = {
        ensureStopped()
        
        model = readAndConstructModel()
        
        super.start()
    }
    
    /**
      * Stops this component.
      */
    override def stop(): Unit = {
        checkStopping()
        
        model = null
        
        super.stop()
    }
    
    /**
      * Gets GEO model.
      *
      * @return Immutable GEO model.
      */
    def getModel: NCGeoModel = {
        ensureStarted()
        
        model
    }
    
    /**
      * Reads and constructs GEO model.
      */
    @throws[NCE]
    private def readAndConstructModel(): NCGeoModel = {
        val geoEntries = mutable.HashMap[String, mutable.HashSet[NCGeoEntry]]()
        
        val conts = mutable.HashSet.empty[NCGeoContinent]
        val subs = mutable.HashSet.empty[NCGeoSubContinent]
        val cntrs = mutable.HashSet.empty[NCGeoCountry]
        val regions = mutable.HashSet.empty[NCGeoRegion]
        val cities = mutable.HashSet.empty[NCGeoCity]
        val metro = mutable.HashSet.empty[NCGeoMetro]
        
        // JSON extractors.
        case class JsonMetro(name: String)
        case class JsonCountry(name: String, code: String)
        case class JsonCity(name: String, latitude: Double, longitude: Double)
        case class JsonRegion(name: String, cities: List[JsonCity])
        case class JsonCountryHolder(name: String, regions: List[JsonRegion])
        case class JsTopCity(name: String, region: String, country: String)
        
        // Add location internal representation.
        
        def addEntry(key: String, geo: NCGeoEntry, lowerCase: Boolean) {
            val k = if (lowerCase) key.toLowerCase else key
            
            geoEntries.put(
                k,
                geoEntries.get(k) match {
                    case Some(set) ⇒ set.add(geo); set
                    case None ⇒ mutable.HashSet[NCGeoEntry](geo)
                }
            )
            
            geo match {
                case x: NCGeoContinent ⇒ conts += x
                case x: NCGeoSubContinent ⇒ subs += x
                case x: NCGeoCountry ⇒ cntrs += x
                case x: NCGeoRegion ⇒ regions += x
                case x: NCGeoCity ⇒ cities += x
                case x: NCGeoMetro ⇒ metro += x
                
                case _ ⇒ assert(assertion = false)
            }
        }

        // Country name -> country.
        val cntrMap = mutable.HashMap.empty[String, NCGeoCountry]

        // Subcontinent name -> continent.
        val subCont2ContMap = mutable.HashMap.empty[String, NCGeoContinent]
        
        // +====================+
        // | 1. Process metros. |
        // +====================+
        
        for (p ← NCJson.extractResource[List[JsonMetro]](METRO_PATH, ignoreCase = true))
            addEntry(p.name, NCGeoMetro(p.name), lowerCase = true)
        
        // +========================+
        // | 2. Process continents. |
        // +========================+
        
        for ((contName, subMap) ← NCJson.extractResource[
            immutable.Map[
                /* Continent name */String,
                /* Map of sub-continents */immutable.Map[String, List[JsonCountry]]
                ]](CONT_PATH, ignoreCase = true)) {
            val gCont = NCGeoContinent(contName)
            
            addEntry(contName, gCont, lowerCase = true)
            
            for ((subName, ctryList) ← subMap) {
                val gSub = NCGeoSubContinent(subName, gCont)

                subCont2ContMap += subName → gCont
                
                addEntry(subName, gSub, lowerCase = true)
                
                for (cntrName ← ctryList.map(_.name)) {
                    val gCntr = NCGeoCountry(cntrName, gSub)
                    
                    addEntry(cntrName, gCntr, lowerCase = true)
                    
                    cntrMap += cntrName → gCntr
                }
            }
        }
        
        // +=======================+
        // | 3. Process countries. |
        // +=======================+
        
        case class CityKey(
            city: String,
            region: String,
            country: String,
            subContinent: Option[String] = None,
            continent: Option[String] = None
        )
        val citiesMap = mutable.HashMap.empty[CityKey, NCGeoCity]
        
        for (f ← U.getFilesResources(CTRY_DIR)) {
            val cntr = NCJson.extractResource[JsonCountryHolder](f, ignoreCase = true)
            
            val gCntr = cntrMap(cntr.name)
            
            for (reg ← cntr.regions) {
                val gReg = NCGeoRegion(reg.name, gCntr)
                
                addEntry(reg.name, gReg, lowerCase = true)
                
                reg.cities.foreach(
                    jsCity ⇒ {
                        val city = NCGeoCity(jsCity.name, jsCity.latitude, jsCity.longitude, gReg)
                        
                        addEntry(jsCity.name, city, lowerCase = true)
                        
                        citiesMap += CityKey(jsCity.name, gReg.name, gCntr.name) → city
                        citiesMap +=
                            CityKey(
                                jsCity.name,
                                gReg.name,
                                gCntr.name,
                                Some(gCntr.subContinent.name),
                                Some(gCntr.subContinent.continent.name)
                            ) → city
                    }
                )
            }
        }
        
        // +======================+
        // | 4. Process synonyms. |
        // +======================+
        
        val dicts = NCDictionaryManager.get(NCDictionaryType.WORD_COMMON)
        
        def readJss(dir: String): Seq[String] =
            if (U.hasResource(dir)) U.getFilesResources(dir).filter(_.endsWith(".json")) else Seq.empty
        
        def extract(res: String, ignoreCase: Boolean): Seq[NCGeoSynonym] =
            NCJson.extractResource[List[NCGeoSynonym]](res, ignoreCase)
        def process(s: NCGeoSynonym, add: (Seq[String], NCGeoEntry) ⇒ Unit): Unit =
            s match {
                // Metro.
                case NCGeoSynonym(None, None, None, None, None, None, None, Some(m), syns) ⇒
                    add(syns, NCGeoMetro(m))
                
                // Continent.
                case NCGeoSynonym(None, None, None, None, None, None, Some(cont), None, syns) ⇒
                    add(syns, NCGeoContinent(cont))
                
                // Sub-continent (full representation).
                case NCGeoSynonym(None, None, None, None, None, Some(sub), Some(cont), None, syns) ⇒
                    add(syns, NCGeoSubContinent(sub, NCGeoContinent(cont)))

                // Sub-continent (short representation).
                case NCGeoSynonym(None, None, None, None, None, Some(sub), None, None, syns) ⇒
                    add(syns, NCGeoSubContinent(sub, subCont2ContMap(sub)))

                // Country (full representation).
                case NCGeoSynonym(None, None, None, None, Some(ctry), Some(sub), Some(cont), None, syns) ⇒
                    add(syns, NCGeoCountry(ctry, NCGeoSubContinent(sub, NCGeoContinent(cont))))
                
                // Country (short representation).
                case NCGeoSynonym(None, None, None, None, Some(ctry), None, None, None, syns) ⇒
                    add(syns, cntrMap(ctry))
                
                // Region (full representation).
                case NCGeoSynonym(None, None, None, Some(reg), Some(ctry), Some(sub), Some(cont), None, syns) ⇒
                    add(syns, NCGeoRegion(reg, NCGeoCountry(ctry, NCGeoSubContinent(sub,
                        NCGeoContinent(cont)))))
                
                // Region (short representation).
                case NCGeoSynonym(None, None, None, Some(reg), Some(ctry), None, None, None, syns) ⇒
                    add(syns, NCGeoRegion(reg, cntrMap(ctry)))
                
                // City (full representation).
                case NCGeoSynonym(Some(city), None, None, Some(reg), Some(ctry), Some(sub), Some(cont), None, syns) ⇒
                    add(syns, citiesMap(CityKey(city, reg, ctry, Some(sub), Some(cont))))
                
                // City (short representation).
                case NCGeoSynonym(Some(city), None, None, Some(reg), Some(ctry), None, None, None, syns) ⇒
                    add(syns, citiesMap(CityKey(city, reg, ctry)))
            }
        
        for (file ← readJss(SYNONYMS_DIR_PATH); s ← extract(file, ignoreCase = true))
            process(
                s,
                (syns: Seq[String], x: NCGeoEntry) ⇒
                    geoEntries.get(x.name.toLowerCase) match {
                        case Some(set) if set.contains(x) ⇒
                            // NCGeoSynonym shouldn't be matched with common dictionary word.
                            // Exception - manually defined synonyms.
                            syns.filter(s ⇒
                                SYNONYMS_MANUAL_FILES.exists(p ⇒ file.endsWith(s"/$p")) ||
                                    (!dicts.contains(s) &&
                                        !dicts.contains(s.replaceAll("the ", "")) &&
                                        !dicts.contains(s.replaceAll("the-", "")))
                            ).foreach(addEntry(_, x, lowerCase = true))
                        case _ ⇒ throw new NCE(s"Unknown synonym or its sub-component: $x")
                    }
            )
        
        // +=====================================+
        // | 5. Process case sensitive synonyms. |
        // +=====================================+
        
        for (file ← readJss(CASE_SENSITIVE_DIR_PATH); s ← extract(file, ignoreCase = false)) {
            def toLc(opt: Option[String]): Option[String] =
                opt match {
                    case Some(str) ⇒ Some(str.toLowerCase)
                    case None ⇒ None
                }
            
            process(
                NCGeoSynonym(
                    toLc(s.city),
                    s.latitude,
                    s.longitude,
                    toLc(s.region),
                    toLc(s.country),
                    toLc(s.subcontinent),
                    toLc(s.continent),
                    toLc(s.metro),
                    s.synonyms
                ),
                (syns: Seq[String], x: NCGeoEntry) ⇒
                    geoEntries.get(x.name) match {
                        // These synonyms are not checked with dictionaries etc.
                        // Case sensitive synonyms (like abbreviations) configuration used as is.
                        case Some(set) if set.contains(x) ⇒ syns.foreach(addEntry(_, x, lowerCase = false))
                        case _ ⇒ throw new NCE(s"Unknown synonym or its sub-component: $x")
                    }
            )
        }
        
        // Adds constructions like 'city LA' etc
        def addAux(geoSyn: String, geo: NCGeoEntry, auxes: Seq[String]): Unit =
            for (aux ← auxes if !geoSyn.split(" ").contains(aux)) {
                addEntry(s"$aux $geoSyn", geo, lowerCase = true)
                addEntry(s"$aux of $geoSyn", geo, lowerCase = true)
                addEntry(s"$geoSyn $aux", geo, lowerCase = true)
            }
        
        geoEntries.flatMap(p ⇒ p._2.map(_ → p._1)).foreach(p ⇒
            p._1 match {
                case _: NCGeoCity ⇒ addAux(p._2, p._1, CITY_AUX)
                case _: NCGeoRegion ⇒ addAux(p._2, p._1, REGION_AUX)
                case _ ⇒ // No-op.
            }
        )
        
        /**
          * Loads top cities from JSON res.
          *
          * @param path JSON res path.
          */
        def mkTopCities(path: String): immutable.Set[NCTopGeoCity] =
            NCJson.extractResource[List[JsTopCity]](path, ignoreCase = true).map(city ⇒
                cntrs.find(_.name == city.country) match {
                    case Some(country) ⇒
                        regions.find(r ⇒ r.name == city.region && r.country == country) match {
                            case Some(region) ⇒ NCTopGeoCity(city.name, region)
                            case None ⇒ throw new AssertionError(s"Region is not found: ${city.region}")
                        }
                    case None ⇒ throw new AssertionError(s"Country is not found: ${city.country}")
                }
            ).toSet
        
        NCGeoModel(
            geoEntries.map(p ⇒ p._1 → p._2.toSet).toMap,
            conts.toSet,
            subs.toSet,
            cntrs.toSet,
            regions.toSet,
            cities.toSet,
            metro.toSet,
            mkTopCities("geo/world_top.json"),
            mkTopCities("geo/us_top.json")
        )
    }
}
