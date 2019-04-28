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

package org.nlpcraft.probe.mgrs.nlp.conversation

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}
import java.util
import java.util.function.Predicate

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.common._
import org.nlpcraft.common.ascii.NCAsciiTable
import org.nlpcraft.model._
import org.nlpcraft.model.utils.NCTokenUtils._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration._

/**
  * Conversation as an ordered set of utterances.
  */
case class NCConversation(usrId: Long, mdlId: String) extends LazyLogging {
    // After 5 mins pause between questions we clear the STM.
    private final val CONV_CLEAR_DELAY = 5.minutes.toMillis

    // If token is not used in last 3 requests, it is removed from conversation context.
    private final val MAX_DEPTH = 3

    // Timestamp format.
    private final val TSTAMP_FMT = DateTimeFormatter.ofPattern("hh:mm:ss a")

    private final val UTC = ZoneId.of("UTC")

    // Short-Term-Memory.
    private val stm = mutable.ArrayBuffer.empty[ConversationItem]
    private val lastUsedToks = mutable.ArrayBuffer.empty[Iterable[NCToken]]

    private var ctx = new util.ArrayList[NCToken]()
    private var lastUpdateTstamp = U.nowUtcMs()
    private var attempt = 0

    /**
      *
      * @param token
      * @param tokenTypeUsageTime
      */
    case class TokenHolder(token: NCToken, var tokenTypeUsageTime: Long = 0)

    /**
      *
      * @param holders Tokens holders.
      * @param text Sentence text. Used just for logging.
      * @param srvReqId Server request ID. Used just for logging.
      * @param tstamp Request timestamp. Used just for logging.
      */
    case class ConversationItem(holders: mutable.ArrayBuffer[TokenHolder], text: String, srvReqId: String, tstamp: Long)

    /**
      *
      */
    private def squeeze(): Unit = {
        require(Thread.holdsLock(stm))

        stm --= stm.filter(_.holders.isEmpty)
    }

    /**
      *
      */
    def update(): Unit = {
        val now = U.nowUtcMs()

        stm.synchronized {
            attempt += 1

            // Conversation cleared by timeout or when there are too much unsuccessful requests.
            if (now - lastUpdateTstamp > CONV_CLEAR_DELAY || attempt > MAX_DEPTH) {
                stm.clear()

                logger.trace(s"Conversation reset [usrId=$usrId, mdlId=$mdlId]")
            }
            else {
                val minUsageTime = now - CONV_CLEAR_DELAY
                val lastToks = lastUsedToks.flatten

                for (item ← stm) {
                    val delHs =
                        // Deleted by timeout for tokens type or when token type used too many requests ago.
                        item.holders.filter(h ⇒ h.tokenTypeUsageTime < minUsageTime || !lastToks.contains(h.token))

                    if (delHs.nonEmpty) {
                        item.holders --= delHs

                        logger.trace(
                            s"Conversation tokens deleted [" +
                                s"usrId=$usrId, " +
                                s"mdlId=$mdlId, " +
                                s"srvReqId=${item.srvReqId}, " +
                                s"sentence=${item.text}, " +
                                s"tokens=${delHs.map(_.token).mkString(", ")}" +
                            s"]"
                        )
                    }
                }

                squeeze()
            }

            lastUpdateTstamp = now

            ctx = new util.ArrayList[NCToken](stm.flatMap(_.holders.map(_.token)).asJava)
        }
    }

    /**
      * Clears all tokens from this conversation satisfying given predicate.
      *
      * @param p Java-side predicate.
      */
    def clear(p: Predicate[NCToken]): Unit = {
        stm.synchronized {
            for (item ← stm)
                item.holders --= item.holders.filter(h ⇒ p.test(h.token))

            squeeze()
        }

        logger.trace(s"Manually cleared conversation for some tokens.")
    }

    /**
      *
      * @param p Scala-side predicate.
      */
    def clear(p: NCToken ⇒ Boolean): Unit =
        clear(new Predicate[NCToken] {
            override def test(t: NCToken): Boolean = p(t)
        })

    /**
      * Adds new item to the conversation.
      *
      * @param srvReqId Server request ID.
      * @param txt Text.
      * @param usedToks Used tokens, including conversation tokens.
      */
    def addItem(srvReqId: String, txt: String, usedToks: Seq[NCToken]): Unit = {
        stm.synchronized {
            attempt = 0

            // Last used tokens processing.
            lastUsedToks += usedToks

            val delCnt = lastUsedToks.length - MAX_DEPTH

            if (delCnt > 0)
                lastUsedToks.remove(0, delCnt)

            val senToks = usedToks.filter(_.getServerRequestId == srvReqId).filter(t ⇒ !isFreeWord(t) && !isStopWord(t))

            if (senToks.nonEmpty) {
                // Adds new conversation element.
                stm += ConversationItem(
                    mutable.ArrayBuffer.empty[TokenHolder] ++ senToks.map(TokenHolder(_)),
                    txt,
                    srvReqId,
                    lastUpdateTstamp
                )

                val groups = mutable.HashSet.empty[String]

                for (
                    item ← stm.reverse;
                    (group, hs) ← item.holders.groupBy(t ⇒ if (t.token.getGroup != null) t.token.getGroup else "")
                ) {
                    // Deletes if tokens with this group already exists. First added tokens from last conversation items.
                    if (!groups.add(group))
                        item.holders --= hs

                    // Updates tokens usage time.
                    item.holders.filter(h ⇒ usedToks.contains(h.token)).foreach(_.tokenTypeUsageTime = lastUpdateTstamp)
                }

                squeeze()
            }
        }

        logger.trace(
            s"Added new sentence to the conversation [" +
                s"usrId=$usrId, " +
                s"mdlId=$mdlId, " +
                s"text=$txt, " +
                s"usedTokens=${usedToks.mkString(", ")}" +
            s"]"
        )
    }

    /**
      * Prints out ASCII table for current STM.
      */
    def ack(): Unit = {
        def mkHistoryTable(): String = {
            val t = NCAsciiTable("Time", "Sentence", "Server Request ID")

            stm.synchronized {
                stm.foreach(item ⇒
                    t += (
                        TSTAMP_FMT.format(Instant.ofEpochMilli(item.tstamp).atZone(UTC)),
                        item.text,
                        item.srvReqId
                    )
                )
            }

            t.toString
        }

        def mkTokensTable(): String = {
            val t = NCAsciiTable("Token ID", "Group", "Text", "Value", "From request")

            stm.synchronized {
                ctx.asScala.foreach(tok ⇒ t += (
                    tok.getId,
                    tok.getGroup,
                    getNormalizedText(tok),
                    tok.getValue,
                    tok.getServerRequestId
                ))
            }

            t.toString
        }

        logger.trace(s"Conversation history [usrId=$usrId, mdlId=$mdlId]:\n${mkHistoryTable()}")
        logger.trace(s"Conversation tokens [usrId=$usrId, mdlId=$mdlId]:\n${mkTokensTable()}")
    }

    /**
      *
      *
      * @return
      */
    def tokens: util.List[NCToken] = stm.synchronized {
        val srvReqIds = ctx.asScala.map(_.getServerRequestId).distinct.zipWithIndex.toMap
        val toks = ctx.asScala.groupBy(_.getServerRequestId).toSeq.sortBy(p ⇒ srvReqIds(p._1)).reverse.flatMap(_._2)

        new util.ArrayList[NCToken](toks.asJava)
    }
}