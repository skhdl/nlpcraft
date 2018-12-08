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

package org.nlpcraft.quote

import java.util.concurrent.ConcurrentHashMap
import java.util.{Calendar => C}

import org.nlpcraft.json.NCJson
import org.nlpcraft.quote.NCUsageLimitType.{NCUsageLimitType, _}
import org.nlpcraft.{NCE, NCLifecycle}

/**
 * Usage limit manager.
 */
object NCUsageLimitManager extends NCLifecycle("SERVER usage limit manager") {
    /**
     * Limit holder.
     *
     * @param second Maximum actions per second.
     * @param minute Maximum actions per minute.
     * @param hour Maximum actions per hour.
     * @param day Maximum actions per day.
     * @param month Maximum actions per month.
     * @param year Maximum actions per year.
     */
    private case class JsonLimit(
        second: Option[Int],
        minute: Option[Int],
        hour: Option[Int],
        day: Option[Int],
        month: Option[Int],
        year: Option[Int]
    ) {
        private def get(opt: Option[Int]): Int = opt.getOrElse(Int.MaxValue)

        def getSecond: Int = get(second)
        def getMinute: Int = get(minute)
        def getHour: Int = get(hour)
        def getDay: Int = get(day)
        def getMonth: Int = get(month)
        def getYear: Int = get(year)
    }

    @volatile private var limits: Map[String, Map[NCUsageLimitType, Int]] = _

    private val usersQuotas = new ConcurrentHashMap[Long, ConcurrentHashMap[String, Seq[Quota]]]()

    /**
     * Starts this component.
     */
    override def start(): NCLifecycle = {
        ensureStopped()
        
        val map = NCJson.extractResource[Map[String, JsonLimit]]("usage/usage.json", ignoreCase = false)

        map.values.foreach(limit ⇒ {
            require(limit.getSecond <= limit.getMinute)
            require(limit.getMinute <= limit.getHour)
            require(limit.getHour <= limit.getDay)
            require(limit.getDay <= limit.getMonth)
            require(limit.getMonth <= limit.getYear)
        })

        limits = map.map { case (action, limit) ⇒
            action →
            Map(
                SECOND → limit.getSecond,
                MINUTE → limit.getMinute,
                HOUR → limit.getHour,
                DAY → limit.getDay,
                MONTH → limit.getMonth,
                YEAR → limit.getYear
            )
        }
        
        super.start()
    }

    /**
     * User quotas state holder.
     *
     * @param quotaType Quota period type.
     * @param quotaLimit User action limit for this period type.
     * @param key Quota period key.
     * @param count Processed actions count.
     */
    case class Quota(quotaType: NCUsageLimitType, quotaLimit: Int, private var key: Long, private var count: Int) {
        /**
         * Registers action and check quotes limits for given key.
         *
         * @param key Quota period key.
         */
        def onAction(key: Long): Option[NCUsageLimitType] =
            this.synchronized {
                if (this.key == key) {
                    count = count + 1

                    if (count > quotaLimit) Some(quotaType) else None
                }
                else {
                    this.key = key

                    count = 1

                    None
                }
            }
    }
    
    /**
      * Throws `NCUsageLimitException` exception in case of usage limit violation.
      * 
      * @param usrId User ID.
      * @param act Action.
      */
    @throws[NCE]
    def onActionEx(usrId: Long, act: String): Unit =
        onAction(usrId, act) match {
            case Some(typ) ⇒ throw NCUsageLimitException(typ)
            case None ⇒
        }

    /**
     * Gets exceeded limit type for given user action or `None` if no usage violations were found.
     *
     * @param usrId User ID.
     * @param act Action.
     */
    def onAction(usrId: Long, act: String): Option[NCUsageLimitType] = {
        ensureStarted()
        
        if (IS_DEBUG)
            None
        else {
            if (!limits.contains(act))
                throw new AssertionError(s"Unknown usage limit action [" +
                    s"userId=$usrId, " +
                    s"act=$act" +
                s"]")
    
            val c = C.getInstance()
    
            /**
              * Makes key for quote type.
              */
            def mkKey(f: Int, v: Int): Long = {
                c.set(f, v)
    
                c.getTimeInMillis
            }
    
            // Keys should be generated one by one in the given order to avoid duplicated fields setting.
            val keys = Map(
                SECOND → mkKey(C.MILLISECOND, 0),
                MINUTE → mkKey(C.SECOND, 0),
                HOUR → mkKey(C.MINUTE, 0),
                DAY → mkKey(C.HOUR_OF_DAY, 0),
                MONTH → mkKey(C.DAY_OF_MONTH, 1),
                YEAR → mkKey(C.DAY_OF_YEAR, 1)
            )
    
            def mkInitial(): Seq[Quota] = NCUsageLimitType.values.map(t ⇒ Quota(t, limits(act)(t), keys(t), 1)).toSeq
    
            usersQuotas.get(usrId) match {
                // First user action during server's life cycle.
                case null ⇒
                    val m = new ConcurrentHashMap[String, Seq[Quota]]()
    
                    m.put(act, mkInitial())
    
                    usersQuotas.put(usrId, m)
    
                    None

                case m ⇒
                    m.get(act) match {
                        case null ⇒
                            m.put(act, mkInitial())
    
                            None
                        case quotas ⇒
                            quotas.flatMap(q ⇒ q.onAction(keys(q.quotaType))).toStream.headOption
                    }
            }
        }
    }
}