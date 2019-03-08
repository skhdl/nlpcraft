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

import java.text.SimpleDateFormat
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
case class NCConversation(usrId: Long, dsId: Long) extends LazyLogging {
    // After 10 mins pause between questions we clear the STM.
    private final val CONV_CLEAR_DELAY = 10.minutes.toMillis
    
    // Timestamp format.
    private final val TSTAMP_FMT = new SimpleDateFormat("hh:mm:ss a")
    
    // Short-Term-Memory.
    private val stm = mutable.TreeSet.empty[NCConversationItem]
    private var ctx = new java.util.HashSet[NCToken]()
    private var lastUpdateTstamp = U.nowUtcMs()
    
    /**
      *
      * @param tokens
      * @param text
      * @param srvReqId
      * @param tstamp
      */
    case class NCConversationItem(
        tokens: java.util.List[NCToken],
        text: String,
        srvReqId: String,
        tstamp: Long
    ) extends Ordered[NCConversationItem] {
        override def compare(that: NCConversationItem): Int = this.tstamp.compareTo(that.tstamp)
    }
    
    /**
      *
      */
    def update(): Unit = stm.synchronized {
        val now = U.nowUtcMs()
    
        if (now - lastUpdateTstamp > CONV_CLEAR_DELAY) {
            logger.trace(s"Conversation reset by timeout [" +
                s"usrId=$usrId, " +
                s"dsId=$dsId" +
            s"]")
        
            stm.clear()
        }
    
        lastUpdateTstamp = now
    
        val map = mutable.HashMap.empty[String/*Token group.*/, mutable.Buffer[NCToken]]
    
        // Recalculate the context based on new STM.
        for (item ← stm)
            // NOTE:
            // (1) STM is a red-black tree and traversed in ascending time order (older first).
            // (2) Map update ensure that only the youngest tokens per each group are retained in the context.
            map ++= item.tokens.asScala.filter(t ⇒ !isFreeWord(t) && !isStopWord(t)).groupBy(
                tok ⇒ if (tok.getGroup == null) "" else tok.getGroup
            )
    
        ctx = new java.util.HashSet[NCToken](map.values.flatten.asJavaCollection)
    }
    
    /**
      * Clears all tokens from this conversation satisfying given predicate.
      *
      * @param p Java-side predicate.
      */
    def clear(p: Predicate[NCToken]): Unit = stm.synchronized {
        for (item ← stm)
            item.tokens.removeIf(p)
    
        logger.trace(s"Manually cleared conversation for some tokens.")
    }
    
    /**
      *
      * @param p Scala-side predicate.
      */
    def clear(p: (NCToken) ⇒ Boolean): Unit =
        clear(new Predicate[NCToken] {
            override def test(t: NCToken): Boolean = p(t)
        })
    
    /**
      * Adds new item to the conversation.
      *
      * @param sen Sentence.
      * @param v Sentence's specific variant.
      */
    def addItem(sen: NCSentence, v: NCVariant): Unit = stm.synchronized {
        stm += NCConversationItem(
            v.getTokens,
            sen.getNormalizedText,
            sen.getServerRequestId,
            lastUpdateTstamp
        )
    
        logger.trace(s"Added new sentence to the conversation [" +
            s"usrId=$usrId, " +
            s"dsId=$dsId, " +
            s"text=${sen.getNormalizedText}" +
        s"]")
    }
    
    /**
      * Prints out ASCII table for current STM.
      */
    def ack(): Unit = stm.synchronized {
        val stmTbl = NCAsciiTable("Time", "Sentence", "Server Request ID")
        
        stm.foreach(item ⇒ stmTbl += (
            TSTAMP_FMT.format(new java.util.Date(item.tstamp)),
            item.text,
            item.srvReqId
        ))
    
        val ctxTbl = NCAsciiTable("Token ID", "Group", "Text", "Value", "From request")
    
        ctx.asScala.foreach(tok ⇒ ctxTbl += (
            tok.getId,
            tok.getGroup,
            getNormalizedText(tok),
            tok.getValue,
            tok.getServerRequestId
        ))
    
        logger.trace(s"Conversation history [usrId=$usrId, dsId=$dsId]:\n${stmTbl.toString}")
        logger.trace(s"Conversation tokens [usrId=$usrId, dsId=$dsId]:\n${ctxTbl.toString}")
    }
    
    /**
      * 
      * @return
      */
    def tokens: java.util.HashSet[NCToken] = stm.synchronized {
        ctx
    }
}