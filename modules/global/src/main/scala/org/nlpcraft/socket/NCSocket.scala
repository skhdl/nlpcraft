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

package org.nlpcraft.socket

import java.io._
import java.net.Socket
import java.security.Key
import java.util.Base64

import org.nlpcraft._
import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.crypto.NCCipher

/**
  * Socket wrapper that does optional encryption and uses HTTP POST protocol for sending and receiving.
  */
case class NCSocket(socket: Socket, host: String, soTimeout: Int = 20000) extends LazyLogging {
    require(socket != null)
    require(host != null)
    require(soTimeout >= 0)

    socket.setSoTimeout(soTimeout)

    private final val mux = new Object()
    private lazy val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream, "UTF8"))
    private lazy val reader = new BufferedReader(new InputStreamReader(socket.getInputStream, "UTF8"))

    override def toString: String = socket.toString
    override def hashCode(): Int = socket.hashCode()
    override def equals(obj: scala.Any): Boolean =
        obj != null && obj.isInstanceOf[NCSocket] && socket.equals(obj.asInstanceOf[NCSocket].socket)

    /**
      *
      */
    def close(): Unit = {
        logger.trace(s"Closing socket: $socket")

        mux.synchronized {
            G.close(socket)
        }
    }
    
    /**
      *
      * @param key Optional encryption key.
      */
    @throws[NCE]
    @throws[IOException]
    def read[T](key: Key = null): T = {
        if (!socket.isConnected || socket.isInputShutdown)
            throw new EOFException()

        val arr =
            mux.synchronized {
                val line = reader.readLine()

                if (line == null)
                    throw new EOFException()

                val len =
                    try
                        Integer.parseInt(line.trim)
                    catch {
                        case e: NumberFormatException ⇒ throw new NCE(s"Unexpected content length: $line", e)
                    }

                if (len <= 0)
                    throw new NCE(s"Unexpected data length: $len")

                val arr = new Array[Char](len)

                var n = 0

                while (n != arr.length) {
                    val k = reader.read(arr, n, arr.length - n)

                    if (k == -1)
                        throw new EOFException()

                    n = n + k
                }

                arr
            }

        try {
            val bytes = if (key != null)
                    Base64.getDecoder.decode(NCCipher.decrypt(new String(arr), key))
                else
                    Base64.getDecoder.decode(new String(arr))

            val res: T = G.deserialize(bytes)

            res
        }
        catch {
            case e: Exception ⇒ throw new NCE("Error reading data.", e)
        }
    }
    
    /**
      *
      * @param v Value to send.
      * @param key Optional encryption key.
      */
    @throws[NCE]
    @throws[IOException]
    def write(v: Serializable, key: Key = null): Unit = {
        if (!socket.isConnected || socket.isOutputShutdown)
            throw new EOFException()

        val data =
            try {
                val serRes = G.serialize(v)
                val base64 = Base64.getEncoder.encodeToString(serRes)
                
                if (key == null) base64 else NCCipher.encrypt(base64, key)
            }
            catch {
                case e: Exception ⇒ throw new NCE("Error sending data.", e)
            }

        mux.synchronized {
            writer.write(s"${data.length}\r\n")
            writer.write(data)

            writer.flush()
        }
    }
}