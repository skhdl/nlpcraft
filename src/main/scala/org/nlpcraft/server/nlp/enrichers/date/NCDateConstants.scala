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

package org.nlpcraft.server.nlp.enrichers.date

import org.nlpcraft.common.makro.NCMacroParser
import org.nlpcraft.server.nlp.enrichers.date.NCDateFormatType._

/**
 * Date enricher constants.
 */
private[date] object NCDateConstants {
    private final val parser = NCMacroParser()
    
    val BETWEEN_INCLUSIVE: Seq[(String, String)] = Seq(
        ("from", "to"),
        ("since", "to"),
        ("since", "till"),
        ("from", "till")
    )

    val BETWEEN_EXCLUSIVE: Seq[(String, String)] = Seq(
        ("between", "and")
    )

    val FROM: Seq[String] = Seq(
        "from",
        "after",
        "starting from",
        "from start of",
        "beginning from",
        "since",
        "starting since",
        "beginning since",
        "from beginning of"
    )

    val TO: Seq[String] = Seq(
        "to",
        "till",
        "until"
    )

    val FOR: Seq[String] = Seq(
        "for",
        "during",
        "during {the|*} {time|*} period of",
        "within",
        "within {the|*} {time|*} period of",
        "of",
        "for {the|*} {time|*} period of"
    ).flatMap(parser.expand)

    val AGO: Seq[String] = Seq(
        "ago",
        "before",
        "back"
    )

    val FORWARD: Seq[String] = Seq(
        "next",
        "following",
        "forward"
    )

    val LAST: Seq[String] = Seq(
        "last"
    )

    val FIRST: Seq[String] = Seq(
        "first"
    )

    val ON: Seq[String] = Seq(
        "on",
        "in",
        "for"
    )

    val PREVIOUS: Seq[String] = Seq(
        "previous",
        "past",
        "earlier",
        "preceding"
    )

    val CURRENT: Seq[String] = Seq(
        "this",
        "current",
        "present"
    )

    val NEXT: Seq[String] = Seq(
        "next",
        "following",
        "future"
    )

    // Formats below will be duplicated.
    // They are grouped according to duplication rules.

    // 'dd' will be duplicated as 'd'
    // 'dd' will be duplicated as 'dth' (st, rd)
    val YEAR_MONTH_DAY1: Seq[String] = Seq(
        "{yy|yyyy} dd {'of'|'for'|*} {MMMM|MMM}",
        "dd {'of'|'for'|*} {MMMM|MMM} {'of'|'for'|*} yyyy"
    ).flatMap(parser.expand)

    // 'dd' will be duplicated as 'd'.
    val YEAR_MONTH_DAY2: Seq[String] = Seq(
        "{yy|yyyy} {MMMM|MMM} dd",
        "{MMMM|MMM} dd {yyyy|yy}"
    ).flatMap(parser.expand)

    // 'dd' will be duplicated as 'd'.
    // 'MM' will be duplicated as 'M'.
    val YEAR_MONTH_DAY3: Seq[String] = Seq(
        "yyyy.MM.dd",
        "yy.MM.dd",
        "yyyy/MM/dd",
        "MM/dd/yyyy",
        "yy/MM/dd",
        "yyyy-MM-dd",
        "MM-dd-yyyy",
        "yy-MM-dd"
    )

    // 'dd' will be duplicated as 'd'
    // 'dd' will be duplicated as 'dth' (st, rd)
    val MONTH_DAY1: Seq[String] = Seq(
        "dd {'of'|*} {MMMM|MMM}"
    ).flatMap(parser.expand)

    // 'dd' will be duplicated as 'd'.
    val MONTH_DAY2: Seq[String] = Seq(
        "{MMM|MMMM} dd"
    ).flatMap(parser.expand)

    // Without duplicates.
    val MONTH_DAY3: Seq[String] = Seq(
        "MM.dd",
        "MM/dd",
        "MM-dd"
    )

    val MONTH_YEAR: Seq[String] = Seq(
        "{'month of'|*} {MMMM|MMM} {'of'|'for'|*} {'year'|'year of'|*} {yy|yyyy} {'year'|*}",
        "yyyy {MMMM|MMM}",
        "{'month of'|*} {MMMMyyyy|MMMyyyy|MMMMyy|MMMyy}",
        "{'month of'|*} {yyyyMMMM|yyyyMMM|yyMMMM|yyMMM}",
        "{'month of'|*} {MM.yyyy|MM.yy|MM/yyyy|MM/yy|MM-yyyy|MM-yy|yyyy.MM|yyyy/MM|yyyy-MM}"
    ).flatMap(parser.expand)

    val YEAR: Seq[String] = Seq(
        "'year' {'of'|'for'|*} {yyyy|yy}",
        "{yy|yyyy} 'year'",
        "yyyy"
    ).flatMap(parser.expand)

    // https://www.compart.com/en/unicode/category/Pd
    val DASHES: Seq[Char] = Seq(
        '\u002D',
        '\u058A',
        '\u05BE',
        '\u1400',
        '\u1806',
        '\u2010',
        '\u2011',
        '\u2012',
        '\u2013',
        '\u2014',
        '\u2015',
        '\u2E17',
        '\u2E1A',
        '\u2E3A',
        '\u2E3B',
        '\u2E40',
        '\u301C',
        '\u3030',
        '\u30A0',
        '\uFE31',
        '\uFE32',
        '\uFE58',
        '\uFE63',
        '\uFF0D'
    )

    val DASHES_LIKE: Seq[Char] = Seq('/', ':', '~')

    // https://www.wikiwand.com/en/Date_format_by_country
    val COUNTRIES: Map[String, Seq[NCDateFormatType]] = Map(
        "AF" → Seq(YMD, DMY),
        "AX" → Seq(YMD, DMY),
        "AL" → Seq(YMD, DMY),
        "DZ" → Seq(DMY),
        "AS" → Seq(MDY),
        "AD" → Seq(DMY),
        "AO" → Seq(DMY),
        "AI" → Seq(DMY),
        "AG" → Seq(DMY),
        "AR" → Seq(DMY),
        "AM" → Seq(DMY),
        "AW" → Seq(DMY),
        "AU" → Seq(YMD, DMY),
        "AT" → Seq(YMD, DMY),
        "AZ" → Seq(DMY),
        "BS" → Seq(DMY),
        "BH" → Seq(DMY),
        "BD" → Seq(DMY),
        "BB" → Seq(DMY),
        "BY" → Seq(DMY),
        "BE" → Seq(DMY),
        "BZ" → Seq(DMY),
        "BJ" → Seq(DMY),
        "BM" → Seq(DMY),
        "BT" → Seq(YMD),
        "BO" → Seq(DMY),
        "BQ" → Seq(DMY),
        "BA" → Seq(DMY),
        "BW" → Seq(YMD, DMY),
        "BR" → Seq(DMY),
        "IO" → Seq(DMY),
        "VG" → Seq(DMY),
        "BN" → Seq(DMY),
        "BG" → Seq(DMY),
        "BF" → Seq(DMY),
        "BI" → Seq(DMY),
        "KH" → Seq(DMY),
        "CM" → Seq(YMD, DMY),
        "CA" → Seq(YMD),
        "CV" → Seq(DMY),
        "KY" → Seq(DMY, MDY),
        "CF" → Seq(DMY),
        "TD" → Seq(DMY),
        "CL" → Seq(DMY),
        "CN" → Seq(YMD),
        "CX" → Seq(DMY),
        "CC" → Seq(DMY),
        "CO" → Seq(DMY),
        "KM" → Seq(DMY),
        "CD" → Seq(DMY),
        "CK" → Seq(DMY),
        "CR" → Seq(DMY),
        "HR" → Seq(DMY),
        "CU" → Seq(YMD, DMY),
        "CW" → Seq(DMY),
        "CY" → Seq(DMY),
        "CZ" → Seq(DMY),
        "DK" → Seq(YMD, DMY),
        "DJ" → Seq(YMD, DMY),
        "DM" → Seq(DMY),
        "DO" → Seq(DMY),
        "TL" → Seq(DMY),
        "EC" → Seq(DMY),
        "EG" → Seq(DMY),
        "SV" → Seq(DMY),
        "GQ" → Seq(DMY),
        "ER" → Seq(YMD, DMY),
        "EE" → Seq(DMY),
        "ET" → Seq(DMY),
        "FK" → Seq(DMY),
        "FO" → Seq(DMY),
        "FM" → Seq(MDY),
        "FI" → Seq(DMY),
        "FJ" → Seq(DMY),
        "FR" → Seq(YMD, DMY),
        "GF" → Seq(DMY),
        "PF" → Seq(DMY),
        "GA" → Seq(DMY),
        "GM" → Seq(DMY),
        "GE" → Seq(DMY),
        "DE" → Seq(YMD, DMY),
        "GH" → Seq(YMD, DMY, MDY),
        "GI" → Seq(DMY),
        "GR" → Seq(DMY),
        "GL" → Seq(DMY, MDY),
        "GD" → Seq(DMY),
        "GP" → Seq(DMY),
        "GU" → Seq(MDY),
        "GT" → Seq(DMY),
        "GG" → Seq(DMY),
        "GN" → Seq(YMD, DMY),
        "GW" → Seq(DMY),
        "GY" → Seq(DMY),
        "HT" → Seq(DMY),
        "HK" → Seq(YMD, DMY),
        "HN" → Seq(DMY),
        "HU" → Seq(YMD),
        "IS" → Seq(DMY),
        "IN" → Seq(YMD, DMY),
        "ID" → Seq(DMY),
        "IR" → Seq(YMD, DMY),
        "IQ" → Seq(DMY),
        "IE" → Seq(DMY),
        "IM" → Seq(DMY),
        "IL" → Seq(DMY),
        "IT" → Seq(DMY),
        "JM" → Seq(YMD, DMY),
        "SJ" → Seq(DMY),
        "JP" → Seq(YMD),
        "JE" → Seq(DMY),
        "JO" → Seq(DMY),
        "KZ" → Seq(DMY),
        "KE" → Seq(YMD, DMY, MDY),
        "KI" → Seq(DMY),
        "KP" → Seq(YMD),
        "KR" → Seq(YMD),
        "KW" → Seq(DMY),
        "KG" → Seq(DMY),
        "LA" → Seq(DMY),
        "LV" → Seq(DMY),
        "LB" → Seq(DMY),
        "LS" → Seq(YMD, DMY),
        "LR" → Seq(DMY),
        "LY" → Seq(DMY),
        "LI" → Seq(DMY),
        "LT" → Seq(YMD),
        "LU" → Seq(YMD, DMY),
        "MG" → Seq(DMY),
        "MW" → Seq(DMY),
        "MY" → Seq(DMY, MDY),
        "MV" → Seq(YMD, DMY),
        "ML" → Seq(DMY),
        "MT" → Seq(DMY),
        "MH" → Seq(MDY),
        "MQ" → Seq(DMY),
        "MR" → Seq(DMY),
        "MU" → Seq(DMY),
        "YT" → Seq(DMY),
        "MX" → Seq(DMY),
        "MD" → Seq(DMY),
        "MC" → Seq(DMY),
        "MN" → Seq(YMD),
        "ME" → Seq(DMY),
        "MS" → Seq(DMY),
        "MA" → Seq(DMY),
        "MZ" → Seq(DMY),
        "MM" → Seq(YMD, DMY),
        "NA" → Seq(YMD, DMY),
        "NR" → Seq(DMY),
        "NP" → Seq(YMD, DMY),
        "NL" → Seq(DMY),
        "NC" → Seq(DMY),
        "NZ" → Seq(DMY),
        "NI" → Seq(DMY),
        "NE" → Seq(DMY),
        "NG" → Seq(DMY),
        "NU" → Seq(DMY),
        "NF" → Seq(DMY),
        "MK" → Seq(DMY),
        "MP" → Seq(MDY),
        "NO" → Seq(YMD, DMY),
        "OM" → Seq(DMY),
        "PK" → Seq(DMY),
        "PS" → Seq(DMY),
        "PW" → Seq(DMY),
        "PA" → Seq(DMY, MDY),
        "PG" → Seq(DMY),
        "PY" → Seq(DMY),
        "PE" → Seq(DMY),
        "PH" → Seq(DMY, MDY),
        "PN" → Seq(DMY),
        "PL" → Seq(YMD, DMY),
        "PT" → Seq(YMD, DMY),
        "PR" → Seq(DMY, MDY),
        "QA" → Seq(DMY),
        "RO" → Seq(DMY),
        "RU" → Seq(YMD, DMY),
        "RW" → Seq(YMD, DMY),
        "BL" → Seq(DMY),
        "SH" → Seq(DMY),
        "KN" → Seq(DMY),
        "LC" → Seq(DMY),
        "MF" → Seq(DMY),
        "PM" → Seq(DMY),
        "VC" → Seq(DMY),
        "WS" → Seq(DMY),
        "SM" → Seq(DMY),
        "SA" → Seq(DMY),
        "SN" → Seq(DMY),
        "RS" → Seq(DMY),
        "SC" → Seq(DMY),
        "SL" → Seq(DMY),
        "SG" → Seq(YMD, DMY),
        "SX" → Seq(DMY),
        "SK" → Seq(DMY),
        "SI" → Seq(DMY),
        "SB" → Seq(DMY),
        "SO" → Seq(DMY, MDY),
        "ZA" → Seq(YMD, DMY, MDY),
        "ES" → Seq(YMD, DMY),
        "LK" → Seq(YMD, DMY),
        "SD" → Seq(DMY),
        "SS" → Seq(DMY),
        "SR" → Seq(DMY),
        "SJ" → Seq(DMY),
        "SE" → Seq(YMD, DMY),
        "CH" → Seq(YMD, DMY),
        "SY" → Seq(DMY),
        "TW" → Seq(YMD),
        "TJ" → Seq(DMY),
        "TZ" → Seq(DMY),
        "TH" → Seq(DMY),
        "TG" → Seq(DMY, MDY),
        "TK" → Seq(DMY),
        "TO" → Seq(DMY),
        "TT" → Seq(DMY),
        "TN" → Seq(DMY),
        "TR" → Seq(DMY),
        "TM" → Seq(DMY),
        "TC" → Seq(DMY),
        "TV" → Seq(DMY),
        "UG" → Seq(DMY),
        "UA" → Seq(DMY),
        "AE" → Seq(DMY),
        "GB" → Seq(YMD, DMY),
        "UM" → Seq(MDY),
        "US" → Seq(YMD, MDY),
        "VI" → Seq(MDY),
        "UY" → Seq(DMY),
        "UZ" → Seq(YMD, DMY),
        "VU" → Seq(DMY),
        "VE" → Seq(DMY),
        "VN" → Seq(YMD, DMY),
        "WF" → Seq(DMY),
        "YE" → Seq(DMY),
        "ZM" → Seq(DMY),
        "ZW" → Seq(DMY)
    )
}