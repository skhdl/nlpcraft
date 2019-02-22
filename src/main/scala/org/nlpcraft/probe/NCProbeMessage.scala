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

package org.nlpcraft.probe

import java.io._

import org.nlpcraft.common._
import org.nlpcraft.common.ascii._

import scala.collection.mutable

/**
  * Probe-server protocol message. Every message has at least these values: TYPE, GUID, TSTAMP.
  *
  * @param typ Type (name) of the message.
  */
class NCProbeMessage(val typ: String) extends mutable.HashMap[String/*Name*/, Serializable/*Value*/]
    with Serializable with NCAsciiLike {
    private val guid = U.genGuid()
    private val hash = guid.hashCode()
    
    put("TYPE", typ)
    put("GUID", guid)
    
    override def equals(obj: Any): Boolean = obj match {
        case msg: NCProbeMessage ⇒ msg.guid == guid
        case _ ⇒ false
    }
    
    override def hashCode(): Int = hash
    
    // Shortcuts.
    def getType: String = typ
    def getGuid: Long = data[Long]("GUID") // Message GUID.
    def getProbeToken: String = data[String]("PROBE_TOKEN")
    def getProbeId: String = data[String]("PROBE_ID")
    def getProbeGuid: String = data[String]("PROBE_GUID") // Probe GUID.
    
    def setProbeToken(tkn: String): NCProbeMessage = {
        put("PROBE_TOKEN", tkn)
        
        this
    }
    def setProbeId(id: String): NCProbeMessage = {
        put("PROBE_ID", id)
        
        this
    }
    def setProbeGuid(guid: String): NCProbeMessage = {
        put("PROBE_GUID", guid)
        
        this
    }
    
    /**
      *
      * @param key Map key.
      * @tparam T Return value type.
      * @return Map value (including `null` values)
      */
    def data[T](key: String): T =
        dataOpt[T](key) match {
            case None ⇒ throw new AssertionError(s"Probe message missing key [key=$key, data=$this]")
            case Some(x) ⇒ x.asInstanceOf[T]
        }
    
    /**
      *
      * @param key Map key.
      * @tparam T Return value type.
      * @return `None` or `Some` map value (including `null` values).
      */
    def dataOpt[T](key: String): Option[T] =
        get(key) match {
            case None ⇒ None
            case Some(x) ⇒ x match {
                case None | null ⇒ None
                case z ⇒ Some(z.asInstanceOf[T])
            }
        }
    
    override def toAscii: String =
        iterator.toSeq.sortBy(_._1).foldLeft(NCAsciiTable("Key", "Value"))((t, p) ⇒ t += p).toString
    
    override def toString(): String =
        iterator.toSeq.sortWith((t1, t2) ⇒ {
            if (t1._1 == "TYPE")
                true
            else if (t2._1 == "TYPE")
                false
            else
                t1._1.compare(t2._1) <= 0
        }).map(t ⇒ s"${t._1} -> ${t._2}").mkString("{", ", ", "}")
}

object NCProbeMessage {
    /**
      *
      * @param typ Message type.
      * @param pairs Parameters.
      */
    def apply(typ: String, pairs: (String, Serializable)*): NCProbeMessage = {
        val impl = new NCProbeMessage(typ)
    
        for ((k, v) ← pairs)
            impl.put(k, v)
        
        impl
    }
}