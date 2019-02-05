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
 * Software:    NlpCraft
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

package org.nlpcraft.version

import java.io.IOException
import java.net.InetAddress
import java.util
import java.util.TimeZone

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.http.HttpResponse
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.nlpcraft.{NCE, _}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

/**
  * Version check manager.
  */
object NCVersionManager extends NCLifecycle("Version manager") {
    // TODO:
    private final val URL = "http://localhost:8099/version"
    
    // Whether or not version check is disabled.
    private final val enabled = !G.isSysEnvTrue("NLPCRAFT_VERSION_CHECK_DISABLED")

    /**
      * Check for version update and prints it.
      *
      * @param name Component name.
      * @param params Additional component related parameters.
      */
    @throws[NCE]
    def checkForUpdates(name: String, params: Map[String, Any]): Unit = {
        ensureStarted()
        
        if (!enabled)
            return
        
        val tmz = TimeZone.getDefault
        val sysProps = System.getProperties
    
        // Collect basic environment data.
        var hostName = ""
        var hostAddr = ""
        
        try {
            val localhost = InetAddress.getLocalHost

            hostName = localhost.getHostName
            hostAddr = localhost.getHostAddress
        }
        catch {
            case e: IOException ⇒ logger.warn(s"Error during network info: ${e.getMessage}")
        }
        
        val ver = NCVersion.getCurrent
        
        val m = Map(
            "API_DATE" → ver.date,
            "API_VERSION" → ver.version,
            "OS_VER" → sysProps.getProperty("os.version"),
            "OS_NAME" → sysProps.getProperty("os.name"),
            "OS_ARCH" → sysProps.getProperty("os.arch"),
            "START_TSTAMP" → G.nowUtcMs(),
            "TMZ_ID" → tmz.getID,
            "TMZ_ABBR" → tmz.getDisplayName(false, TimeZone.SHORT),
            "TMZ_NAME" → tmz.getDisplayName(),
            "SYS_USERNAME" → sysProps.getProperty("user.name"),
            "JAVA_VER" → sysProps.getProperty("java.version"),
            "JAVA_VENDOR" → sysProps.getProperty("java.vendor"),
            "HOST_NAME" → hostName,
            "HOST_ADDR" → hostAddr
        )

        val gson = new Gson()
        val typeResp = new TypeToken[util.HashMap[String, AnyRef]]() {}.getType

        implicit val ec: ExecutionContextExecutor = ExecutionContext.global

        val props = (m ++ params).map(p ⇒ p._1 → (if (p._2 != null) p._2.toString else null)).asJava

        val f =
            Future {
                val client = HttpClients.createDefault

                val post = new HttpPost(URL)

                try {
                    post.setHeader("Content-Type", "application/json")

                    post.setEntity(
                        new StringEntity(
                            gson.toJson(
                                Map(
                                    "name" → name,
                                    "version" → ver.version,
                                    "properties" → props
                                ).asJava
                            )
                        )
                    )

                    client.execute(
                        post,
                        new ResponseHandler[String] {
                            override def handleResponse(resp: HttpResponse): String = {
                                val code = resp.getStatusLine.getStatusCode
                                
                                if (code != 200)
                                    throw new NCE(s"Unexpected response code: $code")
                                
                                val e = resp.getEntity
                                
                                if (e == null)
                                    throw new NCE(s"Unexpected empty response.")
                                
                                val js = EntityUtils.toString(e)
                                
                                val m: util.Map[String, AnyRef] =
                                    try
                                        gson.fromJson(js, typeResp)
                                    catch {
                                        case e: Exception ⇒ throw new NCE(s"Response cannot be parsed: $js", e)
                                    }
                                
                                m.get("status") match {
                                    case null ⇒ throw new NCE(s"Missed status field: $js")
                                    
                                    case status ⇒
                                        if (status != "OK")
                                            throw new NCE(s"Unexpected response status: $status")
                                        
                                        m.get("data") match {
                                            case null ⇒ throw new NCE(s"Missed data field: $js")
                                            case data ⇒ data.asInstanceOf[String]
                                        }
                                }
                            }
                        }
                    )
                }
                finally {
                    post.releaseConnection()

                    client.close()
                }
            }

        f.onSuccess { case s ⇒ logger.info(s) }
    }
}
