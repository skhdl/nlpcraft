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

import java.time.Instant
import java.util
import java.util.{Calendar => C}

import org.nlpcraft.common.{NCLifecycle, _}
import org.nlpcraft.common.nlp.{NCNlpSentence, NCNlpSentenceNote, NCNlpSentenceToken}
import org.nlpcraft.server.nlp.enrichers.NCNlpEnricher
import org.nlpcraft.server.nlp.enrichers.date.NCDateConstants._

import scala.collection.immutable.Iterable
import scala.collection.mutable
import scala.collection.mutable.{LinkedHashMap => LHM}
import scala.collection.JavaConverters._

/**
  * Date enricher.
  */
object NCDateEnricher extends NCNlpEnricher("Date enricher") {
    private type LHM_SS = LHM[String, String]
    
    // Correctness is not checked (double spaces etc).
    private[date] val prepsFrom = mkPrepositions(FROM)
    private[date] val prepsTo = mkPrepositions(TO)
    private[date] val prepsOn = mkPrepositions(ON)
    private[date] val prepsBtwIncl = mkBetweenPrepositions(BETWEEN_INCLUSIVE)
    private[date] val prepsBtwExcl = mkBetweenPrepositions(BETWEEN_EXCLUSIVE)
    
    @volatile private var cacheFull: LHM_SS = _
    @volatile private var cacheParts: LHM_SS = _

    // Preposition data holder.
    case class P(text: String) {
        val words: Seq[String] = text.split(" ").filter(!_.trim.isEmpty).toSeq
        val length: Int = words.length
    }

    // Function's data holder.
    case class F(
        tokens: Seq[NCNlpSentenceToken],
        body: String,
        isFull: Boolean,
        var isProcessed: Boolean = false) {
        def words: Seq[String] = tokens.map(_.normText)
    }

    // Date data holder.
    case class D(
        function: F,
        length: Int // Preposition length.
    )

    // Simple range data holder.
    case class R(
        function: F,
        length: Int, // Preposition length.
        isFromType: Boolean,
        inclusive: Boolean
    )

    // Complex range data holder.
    case class CRD(
        from: F,
        to: F,
        dash: Seq[NCNlpSentenceToken]
    )

    case class CR(
        from: F,
        fromLength: Int, // Preposition length.
        fromInclusive: Boolean,
        to: F,
        toLength: Int, // Preposition length.
        toInclusive: Boolean
    )

    // Time holder.
    case class T(
        tokens: Seq[NCNlpSentenceToken],
        body: Option[String]
    )

    // Time period holder.
    case class TP(
        tokens: Seq[NCNlpSentenceToken],
        body: String
    )

    /**
      * Starts manager.
      */
    override def start(): NCLifecycle = {
        ensureStopped()

        def read(res: String): LHM_SS = {
            val m: LHM_SS = new LHM_SS()

            val map = U.readTextGzipResource(res, "UTF-8", logger).map(p ⇒ {
                val pair = p.split("\\|")

                pair.head.trim → pair.last.trim
            })

            m ++= map

            m
        }

        cacheFull = read("date/full.txt.gz")
        cacheParts = read("date/parts.txt.gz")

        super.start()
    }

    @throws[NCE]
    override def enrich(ns: NCNlpSentence) {
        // This stage must not be 1st enrichment stage.
        assume(ns.nonEmpty)

        require(cacheFull != null)
        require(cacheParts != null)

        val base = U.nowUtcMs()

        val dates = findDates(ns)

        val partsDates = dates.filter(!_.isFull)
        val fullDates = dates.filter(_.isFull)

        def startWith(f: F, prepLen: Int, slice: Seq[String]): Boolean = {
            val startIdx = f.tokens.head.index

            val p1 = ns.take(startIdx).filter(!_.isStopword).takeRight(prepLen)
            val p2 = ns.drop(startIdx).filter(!_.isStopword)

            (p1 ++ p2).map(_.normText).startsWith(slice)
        }

        def findComplexes(ps: Seq[(P, P)], fromIncl: Boolean, toIncl: Boolean): Seq[CR] = {
            val buf = mutable.Buffer.empty[CR]

            for (pair ← partsDates.sliding(2) if !pair.exists(_.isProcessed)) {
                val from = pair.head
                val to = pair.last

                ps.find(p ⇒ startWith(from, p._1.length, p._1.words ++ from.words ++ p._2.words ++ to.words)) match {
                    case Some(e) ⇒
                        buf += CR(from, e._1.length, fromIncl, to, e._2.length, toIncl)

                        mark(from, to)
                    case None ⇒ // No-op.
                }
            }

            buf
        }

        def isDash(toks: Seq[NCNlpSentenceToken]): Boolean = {
            def isDashChar(t: NCNlpSentenceToken): Boolean = t.origText.forall(ch ⇒ DASHES.contains(ch) || DASHES_LIKE.contains(ch))

            toks.exists(isDashChar) && toks.forall(t ⇒ t.isStopword || isDashChar(t))
        }

        def findComplexDash(): Seq[CRD] = {
            val buf = mutable.Buffer.empty[CRD]

            for (pair ← dates.sliding(2) if !pair.exists(_.isProcessed)) {
                val from = pair.head
                val to = pair.last

                val between = ns.slice(from.tokens.last.index + 1, to.tokens.head.index)

                if (between.nonEmpty && isDash(between)) {
                    mark(from, to)

                    buf += CRD(from, to, between)
                }
            }

            buf
        }

        def findSimples[T](ps: Seq[P], mkHolder: (F, P) ⇒ T): Seq[T] = {
            val buf = mutable.Buffer.empty[T]

            for (f ← partsDates.filter(!_.isProcessed))
                ps.find(p ⇒ startWith(f, p.length, p.words ++ f.words)) match {
                    case Some(p) ⇒
                        buf += mkHolder(f, p)

                        mark(f)
                    case None ⇒ None
                }

            buf
        }

        def withBefore(tokens: Seq[NCNlpSentenceToken], lenBefore: Int) =
            ns.take(tokens.head.index).filter(!_.isStopword).takeRight(lenBefore) ++ tokens

        /*
         * Finds and adds ranges and dates (complex dates should be processed first).
         */

        // Between:and, from:to - complex ranges.
        val complexRanges =
            findComplexes(prepsBtwIncl, fromIncl = true, toIncl = true) ++
            findComplexes(prepsBtwExcl, fromIncl = true, toIncl = false)

        for (r ← complexRanges) {
            val body = s"${r.from.body}:${r.to.body}"
            val toks = withBefore(r.from.tokens, r.fromLength) ++ withBefore(r.to.tokens, r.toLength)

            addNote(body, r.fromInclusive, r.toInclusive, toks, base)
        }

        // From, to - simple ranges.
        val simpleRanges =
            findSimples(prepsFrom, (f: F, p: P) ⇒ R(f, p.length, isFromType = true, inclusive = true)) ++
            findSimples(prepsTo, (f: F, p: P) ⇒ R(f, p.length, isFromType = false, inclusive = true))

        for (r ← simpleRanges) {
            val b = r.function.body
            val body = if (r.isFromType) s"$b:" else s":$b"
            val toks = withBefore(r.function.tokens, r.length)

            addNote(body, r.inclusive, r.inclusive, toks, base)
        }

        for (r ← findComplexDash()) {
            val body = s"${r.from.body}:${r.to.body}"
            val toks = r.from.tokens ++ r.dash ++ r.to.tokens

            addNote(body, fromIncl = true, toIncl = false, toks, base)
        }

        // On, in, for - concrete periods.
        val simpleDates = findSimples(prepsOn, (f: F, p: P) ⇒ D(f, p.length))

        for (d ← simpleDates) {
            val body = d.function.body
            val toks = withBefore(d.function.tokens, d.length)

            addNote(body, fromIncl = true, toIncl = true, toks, base)
        }

        // Full cached dates and ranges.
        for (f ← fullDates)
            addNote(f.body, fromIncl = true, toIncl = true, f.tokens, base)

        // Others - partially cached and without prepositions.
        val unknowns = partsDates.filter(!_.isProcessed)

        for (f ← unknowns)
            addNote(f.body, fromIncl = true, toIncl = true, f.tokens, base)

        collapse(ns)
    }
    
    /**
      * Parses time period format.
      * Currently supported only `hh:mm-now` format.
      * Note that `hh` and 'mm' can be any positive values (`00:90` means 90 minutes back.)
      *
      * @param body Body.
      */
    def mkTimePeriod(body: String): NCDateRange = {
        def mkError(): NCE = new NCE(s"Unexpected time period body: $body")

        def parse(s: String): Int =
            try {
                val i = java.lang.Integer.parseInt(s)

                if (i < 0)
                    throw mkError()

                i
            }
            catch {
                case _: NumberFormatException ⇒ throw mkError()
            }

        var seq = body.split("-").map(_.trim)

        if (seq.length != 2 || seq.last != "now")
            throw mkError()

        seq = seq.head.split(":").map(_.trim)

        if (seq.length != 2)
            throw mkError()

        val now = U.nowUtc()
    
        NCDateRange(
            Instant.from(now.minusHours(parse(seq.head)).minusMinutes(parse(seq.last))).toEpochMilli,
            Instant.from(now).toEpochMilli
        )
    }

    private def mkPrepositions(seq: Seq[String]): Seq[P] = seq.map(P).sortBy(-_.length)

    private def mkBetweenPrepositions(seq: Seq[(String, String)]): Seq[(P, P)] = seq.map(t ⇒ P(t._1) → P(t._2))

    private def areSuitableTokens(buf: mutable.Buffer[Set[NCNlpSentenceToken]], toks: Seq[NCNlpSentenceToken]): Boolean =
        toks.forall(t ⇒ !t.isQuoted && !t.isBracketed) && !buf.exists(_.exists(toks.contains))

    private def findDates(ns: NCNlpSentence): Seq[F] = {
        val buf = mutable.Buffer.empty[Set[NCNlpSentenceToken]]
        val res = mutable.Buffer.empty[F]

        for (toks ← ns.tokenMixWithStopWords()) {
            def process(toks: Seq[NCNlpSentenceToken]): Unit = {
                if (areSuitableTokens(buf, toks)) {
                    val s = toks.map(_.normText).mkString(" ")

                    def add(body: String, isFull: Boolean): Unit = {
                        res += F(toks, body, isFull)

                        buf += toks.toSet
                    }

                    cacheFull.get(s) match {
                        case Some(body) ⇒ add(body, isFull = true)
                        case None ⇒
                            cacheParts.get(s) match {
                                case Some(body) ⇒ add(body, isFull = false)
                                case None ⇒ // No-op.
                            }
                    }
                }
            }

            process(toks)

            val nnToks = toks.filter(!_.isStopword)

            if (nnToks != toks)
                process(nnToks)
        }

        res.sortBy(h ⇒ ns.indexOfSlice(h.tokens))
    }
    
    /**
      *
      * @param range
      * @param from
      * @param to
      * @param toks
      * @return
      */
    private def mkNote(range: NCDateRange, from: Int, to: Int, toks: Seq[NCNlpSentenceToken]): NCNlpSentenceNote =
        NCNlpSentenceNote(
            toks.map(_.index),
            "nlp:date",
            "from" → range.from,
            "to" → range.to,
            "periods" → range.periods,
            "length" → (to - from + 1)
        )

    private def addNote(
        body: String,
        fromIncl: Boolean,
        toIncl: Boolean,
        tokens: Seq[NCNlpSentenceToken],
        base: Long) {
        val note = mkNote(
            NCDateParser.calculate(body, base, fromIncl, toIncl).mkInclusiveDateRange,
            tokens.head.index,
            tokens.last.index,
            tokens
        )

        tokens.foreach(_.add(note))
    }

    private def mark(processed: F*): Unit = processed.foreach(_.isProcessed = true)

    private def collapse(ns: NCNlpSentence) {
        removeDuplicates(ns)
        collapsePeriods(ns)
        removeDuplicates(ns)
    }

    private def isValidRange(n: NCNlpSentenceNote): Boolean = n("from").asInstanceOf[Long] < n("to").asInstanceOf[Long]

    private def collapsePeriods(ns: NCNlpSentence) {
        // a) Months and years.
        // 1. "m", "m"... "y, m" → fix year for firsts; try to union all.
        // Example: January, February of 2009.

        // or "m", "m"... "m" → fix year for firsts; try to union all.
        // Example: January, February of previous year. (last month can be represented as Nm-x (x is 1 year))

        // 2. "m", "m"... "y" → fix year for firsts; try to union firsts - if success drop last.
        // Example: January, February, 2009 year.

        // 3. "y, m", "m"... "m" →  → fix year for lasts; try to union all.
        // Example: January of 2009 and February.

        // 4. "y", "m"... "m" → fix year for lasts; try to union lasts - if success drop first.
        // Example: 2009, January and February.

        // b) Days of week and week.
        // 1. "dw", "dw"... "w, dw" → fix week of year for firsts; try to union all.
        // Example: Monday, Tuesday of this week.

        // 2. "dw", "dw"... "w" → fix week of year for firsts; try to union firsts - if success drop last.
        // Example: Monday, Tuesday, this week.

        // 3. "w, dw", "dw"... "dw" → fix week of year for lasts; try to union all.
        // Example: Monday of this week and Tuesday.

        // 4. "w", "dw"... "dw" → fix week of year for lasts; try to union lasts - if success drop first.
        // Example: this week, Monday and Tuesday.
    
        // c) Days of week.
        
        // Try to union all.
        // Example: Monday, Tuesday.
        
        for (neighbours ← findNeighbours(ns, andSupport = true)) {
            val buf = mutable.Buffer.empty[Seq[NCNlpSentenceNote]]

            // Creates all neighbours' sequences starting from longest.
            val combs: Seq[Seq[NCNlpSentenceNote]] = (2 to neighbours.length).reverse.flatMap(i ⇒ neighbours.sliding(i))

            for (comb ← combs if !buf.exists(p ⇒ p.exists(p ⇒ comb.contains(p)))) {
                val first = comb.head
                val last = comb.last
                val firsts = comb.take(comb.size - 1)
                val lasts = comb.takeRight(comb.size - 1)

                def fixField(
                    field: Int,
                    seq: Seq[NCNlpSentenceNote],
                    base: NCNlpSentenceNote,
                    isBefore: Boolean = false,
                    isAfter: Boolean = false) = {
                    val r = mkDateRange(base)

                    val baseDate = if (!r.isFromNegativeInfinity) r.from else r.to

                    def setField(d: Long): Long = {
                        val c = mkCalendar(d)

                        c.set(field, getField(baseDate, field))

                        c.getTimeInMillis
                    }

                    // We cannot set field for both from and to because they can have various values of this field.
                    def convertRange(range: NCDateRange): NCDateRange = {
                        val from1 = range.from
                        val from2 = setField(from1)
    
                        NCDateRange(from2, range.to + from2 - from1)
                    }

                    seq.foreach(n ⇒ {
                        val r = convertRange(mkDateRange(n))

                        n += "from" → r.from
                        n += "to" → r.to
                        n += "periods" → new util.ArrayList[String]()
                    })

                    def optHolder(b: Boolean) = if (b) Some(base) else None

                    compressAndRemoveNotes(ns, seq, optHolder(isBefore), optHolder(isAfter))

                    buf += comb
                }

                // m m m,y
                if (equalHolders(firsts, "m") && equalHolder(last, "m", "y"))
                    fixField(C.YEAR, firsts :+ last, last)

                // m m y or y:y
                else if (equalHolders(firsts, "m") && (equalHolder(last, "y") || equalHolder(last, "y", ":", "y")))
                    fixField(C.YEAR, firsts, last, isAfter = true)

                // m,y m m
                else if (equalHolder(first, "m", "y") && equalHolders(lasts, "m"))
                    fixField(C.YEAR, Seq(first) ++ lasts, first)

                // y or y:y m m
                else if ((equalHolder(first, "y") || equalHolder(first, "y", ":", "y")) && equalHolders(lasts, "m"))
                    fixField(C.YEAR, lasts, first, isBefore = true)

                // dw dw dw,w
                else if (equalHolders(firsts, "dw") && equalHolder(last, "w", "dw"))
                    fixField(C.WEEK_OF_YEAR, firsts :+ last, last)

                // dw dw w or w:w
                else if (equalHolders(firsts, "dw") && (equalHolder(last, "w") || equalHolder(last, "w", ":", "w")))
                    fixField(C.WEEK_OF_YEAR, firsts, last, isAfter = true)

                // dw,w dw dw
                else if (equalHolder(first, "w", "dw") && equalHolders(lasts, "dw"))
                    fixField(C.WEEK_OF_YEAR, Seq(first) ++ lasts, first)

                // w or w:w dw dw
                else if ((equalHolder(first, "w") || equalHolder(first, "w", ":", "w")) && equalHolders(lasts, "dw"))
                    fixField(C.WEEK_OF_YEAR, lasts, first, isBefore = true)
            }
        }
    }

    private def compressNotes(
        ns: NCNlpSentence,
        notes: Seq[NCNlpSentenceNote],
        before: Option[NCNlpSentenceNote] = None,
        after: Option[NCNlpSentenceNote] = None): Boolean = {

        if (nearRanges(notes)) {
            def getSeq(optH: Option[NCNlpSentenceNote]): Seq[NCNlpSentenceNote] =
                optH match {
                    case Some(h) ⇒ Seq(h)
                    case None ⇒ Seq.empty
                }

            val s = getSeq(before) ++ notes ++ getSeq(after)

            val from = s.head.tokenFrom
            val to = s.last.tokenTo

            val note = mkNote(mkSumRange(notes), from, to, ns.filter(t ⇒ t.index >= from && t.index <= to))

            if (isValidRange(note)) {
                ns.
                    filter(t ⇒ t.index >= from && t.index <= to).
                    filter(!_.isStopword).
                    foreach(_.add(note)) // Replaces.
                true
            }
            else
                false
        }
        else
            false
    }

    private def compressAndRemoveNotes(
        ns: NCNlpSentence,
        seq: Seq[NCNlpSentenceNote],
        before: Option[NCNlpSentenceNote] = None,
        after: Option[NCNlpSentenceNote] = None) {
        if (!compressNotes(ns, seq, before, after)) {
            def remove(hOpt: Option[NCNlpSentenceNote]): Unit =
                hOpt match {
                    case Some(h) ⇒ ns.removeNote(h.id)
                    case None ⇒ // No-op.
                }

            remove(before)
            remove(after)
        }
    }

    private def findNeighbours(ns: NCNlpSentence, andSupport: Boolean): Seq[Seq[NCNlpSentenceNote]] = {
        val hs = ns.getNotes("nlp:date").sortBy(_.tokenFrom)

        case class Wrapper(holder: NCNlpSentenceNote, var group: Int)

        val wrappers = hs.map(Wrapper(_, 0))

        val grouped = wrappers.map(w ⇒ {
            val grp =
                if (w.holder == hs.head)
                    0
                else {
                    val prevW = getPrevious(w, wrappers)
                    val prevH = prevW.holder
                    val h = w.holder

                    val g = prevW.group

                    val toksBetween = ns.filter(t ⇒ t.index > prevH.tokenTo && t.index < h.tokenFrom)

                    if (toksBetween.isEmpty || toksBetween.forall(p ⇒ p.isStopword || (andSupport && p.origText == "and")))
                        g
                    else
                        g + 1
                }

            w.group = grp

            w
        }).map(w ⇒ w.holder → w.group).toMap

        hs.groupBy(grouped(_)).toSeq.sortBy(_._1).map(_._2).filter(_.size > 1)
    }

    private def removeDuplicates(ns: NCNlpSentence): Unit = {
        val ids = findNeighbours(ns, andSupport = false).flatMap(g ⇒ {
            case class H(from: Long, to: Long) {
                override def equals(obj: scala.Any): Boolean = obj match {
                    case v: H ⇒ v.from == from && v.to == to
                    case _ ⇒ false
                }

                override def hashCode(): Int = (from + to).hashCode()
            }

            // Neighbours grouped by equal date ranges.
            val grouped: Map[H, Seq[NCNlpSentenceNote]] = g.groupBy(h ⇒ H(h("from").asInstanceOf[Long], h("to").asInstanceOf[Long]))

            // Groups ordered to keep node with maximum information (max periods count in date).
            val hs: Iterable[Seq[NCNlpSentenceNote]] =
                grouped.map(_._2.sortBy(h ⇒ -h("periods").asInstanceOf[java.util.List[String]].asScala.length))

            // First holder will be kept in group, others (tail) should be deleted.
            hs.map(_.tail).flatMap(_.map(_.id))
        })

        ids.foreach(ns.removeNote)
    }

    private def mkCalendar(d: Long) = {
        val c = C.getInstance()

        c.setTimeInMillis(d)

        c
    }

    private def mkSumRange(notes: Seq[NCNlpSentenceNote]): NCDateRange =
        notes.size match {
            case 0 ⇒ throw new AssertionError("Unexpected empty notes")
            case 1 ⇒ mkDateRange(notes.head)
            case _ ⇒
                val ranges = notes.map(mkDateRange).sortBy(_.length)

                val maxRange = ranges.last

                if (ranges.take(ranges.size - 1).forall(maxRange.include))
                    mkDateRange(notes.head) // By the least.
                else
                    mkDateRange(notes.head, notes.last) // Summary.
        }

    private def mkDateRange(n1: NCNlpSentenceNote, n2: NCNlpSentenceNote): NCDateRange =
        NCDateRange(n1("from").asInstanceOf[Long], n2("to").asInstanceOf[Long])

    private def mkDateRange(n: NCNlpSentenceNote): NCDateRange = mkDateRange(n, n)
    private def getField(d: Long, field: Int): Int = mkCalendar(d).get(field)
    private def equalHolder(h: NCNlpSentenceNote, ps: String*): Boolean =
        h("periods").asInstanceOf[java.util.List[String]].asScala.sorted == ps.sorted
    private def equalHolders(hs: Seq[NCNlpSentenceNote], ps: String*): Boolean = hs.forall(equalHolder(_, ps: _*))
    private def getPrevious[T](s: T, seq: Seq[T]): T = seq(seq.indexOf(s) - 1)

    private def nearRanges(ns: Seq[NCNlpSentenceNote]): Boolean =
        ns.forall(
            n ⇒ if (n == ns.head) true else getPrevious(n, ns)("to").asInstanceOf[Long] == n("from").asInstanceOf[Long]
        )
}