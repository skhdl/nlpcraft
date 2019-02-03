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

package org.nlpcraft.mdo

import java.sql.Timestamp

import org.nlpcraft.db.postgres.NCPsql.Implicits.RsParser
import org.nlpcraft.mdo.impl._

/**
  * User MDO.
  */
@NCMdoEntity(table = "nc_user")
case class NCUserMdo(
    @NCMdoField(column = "id", pk = true) id: Long,
    @NCMdoField(column = "email") email: String,
    @NCMdoField(column = "first_name") firstName: String,
    @NCMdoField(column = "last_name") lastName: String,
    @NCMdoField(column = "avatar_url") avatarUrl: Option[String],
    @NCMdoField(column = "passwd_salt") passwordSalt: String,
    @NCMdoField(column = "last_ds_id") lastDsId: Long,
    @NCMdoField(column = "is_admin") isAdmin: Boolean,

    // Base MDO.
    @NCMdoField(column = "created_on") createdOn: Timestamp,
    @NCMdoField(column = "last_modified_on") lastModifiedOn: Timestamp
) extends NCEntityMdo with NCAnnotatedMdo[NCUserMdo]

object NCUserMdo {
    implicit val x: RsParser[NCUserMdo] =
        NCAnnotatedMdo.mkRsParser(classOf[NCUserMdo])

    def apply(
        id: Long,
        email: String,
        firstName: String,
        lastName: String,
        avatarUrl: Option[String],
        passwordSalt: String,
        isAdmin: Boolean
    ): NCUserMdo = {
        require(email != null, "Email cannot be null.")
        require(firstName != null, "First name cannot be null.")
        require(lastName != null, "Last name cannot be null.")
        require(passwordSalt != null, "Password salt cannot be null.")

        NCUserMdo(id, email, firstName, lastName, avatarUrl, passwordSalt, -1, isAdmin, null, null)
    }

    def apply(
        id: Long,
        email: String,
        firstName: String,
        lastName: String,
        avatarUrl: Option[String],
        passwordSalt: String,
        isAdmin: Boolean,
        createdOn: Timestamp
    ): NCUserMdo = {
        require(email != null, "Email cannot be null.")
        require(firstName != null, "First name cannot be null.")
        require(lastName != null, "Last name cannot be null.")
        require(passwordSalt != null, "Password salt cannot be null.")
        require(createdOn != null, "Created date cannot be null.")

        NCUserMdo(id, email, firstName, lastName, avatarUrl, passwordSalt, -1, isAdmin, createdOn, null)
    }
}
