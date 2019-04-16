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
}