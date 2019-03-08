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

package org.nlpcraft.server.ds

import org.apache.ignite.IgniteAtomicSequence
import org.nlpcraft.common.version.NCVersion
import org.nlpcraft.common.{NCLifecycle, _}
import org.nlpcraft.server.ignite.NCIgniteInstance
import org.nlpcraft.server.mdo._
import org.nlpcraft.server.notification.NCNotificationManager
import org.nlpcraft.server.sql.{NCSql, NCSqlManager}

import scala.util.control.Exception.catching

/**
  * Data sources manager.
  */
object NCDsManager extends NCLifecycle("Data source manager") with NCIgniteInstance {
    @volatile private var dsSeq: IgniteAtomicSequence = _

    /**
      * Starts this manager.
      */
    override def start(): NCLifecycle = {
        ensureStopped()

        catching(wrapIE) {
            dsSeq = NCSql.mkSeq(ignite, "dsSeq", "ds_instance", "id")
        }

        super.start()
    }

    /**
      * Stops this manager.
      */
    override def stop(): Unit = {
        super.stop()
    }

    /**
      * Gets data source for given user ID.
      *
      * @param dsId User ID.
      */
    @throws[NCE]
    def getDataSource(dsId: Long): Option[NCDataSourceMdo] = {
        ensureStarted()

        NCSql.sql {
            NCSqlManager.getDataSource(dsId)
        }
    }

    /**
      * Updates given data source.
      *
      * @param dsId ID of the data source to update.
      * @param name Data source name.
      * @param shortDesc Data source description.
      */
    @throws[NCE]
    def updateDataSource(
        dsId: Long,
        name: String,
        shortDesc: String
    ): Unit = {
        ensureStarted()

        val ds =
            NCSql.sql {
                val ds = NCSqlManager.getDataSource(dsId).getOrElse(throw new NCE(s"Unknown data source ID: $dsId"))

                NCSqlManager.updateDataSource(
                    ds.id,
                    name,
                    shortDesc
                )

            ds
        }

        // Notification.
        NCNotificationManager.addEvent("NC_DS_UPDATE",
            "dsId" → dsId,
            "name" → ds.name,
            "desc" → ds.shortDesc
        )
    }
    
    /**
      * Adds new temporary data source.
      *
      * @param mdlId Model ID.
      */
    def addTempDataSource(mdlId: String): Long = {
        ensureStarted()

        val newDsId = dsSeq.incrementAndGet()

        NCSql.sql {
            NCSqlManager.addDataSource(
                newDsId,
                s"tmp",
                "auto-delete-temp-ds",
                mdlId,
                s"tmp",
                s"${NCVersion.getCurrent.version}",
                None
            )
        }

        // NOTE: no notification for temp data source.

        newDsId
    }

    /**
      * Adds new data source.
      *
      * @param name Data source name.
      * @param desc Data source description.
      * @param mdlId Model ID.
      * @param mdlName Model name.
      * @param mdlVer Model version.
      * @param mdlCfg Model configuration.
      * @return
      */
    @throws[NCE]
    def addDataSource(
        name: String,
        desc: String,
        mdlId: String,
        mdlName: String,
        mdlVer: String,
        mdlCfg: Option[String]
    ): Long = {
        ensureStarted()

        val newDsId = dsSeq.incrementAndGet()

        NCSql.sql {
            NCSqlManager.addDataSource(
                newDsId,
                name,
                desc,
                mdlId,
                mdlName,
                mdlVer,
                mdlCfg
            )
        }

        // Notification.
        NCNotificationManager.addEvent("NC_DS_ADD",
            "dsId" → newDsId,
            "name" → name,
            "desc" → desc,
            "modelId" → mdlId,
            "modelName" → mdlName,
            "mdlVer" → mdlVer,
            "mdlCfg" → mdlCfg
        )

        newDsId
    }

    /**
      * Deletes data source with given ID.
      *
      * @param dsId ID of the data source to delete.
      */
    @throws[NCE]
    def deleteDataSource(dsId: Long): Unit = {
        ensureStarted()

        val ds =
            NCSql.sql {
                val ds = NCSqlManager.getDataSource(dsId).getOrElse(throw new NCE(s"Unknown data source ID: $dsId"))

                NCSqlManager.deleteDataSource(dsId)

                ds
            }

        // Notification.
        NCNotificationManager.addEvent("NC_DS_DELETE",
            "dsId" → dsId,
            "name" → ds.name,
            "desc" → ds.shortDesc,
            "modelId" → ds.modelId,
            "modelName" → ds.modelName,
            "mdlVer" → ds.modelVersion,
            "mdlCfg" → ds.modelConfig
        )
    }

    /**
      * Gets the list of all data sources.
      */
    @throws[NCE]
    def getAllDataSources: Seq[NCDataSourceMdo] = {
        ensureStarted()

        NCSql.sql {
            NCSqlManager.getAllDataSources
        }
    }
}
