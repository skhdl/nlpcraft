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

import java.text.SimpleDateFormat
import java.time._

object NCDateRange {
    private final val UTC = ZoneId.of("UTC")
    private final val FMT = new SimpleDateFormat("MM-dd-yyyy")
    
    val MAX_VALUE: Long = Long.MaxValue
    val MIN_VALUE: Long = FMT.parse("01-01-0001").getTime

    /**
      * Constructors for date range.
      * 
      * @param from
      * @param to
      * @param text
      * @param periods
      * @return
      */
    def apply(from: Long, to: Long, text: String, periods: Seq[String]) =
        new NCDateRange(from, to, text, periods)
    
    /**
      *
      * @param from
      * @param to
      * @return
      */
    def apply(from: Long, to: Long) = new NCDateRange(from, to, "?", Seq.empty)
    
    /**
      *
      * @param l
      * @param limit
      * @param dflt
      * @return
      */
    private def long2String(l: Long, limit: Long, dflt: String): String =
        if (l == limit) dflt else FMT.format(new java.util.Date(l))
}

import org.nlpcraft.server.nlp.enrichers.date.NCDateRange._

/**
  * Date range data holder.
  */
class NCDateRange(val from: Long, val to: Long, val text: String, val periods: Seq[String]) {
    private val hash = 31 * from.hashCode() + to.hashCode()
    
    /**
      *
      * @return
      */
    def isComplex: Boolean = periods.contains(":")
    
    /**
      *
      * @return
      */
    def isAggregator: Boolean = text == "?"
    
    /**
      *
      * @return
      */
    def isFromNegativeInfinity: Boolean = from == MIN_VALUE
    
    /**
      *
      * @return
      */
    def isToPositiveInfinity: Boolean = to == MAX_VALUE
    
    /**
      * Creates date range with inclusive upper bound.
      *
      * @return
      */
    def mkInclusiveDateRange: NCDateRange = {
        var dt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(to), UTC)
        
        if (dt.getHour == 0 && dt.getMinute == 0 && dt.getSecond == 0)
            dt = dt.minusSeconds(1)
        
        new NCDateRange(from, Instant.from(dt).toEpochMilli, text, periods)
    }
    
    /**
      * Gets `include` flag.
      *
      * @param range Range.
      */
    def include(range: NCDateRange): Boolean = from <= range.from && to >= range.to
    
    /**
      * Gets length in milliseconds.
      */
    def length: Long = to - from
    
    override def equals(other: Any): Boolean = other match {
        case it: NCDateRange ⇒ from == it.from && to == it.to
        case _ ⇒ false
    }
    
    override def hashCode(): Int = hash
    
    override def toString: String =
        s"[${long2String(from, MIN_VALUE, "MIN_VALUE")}:${long2String(to, MAX_VALUE, "MAX_VALUE")}]"
}

