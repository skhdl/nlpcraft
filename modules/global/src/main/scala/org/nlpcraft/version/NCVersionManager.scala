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

import java.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.typesafe.scalalogging.LazyLogging
import org.apache.http.HttpResponse
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.nlpcraft.NCE

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

/**
  * Version manager.
  */
object NCVersionManager extends LazyLogging {
    /**
      * Sends POST request to given URL.
      *
      * @param url
      * @param component
      * @param params
      */
    @throws[NCE]
    def askVersion(
        url: String,
        component: String,
        params: Map[String, Any]
    ): Future[Map[String, String]] = {
        val gson = new Gson()
        val typeResp = new TypeToken[util.HashMap[String, AnyRef]]() {}.getType

        implicit val ec: ExecutionContextExecutor = ExecutionContext.global

        Future {
            val client = HttpClients.createDefault

            val post = new HttpPost(url)

            try {
                post.setHeader("Content-Type", "application/json")
                post.setEntity(new StringEntity(gson.toJson(params)))

                logger.trace("Request prepared: {}", post)

                client.execute(
                    post,
                    new ResponseHandler[Map[String, String]] {
                        override def handleResponse(resp: HttpResponse): Map[String, String] = {
                            val code = resp.getStatusLine.getStatusCode

                            val e = resp.getEntity

                            if (e == null)
                                throw new NCE(s"Unexpected empty response code=$code")

                            val js = EntityUtils.toString(e)

                            if (code != 200)
                                throw new NCE(s"Unexpected response [code=$code, text=$js]")

                            try
                                gson.fromJson(js, typeResp)
                            catch {
                                case e: Exception ⇒ throw new NCE(s"Response cannot be parsed: $resp", e)
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
    }
}
