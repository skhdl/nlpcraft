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

package org.nlpcraft.server.cache

import org.apache.ignite.IgniteException
import org.apache.ignite.lang.IgniteBiInClosure
import org.nlpcraft.server.sql.{NCSqlManager, NCSql}
import org.nlpcraft.server.ignite.NCIgniteCacheStore
import org.nlpcraft.server.mdo.NCUserMdo

import scala.util.control.Exception._

/**
 * User cache storage.
 */
class NcUserCacheStore extends NCIgniteCacheStore[Either[Long, String], NCUserMdo] {
    @throws[IgniteException]
    override protected def put(key: Either[Long, String], usr: NCUserMdo): Unit =
        if (key.isLeft)
            catching(wrapNCE) {
                NCSql.sql {
                    val updated = NCSqlManager.updateUser(usr.id, usr.firstName, usr.lastName, usr.avatarUrl, usr.isAdmin)

                    if (updated == 0)
                        NCSqlManager.addUser(
                            usr.id,
                            usr.firstName,
                            usr.lastName,
                            usr.email,
                            usr.passwordSalt,
                            usr.avatarUrl,
                            usr.isAdmin
                        )
                }
            }

    @throws[IgniteException]
    override protected def get(key: Either[Long, String]): NCUserMdo =
        catching(wrapNCE) {
            NCSql.sql {
                if (key.isLeft)
                    NCSqlManager.getUser(key.left.get)
                else
                    NCSqlManager.getUserByEmail(key.right.get)
            }.orNull
        }

    @throws[IgniteException]
    override protected def remove(key: Either[Long, String]): Unit =
        if (key.isLeft)
            catching(wrapNCE) {
                NCSql.sql {
                    NCSqlManager.deleteUser(key.left.get)
                }
            }

    @throws[IgniteException]
    override def loadCache(clo: IgniteBiInClosure[Either[Long, String], NCUserMdo], args: AnyRef*): Unit =
        catching(wrapNCE) {
            NCSql.sql {
                val items =
                    args.size match {
                        case 0 ⇒ NCSqlManager.getAllUsers
                        case _ ⇒ args.map(_.asInstanceOf[Long]).flatMap(NCSqlManager.getUser)
                    }

                items.foreach(item ⇒ clo.apply(Left(item.id), item))
            }
        }
}