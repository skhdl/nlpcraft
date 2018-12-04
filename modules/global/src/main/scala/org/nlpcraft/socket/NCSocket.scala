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

        @throws[NCE]
        @throws[EOFException]
        def readLine(): String = {
            require(Thread.holdsLock(mux))

            val line = reader.readLine()

            if (line == null)
                throw new EOFException()

            line.trim
        }

        @throws[NCE]
        @throws[EOFException]
        def readHeaders(): Seq[String] = {
            require(Thread.holdsLock(mux))

            val lines = scala.collection.mutable.ArrayBuffer.empty[String]

            var line = readLine()

            // Between headers and content should be empty line.
            while (line.nonEmpty) {
                lines += line

                line = readLine()
            }

            lines
        }

        val arr =
            mux.synchronized {
                val firstLine = readLine()

                if (!firstLine.startsWith("POST")) {
                    // GETs processed special way to avoid stacktrace in errors log.
                    if (firstLine.startsWith("GET")) {
                        // Tries to answer.
                        if (firstLine.contains("robots.txt"))
                            try {
                                writer.write("User-agent: *\r\n")
                                writer.write("Allow: /")

                                writer.flush()

                                logger.trace("GET robots.txt request answered.")
                            }
                            catch {
                                case e: EOFException ⇒ throw e
                                case e: Exception ⇒ logger.trace("Error sending data.", e)
                            }

                        throw new EOFException()
                    }
                    else
                        throw new NCE(s"Unexpected line [line=$firstLine, expected-line-start='POST']")
                }

                val headers = readHeaders()

                val len =
                    headers.find(_.startsWith("Content-Length:")) match {
                        case Some(line) ⇒
                            try
                                Integer.parseInt(line.substring("Content-Length:".length).trim)
                            catch {
                                case e: NumberFormatException ⇒ throw new NCE(s"Unexpected `Content-Length`: $line", e)
                            }
                        case None ⇒ throw new NCE("`Content-Length` is not found")
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
            // HTTP proto.
            writer.write("POST /socket HTTP/1.1\r\n")

            if (host != null)
                writer.write(s"Host: $host\r\n")

            writer.write(s"Content-Length: ${data.length}\r\n")
            writer.write("Content-Type: application/x-www-form-urlencoded\r\n")
            writer.write("\r\n")

            writer.write(data)

            writer.flush()
        }
    }
}