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

package org.nlpcraft.cache

import org.apache.ignite.IgniteException
import org.apache.ignite.lang.IgniteBiInClosure
import org.nlpcraft.db.NCDbManager
import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft.ignite.NCIgniteCacheStore
import org.nlpcraft.mdo.NCDataSourceMdo

import scala.util.control.Exception._

/**
 * Datasource cache storage.
 */
class NcDsCacheStore extends NCIgniteCacheStore[Long, NCDataSourceMdo] {
    @throws[IgniteException]
    override protected def put(id: Long, ds: NCDataSourceMdo): Unit =
        catching(wrapNCE) {
            NCPsql.sql {
                if (ds.createdOn == null)
                    NCDbManager.addDataSource(
                        ds.id,
                        ds.name,
                        ds.shortDesc,
                        ds.modelId,
                        ds.modelName,
                        ds.modelVersion,
                        ds.modelConfig
                    )
                else
                    NCDbManager.updateDataSource(ds.id, ds.name, ds.shortDesc)
            }
        }

    @throws[IgniteException]
    override protected def get(id: Long): NCDataSourceMdo =
        catching(wrapNCE) {
            NCPsql.sql {
                NCDbManager.getDataSource(id)
            }.orNull
        }

    @throws[IgniteException]
    override protected def remove(id: Long): Unit =
        catching(wrapNCE) {
            NCPsql.sql {
                NCDbManager.deleteDataSource(id)
            }
        }

    @throws[IgniteException]
    override def loadCache(clo: IgniteBiInClosure[Long, NCDataSourceMdo], args: AnyRef*): Unit =
        catching(wrapNCE) {
            NCPsql.sql {
                val items =
                    args.size match {
                        case 0 ⇒ NCDbManager.getAllDataSources
                        case _ ⇒ args.map(_.asInstanceOf[Long]).flatMap(NCDbManager.getDataSource)
                    }

                items.foreach(item ⇒ clo.apply(item.id, item))
            }
        }
}