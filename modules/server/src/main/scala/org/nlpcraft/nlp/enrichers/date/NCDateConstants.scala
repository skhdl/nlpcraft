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

package org.nlpcraft.nlp.enrichers.date

import org.nlpcraft.makro.NCMacroParser

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
    val DASHES = Seq(
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

    val DASHES_LIKE = Seq('/', ':', '~')
}