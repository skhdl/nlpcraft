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

package org.nlpcraft.model.tools.dump

import java.io.{BufferedInputStream, FileInputStream, IOException, ObjectInputStream}
import java.util
import java.util.zip.GZIPInputStream

import com.typesafe.scalalogging.LazyLogging
import org.nlpcraft.common._
import org.nlpcraft.common.version.NCVersion
import org.nlpcraft.model.{NCModel, NCModelDescriptor, NCModelProvider}
import resource.managed

import scala.collection.JavaConverters._

/**
  * Dump reader.
  */
object NCDumpReader extends LazyLogging {
    @throws[NCE]
    def read(path: String): NCModelProvider = {
        val (version, mdl) =
            try {
                managed(
                    if (path.toLowerCase.endsWith(".gz"))
                        new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(path))))
                    else
                        new ObjectInputStream(new BufferedInputStream(new FileInputStream(path)))
                ) acquireAndGet { in ⇒
                    (
                        in.readObject().asInstanceOf[NCVersion.Version],
                        in.readObject().asInstanceOf[NCModel]
                    )
                }
            }
            catch {
                case e: IOException ⇒ throw new NCE(s"Error reading file: $path", e)
            }

        if (version != NCVersion.getCurrent)
            logger.warn(s"Unexpected version [current=${NCVersion.getCurrent}, read=$version]")

        new NCModelProvider() {
            override def makeModel(id: String): NCModel = {
                if (mdl.getDescriptor.getId != id)
                    throw new UnsupportedOperationException(s"Model cannot be created: $id")

                mdl
            }

            override def getDescriptors: util.List[NCModelDescriptor] = Seq(mdl.getDescriptor).asJava
        }
    }
}