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

package org.nlpcraft.nlp.numeric

import java.text.{DecimalFormat, ParseException}
import java.util.Locale

import org.nlpcraft.NCLifecycle
import org.nlpcraft.nlp._
import org.nlpcraft.nlp.opennlp.NCNlpManager

case class NCNumericUnit(name: String, unitType: String)
case class NCNumeric(
    tokens: Seq[NCNlpSentenceToken],
    value: Double,
    isFractional: Boolean,
    unit: Option[NCNumericUnit]
)

/**
  * Numeric detection manager.
  */
object NCNumericManager extends NCLifecycle("Numeric manager") {
    // Sets EN numeric format.
    Locale.setDefault(Locale.forLanguageTag("EN"))

    private final val NUM_FMT = new DecimalFormat()

    @volatile private var genNums: Map[String, Int] = _
    @volatile private var unitsOrigs: Map[String, NCNumericUnit] = _
    @volatile private var unitsStem: Map[String, NCNumericUnit] = _
    @volatile private var maxSynWords: Int = 0

    private def toNumeric(s: String): Option[Double] =
        try
            Some(NUM_FMT.parse(s).doubleValue())
        catch {
            case _: ParseException ⇒ None
        }

    private def toString(seq: Seq[NCNlpSentenceToken], sep: String = " ", stem: Boolean = false) =
        seq.map(t ⇒ if (stem) t.stem else t.normText).mkString(sep)

    private def isFractional(s: String): Boolean = s.exists(_ == '.')

    private def mkSolidNumUnit(t: NCNlpSentenceToken): Option[NCNumeric] = {
        val s = t.origText

        val num = s.takeWhile(_.isDigit)
        val after = s.drop(num.length)

        if (num.nonEmpty && after.nonEmpty) {
            def mkNumeric(u: NCNumericUnit): Option[NCNumeric] =
                Some(NCNumeric(Seq(t), java.lang.Double.valueOf(num), isFractional = isFractional(num), unit = Some(u)))

            unitsOrigs.get(after) match {
                case Some(u) ⇒ mkNumeric(u)
                case None ⇒
                    unitsStem.get(NCNlpManager.stem(after)) match {
                        case Some(u) ⇒ mkNumeric(u)
                        case None ⇒ None
                    }
            }
        }
        else
            None
    }


    /**
      * Starts this component.
      */
    override def start(): NCLifecycle = {
        ensureStopped()

        genNums = NCNumericGenerator.generate(100000).map(p ⇒ p._2 → p._1)

        // Data source: https://www.adducation.info/how-to-improve-your-knowledge/units-of-measurement/
        // Note that this provided data edited.
        case class U(name: String, unitType: String, synonyms: Seq[String]) {
            val extSynonyms: Seq[String] =
                synonyms ++
                    // Extends by dot for shortenings, like "mm" → "mm ."
                    // Skips whole words and constructions like `ft/s`
                    synonyms.filter(_.length <= 3).filter(p ⇒ !p.exists(_ == '/')).
                        // To avoid difference in tokenization behaviour.
                        flatMap(p ⇒ Seq(s"$p .", s"$p."))

            val stem: String = NCNlpManager.stem(name)
        }

        val hs =
            Seq(
                U("meter", "length", Seq("metre", "m")),
                U("millimeter", "length", Seq("mm")),
                U("centimeter", "length", Seq("cm")),
                U("decimeter", "length", Seq("dm")),
                U("kilometer", "length", Seq("km")),
                U("astronomical unit", "length", Seq("ae")),
                U("light year", "length", Seq("lj")),
                U("parsec", "length", Seq("pc")),
                U("inch", "length", Seq("in")),
                U("foot", "length", Seq("ft")),
                U("yard", "length", Seq("yd")),
                U("mile", "length", Seq("mi")),
                U("nautical mile", "length", Seq("sm")),
                U("square meter", "area", Seq("sqm", "m2")),
                U("acre", "area", Seq.empty),
                U("are", "area", Seq("a", "ares")),
                U("hectare", "area", Seq("ha")),
                U("square inches", "area", Seq("in2")),
                U("square feet", "area", Seq("ft2")),
                U("square yards", "area", Seq("yd2")),
                U("square miles", "area", Seq("mi2")),
                U("cubic meter", "volume", Seq("m3")),
                U("liter", "volume", Seq("l")),
                U("milliliter", "volume", Seq("ml")),
                U("centiliter", "volume", Seq("cl")),
                U("deciliter", "volume", Seq("dl")),
                U("hectoliter", "volume", Seq("hl")),
                U("cubic inch", "volume", Seq("cu in", "in3")),
                U("cubic foot", "volume", Seq("cu ft", "ft3")),
                U("cubic yard", "volume", Seq("cu yd", "yd3")),
                U("acre-foot", "volume", Seq("acre ft")),
                U("teaspoon", "volume", Seq("tsp")),
                U("tablespoon", "volume", Seq("tbsp")),
                U("fluid ounce", "volume", Seq("fl oz", "oz. fl")),
                U("cup", "volume", Seq.empty),
                U("gill", "volume", Seq.empty),
                U("pint", "volume", Seq("pt", "p")),
                U("quart", "volume", Seq("qt")),
                U("gallon", "volume", Seq("gal")),
                U("radian", "angle", Seq("rad")),
                U("degree", "angle", Seq("deg")),
                U("steradian", "solid angle", Seq.empty),
                U("second", "datetime", Seq("s", "sec", "secs")),
                U("minute", "datetime", Seq("min", "mins")),
                U("hour", "datetime", Seq("h", "hr")),
                U("day", "datetime", Seq("d")),
                U("week", "datetime", Seq.empty),
                U("month", "datetime", Seq("months")),
                U("year", "datetime", Seq("y")),
                U("hertz", "frequency", Seq("hz")),
                U("angular frequency", "frequency", Seq.empty),
                U("decibel", "sound", Seq("db")),
                U("kilogram meters per second", "momentum", Seq("kg m/s")),
                U("miles per hour", "speed", Seq("mph")),
                U("meters per second", "speed", Seq("m/s", "kph")),
                U("gravity imperial", "acceleration of gravity", Seq("ft/s2")),
                U("gravity metric", "acceleration of gravity", Seq("m/s2")),
                U("feet per second", "mass", Seq("ft/s")),
                U("grams", "mass", Seq("g")),
                U("kilogram", "mass", Seq("kg")),
                U("grain", "mass", Seq("gr")),
                U("dram", "mass", Seq("dr")),
                U("ounce", "mass", Seq("oz")),
                U("pound", "mass", Seq("lb")),
                U("hundredweight", "mass", Seq("hundred weight", "cwt")),
                U("ton", "mass", Seq.empty),
                U("tonne", "mass", Seq("t")),
                U("slug", "mass", Seq.empty),
                U("density", "density", Seq("kg/m3")),
                U("newton", "force, weight", Seq("n")),
                U("kilopond", "force", Seq("kp")),
                U("pond", "force", Seq.empty),
                U("newton meter", "torque", Seq.empty),
                U("joule", "work, energy", Seq("j")),
                U("watt", "power, radiant flux", Seq("w")),
                U("kilowatt", "power", Seq("kw")),
                U("horsepower", "power", Seq("hp")),
                U("pascal", "pressure, stress", Seq("pa")),
                U("bar", "power", Seq.empty),
                U("pounds per square inch", "pressure", Seq("psi", "lbf/in2")),
                U("kelvin", "temperature", Seq("k")),
                U("centigrade", "temperature", Seq.empty),
                U("calorie", "amount of heat", Seq("cal")),
                U("fahrenheit", "temperature", Seq.empty),
                U("candela", "luminous intensity", Seq("cd")),
                U("candela per square metre", "luminance", Seq("cd/m2")),
                U("lumen", "luminous flux", Seq("lm")),
                U("lux", "illuminance", Seq("lx")),
                U("lumen seconds", "light quantity", Seq("ls")),
                U("diopter", "refractive index", Seq("dpt")),
                U("ampere", "current", Seq("amps")),
                U("coulomb", "electric charge", Seq("c")),
                U("volt", "voltage, electrical", Seq("v")),
                U("ohm", "electrical resistance, impedence", Seq.empty),
                U("farad", "electrical capacitance", Seq("f")),
                U("siemens", "electrical conductance", Seq.empty),
                U("henry", "electrical inductance", Seq.empty),
                U("weber", "magnetic flux", Seq("wb")),
                U("tesla", "magnetic flux density, magnetic field", Seq("(t)")),
                U("becquerel", "radioactive decay", Seq("bq")),
                U("mole", "amount of substance", Seq("mol")),
                U("paper bale", "paper quantity", Seq("ream")),
                U("dozen", "quantities", Seq("dz", "doz"))
            )

        def check(synonyms: Seq[String]): Unit = {
            val sd = synonyms.distinct

            require(synonyms.size == sd.size, s"Duplicated synonyms: ${synonyms.diff(sd).distinct.mkString(", ")}")
        }

        check(hs.flatMap(_.extSynonyms))
        check(hs.map(_.stem))

        unitsOrigs = hs.flatMap(p ⇒ p.extSynonyms.map(s ⇒ s → NCNumericUnit(p.name, p.unitType))).toMap
        unitsStem = hs.map(p ⇒ p.stem → NCNumericUnit(p.name, p.unitType)).toMap
        maxSynWords = (unitsOrigs ++ unitsStem).keySet.map(_.split(" ").length).max

        super.start()
    }

    /**
      * Gets `numerics` which found in the given sentence.
      *
      * @param ns Sentence.
      */
    def find(ns: NCNlpSentence): Seq[NCNumeric] = {
        // 1. For some reasons Stanford doesn't mark as CD some numerics - for example "eleven".
        // 2. NE as NUMBER value is not enough as condition too because sometimes number words be marked as DURATIONs.
        // Also we have to filter by POS because for sentences like `between 11 and 12`
        // word 'and' marked as NUMBER (POS CC)
        // 3. Condition CD + letter or digit - to avoid detection as CD some symbols like `==`
        // (unexpected Stanford behaviour)
        def isDigitChar(ch: Char): Boolean = Character.isDigit(ch) || ch == '.'
        def isOk(ch: Char): Boolean = isDigitChar(ch) || Character.isLetter(ch)

        val cds = ns.filter(p ⇒ p.pos == "CD" && p.origText.forall(isOk) || p.pos.head == "N")

        def isDigitText(normTxt: String): Boolean = normTxt.forall(isDigitChar)

        val grpd: Seq[Seq[NCNlpSentenceToken]] =
            cds.groupBy(cd ⇒ {
                var i = cds.indexOf(cd)

                if (i == 0)
                    cd
                else {
                    while (
                        i > 0 &&
                        cds(i - 1).index + 1 == cds(i).index &&
                        !isDigitText(cds(i - 1).stem) &&
                        !isDigitText(cds(i).stem)
                    )
                        i = i - 1

                    cds(i)
                }
            }).toSeq.map(_._2)

        val nums = grpd.flatMap(seq ⇒ {
            def mkNum(v: Double, isFractional: Boolean): NCNumeric = {
                // Units synonyms are not stemmed.
                Range.inclusive(1, maxSynWords).reverse.toStream.flatMap(i ⇒ {
                    val afterNum = ns.slice(seq.last.index + 1, seq.last.index + i + 1)

                    if (afterNum.nonEmpty && !afterNum.exists(cds.contains)) {
                        def process(syns: Map[String, NCNumericUnit], getter: NCNlpSentenceToken ⇒ String):
                        Option[(NCNumericUnit, Seq[NCNlpSentenceToken])] = {
                            val str = afterNum.map(getter).mkString(" ")

                            syns.get(str) match {
                                case Some(unit) ⇒ Some((unit, afterNum))
                                case None ⇒ None
                            }
                        }

                        process(unitsOrigs, (t: NCNlpSentenceToken) ⇒ t.normText) match {
                            case Some(p) ⇒ Some(p)
                            case None ⇒ process(unitsStem, (t: NCNlpSentenceToken) ⇒ t.stem)
                        }
                    }
                    else
                        None
                }).headOption match {
                    case Some((unit, unitToks)) ⇒ NCNumeric(seq ++ unitToks, v, isFractional = isFractional, Some(unit))
                    case None ⇒ NCNumeric(seq, v, isFractional = isFractional, None)
                }
        }

        seq.size match {
            case 1 ⇒
                val txt = seq.head.normText

                genNums.get(txt) match {
                    case Some(intVal) ⇒ Some(mkNum(intVal.toDouble, isFractional = false))
                    case None ⇒
                        toNumeric(txt) match {
                            case Some(dblVal) ⇒ Some(mkNum(dblVal, isFractional = isFractional(txt)))
                            case None ⇒ None
                        }
                }
            case _ ⇒
                genNums.get(toString(seq)) match {
                    case Some(intVal) ⇒ Some(mkNum(intVal.toDouble, isFractional = false))
                    // Try to parse space separated numerics 1 000 000.
                    case None ⇒
                        // To skip mixed variants like: twenty 2, 22 twenty.
                        if (seq.forall(t ⇒ toNumeric(t.normText).isDefined)) {
                            val txt = toString(seq, "")

                            toNumeric(txt) match {
                                case Some(dblVal) ⇒ Some(mkNum(dblVal, isFractional = isFractional(txt)))
                                case None ⇒ None
                            }
                        }
                        else
                            None
                }
        }})

        val usedToks = nums.flatMap(_.tokens)

        (nums ++ ns.filter(t ⇒ !usedToks.contains(t)).flatMap(mkSolidNumUnit)).sortBy(_.tokens.head.index)
    }
}
