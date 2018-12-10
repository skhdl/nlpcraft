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

package org.nlpcraft2.db

import java.sql.Timestamp
import java.util.Date

import org.nlpcraft2.apicodes.NCApiStatusCode._
import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft.db.postgres.NCPsql.Implicits._
import org.nlpcraft.ignite.NCIgniteNlpCraft
import org.nlpcraft2.mdo.{NCSynonymCacheKeyMdo, _}
import org.nlpcraft.{NCE, NCLifecycle, _}

/**
 * Provides basic CRUD and often used operations on PostgreSQL RDBMS.
 * Note that all functions in this class expect outside 'NCPsql.sql()' block.
 */
object NCDbManager extends NCLifecycle("SERVER DB manager") with NCIgniteNlpCraft {
    /**
     * Starts manager.
     */
    @throws[NCE]
    override def start(): NCLifecycle = {
        ensureStopped()

        // Warm up SQL connection pool.
        NCPsql.sql {
            if (!NCPsql.isValid)
                throw new NCE("No valid JDBC connection found.")
        }

        super.start()
    }

    /**
     * Stop manager.
     */
    override def stop(): Unit = {
        checkStopping()

        super.stop()
    }

    /**
     * Changes admin flag of specified user.
     *
     * @param usrId User ID.
     * @param flag New admin flag for user.
     */
    private def changeUserAdmin(usrId: Long, flag: Boolean): Unit = {
        ensureStarted()

        NCPsql.update(
            s"""
                |UPDATE company_user
                |  SET
                |    is_admin = $flag,
                |    last_modified_on = current_timestamp
                |  WHERE
                |    id = ? AND
                |    deleted = FALSE
                """.stripMargin,
            usrId
        )
    }

    /**
      * Updates timezone and IP-based GEO location for given user ID.
      *
      * @param usrId User ID to update.
      * @param tmzName User's last access timezone name (TZ in https://en.wikipedia.org/wiki/List_of_tz_database_time_zones).
      * @param tmzAbbr User's last access timezone abbreviation: https://www.timeanddate.com/time/zones/
      * @param latitude User's last access latitude.
      * @param longitude User's last access longitude.
      * @param cntryName Country name.
      * @param cntryCode Country ISO code.
      * @param regName Region name.
      * @param regCode Region ISO code.
      * @param zipCode ZIP or postal code.
      * @param metroCode Metro ISO code.
      * @param city City name.
      */
    @throws[NCE]
    def updateTmzGeo(
        usrId: Long,
        tmzName: String,
        tmzAbbr: String,
        latitude: Double,
        longitude: Double,
        cntryName: String,
        cntryCode: String,
        regName: String,
        regCode: String,
        zipCode: String,
        metroCode: Int,
        city: String): Unit = {
        ensureStarted()

        NCPsql.update(
            s"""
               |UPDATE company_user
               |  SET
               |    tmz_name = ?,
               |    tmz_abbr = ?,
               |    latitude = ?,
               |    longitude = ?,
               |    country_name = ?,
               |    country_code = ?,
               |    region_name = ?,
               |    region_code = ?,
               |    zip_code = ?,
               |    metro_code = ?,
               |    city = ?,
               |    last_modified_on = current_timestamp
               |  WHERE
               |    id = ? AND
               |    deleted = FALSE
            """.stripMargin,
            tmzName,
            tmzAbbr,
            latitude,
            longitude,
            cntryName,
            cntryCode,
            regName,
            regCode,
            zipCode,
            metroCode,
            city,
            usrId
        )
    }
    
    /**
      * Updates timezone for given user ID .
      *
      * @param usrId User ID to update.
      * @param tmzName User's last access timezone name (TZ in https://en.wikipedia.org/wiki/List_of_tz_database_time_zones).
      * @param tmzAbbr User's last access timezone abbreviation: https://www.timeanddate.com/time/zones/
      */
    @throws[NCE]
    def updateTmz(
        usrId: Long,
        tmzName: String,
        tmzAbbr: String): Unit = {
        ensureStarted()
        
        NCPsql.update(
            s"""
               |UPDATE company_user
               |  SET
               |    tmz_name = ?,
               |    tmz_abbr = ?,
               |    last_modified_on = current_timestamp
               |  WHERE
               |    id = ? AND
               |    deleted = FALSE
            """.stripMargin,
            tmzName,
            tmzAbbr,
            usrId
        )
    }
    
    /**
      * Updates user.
      *
      * @param usrId ID of the user to update.
      * @param avatarUrl Avatar URL.
      * @param firstName First name.
      * @param lastName Last name.
      * @param title Title.
      * @param department Department.
      * @param phone Phone.
      */
    @throws[NCE]
    def updateUser(
        usrId: Long,
        avatarUrl: String,
        firstName: String,
        lastName: String,
        title: String,
        department: String,
        phone: String
    ): Unit = {
        ensureStarted()
        
        NCPsql.update(
            s"""
               |UPDATE company_user
               |  SET
               |    first_name = ?,
               |    last_name = ?,
               |    title = ?,
               |    department = ?,
               |    phone = ?,
               |    avatar_url = ?,
               |    last_modified_on = current_timestamp
               |  WHERE
               |    id = ? AND
               |    deleted = FALSE
                """.stripMargin,
            firstName,
            lastName,
            title,
            department,
            phone,
            avatarUrl,
            usrId
        )
    }

    /**
      * Updates user.
      *
      * @param usrId ID of the user to update.
      * @param avatarUrl Avatar URL.
      * @param firstName First name.
      * @param lastName Last name.
      * @param title Title.
      * @param department Department.
      * @param phone Phone.
      * @param prefs Preferences.
      */
    @throws[NCE]
    def updateUser(
        usrId: Long,
        avatarUrl: String,
        firstName: String,
        lastName: String,
        title: String,
        department: String,
        phone: String,
        prefs: String
    ): Unit = {
        ensureStarted()

        NCPsql.update(
            s"""
               |UPDATE company_user
               |  SET
               |    first_name = ?,
               |    last_name = ?,
               |    title = ?,
               |    department = ?,
               |    phone = ?,
               |    avatar_url = ?,
               |    last_modified_on = current_timestamp,
               |    prefs_json = ?
               |  WHERE
               |    id = ? AND
               |    deleted = FALSE
                """.stripMargin,
            firstName,
            lastName,
            title,
            department,
            phone,
            avatarUrl,
            prefs,
            usrId
        )
    }

    
    /**
      * Gets user avatar URL for given email.
      *
      * @param email User email.
      */
    @throws[NCE]
    def getUserAvatar(email: String): Option[String] = {
        ensureStarted()
        
        NCPsql.selectSingle[String]("SELECT avatar_url FROM company_user WHERE email = ? AND deleted = FALSE", email)
    }

    /**
      * Tests whether or not given sign up domain exists.
      *
      * @param domain Sign up domain to test.
      */
    @throws[NCE]
    def isKnownSignUpDomain(domain: String): Boolean = {
        ensureStarted()

        NCPsql.exists(
            """
              |company
              |WHERE
              |     sign_up_domain = ? AND
              |     deleted = FALSE""".stripMargin,
            domain)
    }

    /**
      * Updates user's preferences.
      *
      * @param usrId User ID.
      * @param prefs User preferences as JSON string.
      */
    @throws[NCE]
    def updateUserPreferences(usrId: Long, prefs: String): Unit = {
        ensureStarted()

        val cnt = NCPsql.update(
            s"""
               |UPDATE company_user
               |  SET
               |    prefs_json = ?,
               |    last_modified_on = current_timestamp
               |  WHERE
               |    id = ? AND
               |    deleted = FALSE
                """.stripMargin,
            prefs,
            usrId
        )

        if (cnt != 1)
            throw new NCE(s"Update count != 1 for updating preferences for user ID: $usrId")
    }
    
    /**
     * Changes active flag of specified user.
     *
     * @param get User select function.
     * @param active Active flag for user.
     * @return 'true' if user was successfully deactivated.
     */
    @throws[NCE]
    private def activate0(get: () ⇒ Option[NCUserMdo], active: Boolean): Boolean = {
        ensureStarted()

        get() match {
            case None ⇒ false
            case Some(user) ⇒ NCPsql.update(
                """
                  |UPDATE company_user
                  |  SET
                  |     is_active = ?,
                  |     last_modified_on = current_timestamp
                  |  WHERE
                  |     id = ? AND
                  |     deleted = FALSE""".stripMargin,
                active,
                user.id) == 1
        }
    }

    /**
     * Changes active flag of specified user.
     *
     * @param id User ID.
     * @param active Active flag for user.
     * @return 'true' if user was successfully deactivated.
     */
    @throws[NCE]
    private def activate0(id: Long, active: Boolean): Boolean = activate0(() ⇒ getUser(id), active)

    /**
     * Changes active flag of specified user.
     *
     * @param email User email.
     * @param active Active flag for user.
     * @return 'true' if user was successfully deactivated.
     */
    @throws[NCE]
    private def activate0(email: String, active: Boolean): Boolean = activate0(() ⇒ getUserByEmail(email), active)

    /**
      * Adds new company with given name.
      *
      * @param origin Company's origin, e.g. 'dev', 'web', etc.
      * @param name Company name.
      * @param domain Company signup domain.
      * @param website Optional website.
      * @param address Option address.
      * @param city Option city.
      * @param region Option region.
      * @param postalCode Optional postal code.
      * @param country Optional country.
      * @param probeTkn Probe token. If empty or not provided - a random one will be generated.
      * @return Newly added company ID.
     */
    @throws[NCE]
    def addCompany(
        origin: String,
        name: String = "",
        domain: String,
        website: String = "",
        address: String = "",
        city: String = "",
        region: String = "",
        postalCode: String = "",
        country: String = "",
        probeTkn: String = ""
        ): Long = {
        ensureStarted()
    
        // Use domain if name is not provided.
        val nameX = if (name.isEmpty) domain else name
        
        val tkn = if (probeTkn != "") probeTkn else G.genGuid()
    
        NCPsql.insertGetKey[Long](
            """
              | INSERT INTO company (
              |     name,
              |     origin,
              |     sign_up_domain,
              |     website,
              |     address,
              |     city,
              |     region,
              |     postal_code,
              |     country,
              |     probe_token,
              |     probe_token_hash
              | )
              | VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.stripMargin,
            nameX,
            origin,
            domain,
            website,
            address,
            city,
            region,
            postalCode,
            country,
            tkn,
            G.makeSha256Hash(tkn)
        )
    }

    /**
      * Gets invite with given invite token.
      *
      * @param tkn Invite token.
      */
    @throws[NCE]
    def getInviteForToken(tkn: String): Option[NCInviteMdo] = {
        ensureStarted()

        NCPsql.selectSingle[NCInviteMdo](
            """
              |SELECT
              |    i.id,
              |    i.first_name,
              |    i.last_name,
              |    i.email,
              |    i.token,
              |    i.requested_on,
              |    i.invited_on,
              |    i.signed_up_on,
              |    i.user_id,
              |    u.avatar_url,
              |    i.created_on,
              |    i.last_modified_on
              |  FROM invite i
              |  LEFT OUTER JOIN company_user u
              |  ON (i.user_id = u.id)
              |  WHERE
              |     i.token = ? AND
              |     i.deleted = FALSE
            """.stripMargin, tkn)
    }

    /**
      * Marks all active invites with given email as signed up for provided user ID.
      *
      * @param email Email of the user.
      * @param usrId ID of the signed up user.
      */
    @throws[NCE]
    def markInvitesAsSignedUp(email: String, usrId: Long): Unit = {
        ensureStarted()

        NCPsql.update(
            """
              |UPDATE invite
              |  SET
              |    user_id = ?,
              |    signed_up_on = current_timestamp,
              |    last_modified_on = current_timestamp
              |  WHERE
              |    email = ? AND
              |    deleted = FALSE AND
              |    signed_up_on IS NULL""".stripMargin,
            usrId,
            G.normalizeEmail(email)
        )
    }

    /**
      *
      * @param invId Invite ID.
      * @param tkn Invite activation token.
      * @return
      */
    @throws[NCE]
    def activateInvite(invId: Long, tkn: String): Unit = {
        ensureStarted()

        NCPsql.update(
            """
              |UPDATE invite
              |  SET
              |    token = ?,
              |    invited_on = current_timestamp,
              |    last_modified_on = current_timestamp
              |  WHERE
              |    id = ? AND
              |    deleted = FALSE""".stripMargin,
            tkn,
            invId
        )
    }

    /**
     * Adds new invite for given prospect.
     *
     * @param firstName Prospect's first name.
     * @param lastName Prospect's last name.
     * @param email Prospect's email.
     */
    @throws[NCE]
    def addInvite(firstName: String, lastName: String, email: String): Long = {
        ensureStarted()

        NCPsql.insertGetKey(
            """
              |INSERT INTO invite (
              |  first_name,
              |  last_name,
              |  email
              |)
              |VALUES (?, ?, ?)""".stripMargin,
            firstName,
            lastName,
            G.normalizeEmail(email)
        )
    }

    /**
      * Adds login history record.
      *
      * @param usrId User ID.
      * @param userEmail User email.
      * @param act Specific login or logout action.
      * @param userAgent User agent descriptor.
      * @param rmtAddr Remote address.
      */
    @throws[NCE]
    def addLoginHistory(
        usrId: Long,
        userEmail: String,
        act: String,
        userAgent: String,
        rmtAddr: String): Unit = {
        require(userEmail != null)
        require(userAgent != null)
        require(act != null)

        ensureStarted()

        NCPsql.insertSingle(
            """
              |INSERT INTO login_history (
              |  user_id,
              |  user_email,
              |  act,
              |  user_agent,
              |  rmt_addr)
              |VALUES (?, ?, ?, ?, ?)""".stripMargin,
            usrId,
            G.normalizeEmail(userEmail),
            act.toUpperCase,
            userAgent,
            rmtAddr
        )
    }

    /**
     * Adds input history record.
     *
     * @param usrId User Id.
     * @param srvReqId Server request ID.
     * @param origTxt Original text.
     * @param dsId Data source ID.
     * @param userAgent User agent descriptor.
     * @param origin Origin.
     * @param rmtAddr Remote address.
     * @param test Test flag.
     */
    @throws[NCE]
    def addHistory(
        usrId: Long,
        srvReqId: String,
        origTxt: String,
        dsId: Long,
        userAgent: String,
        origin: String,
        rmtAddr: String,
        status: NCApiStatusCode,
        test: Boolean
    ): Unit = {
        ensureStarted()

        NCPsql.insertSingle(
            """
              |INSERT
              |  INTO history (
              |     user_id,
              |     srv_req_id,
              |     orig_txt,
              |     ds_id,
              |     user_agent,
              |     origin,
              |     rmt_addr,
              |     status,
              |     is_test
              | )
              | VALUES (
              |     ?, ?, ?, ?, ?, ?, ?, ?, ?
              | )""".stripMargin,
            usrId,
            srvReqId,
            origTxt,
            dsId,
            userAgent,
            origin,
            rmtAddr,
            status.toString,
            test
        )
        
        // Copy optional GEO information from the user.
        NCPsql.update(
            """
              |UPDATE history h
              | SET
              |    tmz_name = u.tmz_name,
              |    tmz_abbr = u.tmz_abbr,
              |    latitude = u.latitude,
              |    longitude = u.longitude,
              |    city = u.city,
              |    country_name = u.country_name,
              |    country_code = u.country_code,
              |    region_name = u.region_name,
              |    region_code = u.region_code,
              |    zip_code = u.zip_code,
              |    metro_code = u.metro_code
              | FROM
              |     company_user u
              | WHERE
              |     u.id = ? AND
              |     h.user_id = u.id
            """.stripMargin,
            usrId
        )
    }

    /**
      * Gets a list of user-data source relationships for given company.
      *
      * @param compId Company ID.
      */
    @throws[NCE]
    def getCompanyUserDataSource(compId: Long): List[NCUserDataSourceMdo] = {
        ensureStarted()
        
        NCPsql.select[NCUserDataSourceMdo](
            """
              |SELECT
              |     ud.user_id,
              |     ud.ds_id
              |  FROM
              |    user_ds ud,
              |    company c,
              |    company_user u
              |  WHERE
              |    u.company_id = c.id AND
              |    c.id = ? AND
              |    c.deleted = FALSE AND
              |    u.deleted = FALSE AND
              |    ud.user_id = u.id""".stripMargin,
            compId
        )
    }
    
    /**
      * Links provided data source and user.
      *
      * @param usrId User ID.
      * @param dsId Data source ID.
      */
    @throws[NCE]
    def linkUserDataSource(usrId: Long, dsId: Long): Unit = {
        ensureStarted()
        
        NCPsql.insertSingle(
            """
              |INSERT INTO user_ds (
              |  ds_id,
              |  user_id
              |)
              |VALUES (?, ?)""".stripMargin,
            dsId,
            usrId
        )
    }
    
    /**
      * Unlinks provided user and data source.
      *
      * @param usrId User ID.
      * @param dsId Data source ID.
      */
    @throws[NCE]
    def unlinkUserDataSource(usrId: Long, dsId: Long): Unit = {
        ensureStarted()
        
        NCPsql.delete(
            """
              |DELETE FROM user_ds
              |  WHERE
              |    user_id = ? AND
              |    ds_id = ?""".stripMargin,
            usrId,
            dsId
        )
    }

    /**
      * Finds ID of the data source matching given vendor ID, type ID for specified company.
      *
      * @param vendorId Vendor specific unique data source ID.
      * @param typeId Data source type ID.
      * @param compId Company ID.
      */
    @throws[NCE]
    def findMatchingDataSourceId(vendorId: String, typeId: String, compId: Long): Option[Long] = {
        ensureStarted()

        NCPsql.selectSingle[Long](
            """
              |SELECT
              |    d.id
              |  FROM
              |    ds_instance d,
              |    company c,
              |    company_user u
              |  WHERE
              |    d.vendor_id = ? AND
              |    d.type_id = ? AND
              |    c.id = ? AND
              |    u.id = d.user_id AND
              |    u.company_id = c.id AND
              |    u.deleted = FALSE AND
              |    c.deleted = FALSE AND
              |    d.deleted = FALSE""".stripMargin,
            vendorId,
            typeId,
            compId
        )
    }

    /**
     * Finds IDs of the data source for specified company.
     *
     * @param companyId Company ID.
     */
    @throws[NCE]
    def getDataSourceIds(companyId: Long): Seq[Long] = {
        ensureStarted()

        NCPsql.select[Long](
            """
              |SELECT d.id
              |  FROM
              |    ds_instance d,
              |    company c,
              |    company_user u
              |  WHERE
              |    c.id = ? AND
              |    u.company_id = c.id AND
              |    d.user_id = u.id AND
              |    d.deleted = FALSE AND
              |    u.deleted = FALSE AND
              |    c.deleted = FALSE""".stripMargin,
            companyId
        )
    }
    
    /**
      * Checks that given data source name is unique in the company.
      *
      * @param compId Company ID.
      * @param name Data source name to check.
      */
    @throws[NCE]
    def isDataSourceNameUnique(compId: Long, name: String): Boolean = {
        ensureStarted()
        
        !NCPsql.exists(
            """
              | SELECT 1
              | FROM
              |    ds_instance
              | WHERE
              |    deleted = FALSE AND
              |    user_id in (
              |        SELECT id
              |        FROM company_user
              |        WHERE
              |            company_id = ? AND
              |            DELETED = FALSE
              |    ) AND
              |    name = ?
              | LIMIT 1
              | """.stripMargin,
            compId,
            name
        )
    }
    
    /**
      * Tests if given probe token is known and valid in the system.
      *
      * @param probeTkn Probe token to check.
      */
    @throws[NCE]
    def isProbeTokenKnown(probeTkn: String): Boolean = {
        ensureStarted()
    
        NCPsql.exists(
            """
              | SELECT 1
              | FROM
              |     company
              | WHERE
              |     probe_token = ? AND
              |     deleted = FALSE
              | """.stripMargin,
            probeTkn
        )
    }
    
    /**
      * Tests that given probe token and user email are known, and belong to the same company.
      *
      * @param probeTkn Probe token to check.
      * @param email User email to check.
      */
    @throws[NCE]
    def isProbeTokenAndEmailValid(probeTkn: String, email: String): Boolean = {
        ensureStarted()
    
        NCPsql.exists(
            """
              | SELECT 1
              | FROM
              |     company c,
              |     company_user u
              | WHERE
              |     probe_token = ? AND
              |     u.email = ? AND
              |     u.company_id = c.id AND
              |     u.is_active = TRUE AND
              |     (
              |         u.is_admin = TRUE OR
              |         u.is_root = TRUE
              |     ) AND
              |     c.deleted = FALSE AND
              |     u.deleted = FALSE
              | """.stripMargin,
            probeTkn,
            email
        )
    }
    
    /**
      * Adds new data source instance.
      *
      * @param name Name.
      * @param desc Description.
      * @param usrId User ID.
      * @param enabled Enabled flag.
      * @param mdlId Model ID.
      * @param mdlName Model name.
      * @param mdlVer Model version.
      * @param mdlCfg Model config.
      */
    @throws[NCE]
    def addDataSource(
        name: String,
        desc: String,
        usrId: Long,
        enabled: Boolean,
        mdlId: String,
        mdlName: String,
        mdlVer: String,
        mdlCfg: String
    ): Long = {
        ensureStarted()

        NCPsql.insertGetKey[Long](
            """
              |INSERT INTO ds_instance
              |(
              |     name,
              |     short_desc,
              |     user_id,
              |     enabled,
              |     model_id,
              |     model_name,
              |     model_ver,
              |     model_cfg
              |) VALUES (?, ?, ?, ?, ?, ?, ?, ?)""".stripMargin,
            name,
            desc,
            usrId,
            enabled,
            mdlId,
            mdlName,
            mdlVer,
            mdlCfg
        )
    }

    /**
     * Gets all probe releases.
     *
     * @return List of all probe releases.
     */
    @throws[NCE]
    def getAllProbeReleases: List[NCProbeReleaseMdo] = {
        ensureStarted()
    
        NCPsql.select[NCProbeReleaseMdo](
            """
              | SELECT *
              | FROM probe_release
            """.stripMargin)
    }
    
    /**
     * Gets company for given ID.
     *
     * @param compId Company ID.
     * @return Company MDO.
     */
    @throws[NCE]
    def getCompany(compId: Long): Option[NCCompanyMdo] = {
        ensureStarted()
    
        NCPsql.selectSingle[NCCompanyMdo](
            """
              | SELECT *
              | FROM company
              | WHERE
              |     id = ? AND
              |     deleted = FALSE
            """.stripMargin,
            compId)
    }
    
    /**
      * Checks probe token and user email for public REST API authentication.
      *
      * @param probeTkn Probe token.
      * @param email User email.
      * @return
      */
    @throws[NCE]
    def checkProbeTokenAndAdminEmail(probeTkn: String, email: String): Boolean = {
        ensureStarted()
        
        NCPsql.exists(
            """
              | SELECT 1
              | FROM
              |     company c,
              |     company_user u
              | WHERE
              |     c.probe_token = ? AND
              |     u.email = ? AND
              |     u.company_id = c.id AND
              |     c.deleted = FALSE AND
              |     u.deleted = FALSE AND
              |     u.is_active = TRUE AND
              |     u.is_admin = TRUE
            """.stripMargin,
            probeTkn,
            email)
    }
    
    /**
      * Updates company probe token.
      * 
      * @param compId Company ID.
      * @param tkn New probe token.
      */
    @throws[NCE]
    def updateCompanyToken(compId: Long, tkn: String): Boolean = {
        ensureStarted()
        
        NCPsql.update(
            """
              | UPDATE company
              | SET
              |     probe_token = ?,
              |     probe_token_hash = ?,
              |     last_modified_on = current_timestamp
              | WHERE
              |     id = ? AND
              |     deleted = FALSE
            """.stripMargin,
            tkn,
            G.makeSha256Hash(tkn),
            compId
        ) == 1
    }

    /**
     * Gets a list of data source instance IDs (enabled or disabled) for the given user ID.
     *
     * @param usrId User ID.
     * @return List of data source instance IDs for the given user ID.
     */
    @throws[NCE]
    def getDataSourceIdsForUser(usrId: Long): List[Long] = {
        ensureStarted()

        NCPsql.select[Long](
            """
              | SELECT DISTINCT ds.id
              | FROM
              |     company_user cu,
              |     user_ds ud,
              |     ds_instance ds
              | WHERE
              |     ud.user_id = ? AND
              |     ud.ds_id = ds.id AND
              |     cu.id = ud.user_id AND
              |     cu.deleted = FALSE AND
              |     ds.deleted = FALSE
            """.stripMargin,
            usrId
        )
    }
    
    /**
      * Gets a list of data source instances for the given company ID. Note that this method
      * ignores enabled/disable and role-based accessibility: it returns all data sources regardless.
      *
      * @param compId Company ID.
      * @return List of data source instances for the given company ID.
      */
    @throws[NCE]
    def getDataSourcesForCompany(compId: Long): List[NCDataSourceInstanceMdo] = {
        ensureStarted()
        
        NCPsql.select[NCDataSourceInstanceMdo](
            """
              | SELECT ds.*
              | FROM
              |    company_user u,
              |    company c,
              |    ds_instance ds
              | WHERE
              |    c.id = ? AND
              |    u.company_id = c.id AND
              |    ds.user_id = u.id AND
              |    u.deleted = FALSE AND
              |    ds.deleted = FALSE AND
              |    c.deleted = FALSE
            """.stripMargin,
            compId
        )
    }
    
    /**
      * Gets a list of data source instance IDs for the given company ID. Note that this method
      * ignores enabled/disable and accessibility: it returns all data sources regardless.
      *
      * @param compId Company ID.
      * @return List of data source instance IDs for the given company ID.
      */
    @throws[NCE]
    def getDataSourceIdsForCompany(compId: Long): List[Long] = {
        ensureStarted()
        
        NCPsql.select[Long](
            """
              | SELECT DISTINCT ds.id
              | FROM
              |    company_user u,
              |    company c,
              |    ds_instance ds
              | WHERE
              |    c.id = ? AND
              |    u.company_id = c.id AND
              |    ds.user_id = u.id AND
              |    u.deleted = FALSE AND
              |    ds.deleted = FALSE AND
              |    c.deleted = FALSE
            """.stripMargin,
            compId
        )
    }

    /**
      * Sets active data source for given user.
      *
      * @param dsId Data source ID to set as active.
      * @param usrIds User IDs to set active data source ID for.
      */
    @throws[NCE]
    def setActiveDataSourceId(dsId: Long, usrIds: Long*): Unit = {
        ensureStarted()

        for (usrId ← usrIds) {
            NCPsql.update(
                """
                    | UPDATE company_user
                    | SET
                    |     active_ds_id = ?,
                    |     last_modified_on = current_timestamp
                    | WHERE
                    |     id = ? AND
                    |     deleted = FALSE
                """.stripMargin,
                dsId,
                usrId
            )
         }
    }
    
    /**
      * Updates company.
      *
      * @param compId Company ID.
      * @param name Company name.
      * @param website Website
      * @param addr Address
      * @param city City
      * @param region Region
      * @param postalCode Postal code.
      * @param country Country.
      */
    @throws[NCE]
    def updateCompany(
        compId: Long,
        name: String,
        website: String,
        addr: String,
        city: String,
        region: String,
        postalCode: String,
        country: String
    ): Unit = {
        ensureStarted()
        
        NCPsql.update(
            """
              | UPDATE company
              | SET
              |     name = ?,
              |     website = ?,
              |     address = ?,
              |     city = ?,
              |     region = ?,
              |     postal_code = ?,
              |     country = ?,
              |     last_modified_on = current_timestamp
              | WHERE
              |     id = ? AND
              |     deleted = FALSE
            """.stripMargin,
            name,
            website,
            addr,
            city,
            region,
            postalCode,
            country,
            compId
        )
    }
    
    /**
      * Tests whether given data source is live, enabled and accessible by the given user.
      *
      * @param dsId ID of the data source.
      * @param usrId ID of the user.
      */
    @throws[NCE]
    def isDataSourceAccessible(dsId: Long, usrId: Long): Boolean = {
        ensureStarted()
    
        NCPsql.exists(
            """
              | SELECT 1
              | FROM
              |     ds_instance ds,
              |     user_ds ud
              | WHERE
              |     ds.id = ? AND
              |     ud.ds_id = ds.id AND
              |     ud.user_id = ? AND
              |     ds.deleted = FALSE AND
              |     ds.enabled = TRUE
            """.stripMargin,
            dsId,
            usrId
        )
    }

    /**
      * Tests whether given data source is exists and accessible by the given user.
      *
      * @param dsId ID of the data source.
      * @param usrId ID of the user.
      */
    @throws[NCE]
    def isDataSourceExists(dsId: Long, usrId: Long): Boolean = {
        ensureStarted()

        NCPsql.exists(
            """
              | SELECT 1
              | FROM
              |     ds_instance ds,
              |     user_ds ud
              | WHERE
              |     ds.id = ? AND
              |     ud.ds_id = ds.id AND
              |     ud.user_id = ? AND
              |     ds.deleted = FALSE
            """.stripMargin,
            dsId,
            usrId
        )
    }

    /**
     * Gets user for given ID.
     *
     * @param usrId User ID.
     * @return User MDO.
     */
    @throws[NCE]
    def getUser(usrId: Long): Option[NCUserMdo] = {
        ensureStarted()
    
        NCPsql.selectSingle[NCUserMdo](
            s"""
               |SELECT
               |  *
               |FROM
               |  company_user
               |WHERE
               |  id = ? AND
               |  deleted = FALSE
            """.stripMargin,
            usrId)
    }
    
    /**
      * Checks if given hash exists in the passwrod pool.
      *
      * @param hash Hash to check.
      */
    @throws[NCE]
    def isKnownPasswordHash(hash: String): Boolean = {
        ensureStarted()
        
        NCPsql.exists("passwd_pool WHERE passwd_hash = ?", hash)
    }
    
    /**
      * Inserts password hash into anonymous password pool.
      * 
      * @param hash Password hash to insert into anonymous password pool.
      */
    @throws[NCE]
    def addPasswordHash(hash: String): Unit = {
        ensureStarted()
        
        NCPsql.insert("INSERT INTO passwd_pool (passwd_hash) VALUES (?)", hash)
    }
    
    /**
      * Removes password hash from anonymous password pool.
      *
      * @param hash Password hash to remove.
      */
    @throws[NCE]
    def removePasswordHash(hash: String): Unit = {
        ensureStarted()
        
        NCPsql.delete("DELETE FROM passwd_pool WHERE passwd_hash = ?", hash)
    }
    
    /**
      * 
      * @param usrId ID of the user to fetch last `num` history records for.
      * @param num Number of last records to fetch.
      * @return
      */
    def getLastHistoryItems(usrId: Long, num: Int): List[NCHistoryItemMdo] = {
        ensureStarted()
        
        NCPsql.select[NCHistoryItemMdo](
            """
              | SELECT *
              | FROM history
              | WHERE
              |     user_id = ?
              | ORDER BY
              |     recv_tstamp DESC
              | LIMIT ?;
            """.stripMargin,
            usrId,
            num)
    }

    /**
     * Gets data source for given ID.
     *
     * @param dsId Data source ID.
     * @return Data source MDO.
     */
    @throws[NCE]
    def getDataSource(dsId: Long): Option[NCDataSourceInstanceMdo] = {
        ensureStarted()
    
        NCPsql.selectSingle[NCDataSourceInstanceMdo](
            """
              | SELECT *
              | FROM ds_instance
              | WHERE
              |     id = ? AND
              |     deleted = FALSE
            """.stripMargin,
            dsId)
    }

    /**
     * Gets company for given sign up domain.
     *
     * @param domain Company sign up domain.
     * @return Company MDO.
     */
    @throws[NCE]
    def getCompanyByDomain(domain: String): Option[NCCompanyMdo] = {
        ensureStarted()

        NCPsql.selectSingle[NCCompanyMdo](
            """
              |SELECT *
              |FROM company
              |WHERE
              |     lower(sign_up_domain) = ? AND
              |     deleted = FALSE""".stripMargin,
            domain.toLowerCase
        )
    }

    /**
      * Gets company for given hash token.
      *
      * @param hash Company hash token.
      * @return Company MDO.
      */
    @throws[NCE]
    def getCompanyByHashToken(hash: String): Option[NCCompanyMdo] = {
        ensureStarted()

        NCPsql.selectSingle[NCCompanyMdo](
            """
              |SELECT *
              |FROM company
              |WHERE
              |     probe_token_hash = ? AND
              |     deleted = FALSE""".stripMargin,
            hash
        )
    }
    
    /**
      * Gets company for given user ID.
      *
      * @param usrId ID of the user to get company for.
      * @return Company MDO.
      */
    @throws[NCE]
    def getCompanyByUserId(usrId: Long): Option[NCCompanyMdo] = {
        ensureStarted()
        
        NCPsql.selectSingle[NCCompanyMdo](
            """
              |SELECT c.*
              |FROM
              |  company c,
              |  company_user u
              |WHERE
              |     u.id = ? AND
              |     u.company_id = c.id AND
              |     c.deleted = FALSE""".stripMargin,
            usrId
        )
    }

    /**
      * Gets company for given probe token.
      *
      * @param tkn Probe token.
      * @return Company MDO.
      */
    @throws[NCE]
    def getCompanyByProbeToken(tkn: String): Option[NCCompanyMdo] = {
        ensureStarted()
        
        NCPsql.
            selectSingle[Long]("SELECT id FROM company WHERE probe_token = ? AND deleted = FALSE", tkn).
            flatMap(getCompany)
    }

    /**
     * Gets user for given email.
     *
     * @param email User's email.
     * @return User MDO.
     */
    @throws[NCE]
    def getUserByEmail(email: String): Option[NCUserMdo] = {
        ensureStarted()

        NCPsql.selectSingle[NCUserMdo](
            """
              |SELECT *
              |FROM company_user
              |WHERE
              |     email = ? AND
              |     deleted = FALSE""".stripMargin,
            G.normalizeEmail(email)
        )
    }
    
    /**
      * Sets the flag to force user password reset. No-op if it's already set for this user.
      * 
      * @param usrId User ID.
      */
    @throws[NCE]
    def setPasswordReset(usrId: Long): Unit = {
        ensureStarted()
        
        if (!isPasswordReset(usrId))
            NCPsql.insert(
                """
                  | INSERT INTO pwd_reset (user_id)
                  | VALUES (?)
                """.stripMargin,
                usrId)
    }
    
    /**
      * Clears the flag to force user password reset.
      *
      * @param usrId User ID.
      */
    @throws[NCE]
    def clearPasswordReset(usrId: Long): Unit = {
        ensureStarted()
        
        NCPsql.delete(
            """
              | DELETE FROM pwd_reset
              | WHERE
              |     user_id = ?""".stripMargin,
            usrId
        )
    }
    
    /**
      * Checks whether or not flag is set to force user password reset.
      *
      * @param usrId User ID.
      * @return
      */
    @throws[NCE]
    def isPasswordReset(usrId: Long): Boolean = {
        ensureStarted()
    
        NCPsql.exists(
            """
              | SELECT 1
              | FROM
              |     pwd_reset
              | WHERE
              |     user_id = ?
            """.stripMargin,
            usrId)
    }

    /**
     * Deactivates first login flag of user with given email.
     *
     * @param email User's email.
     * @return 'true' if flag was successfully deactivated.
     */
    @throws[NCE]
    def deactivateFirstLogin(email: String): Boolean = {
        ensureStarted()

        getUserByEmail(email) match {
            case None ⇒ false
            case Some(user) ⇒ NCPsql.update(
                """
                  |UPDATE
                  |     company_user
                  |SET
                  |     is_first_login = false,
                  |     last_modified_on = current_timestamp
                  |WHERE
                  |     id = ? AND
                  |     deleted = FALSE""".stripMargin,
                user.id) match {
                    case 1 ⇒
                        logger.trace(s"User first login flag deactivated for $email")
        
                        true
                        
                    case _ ⇒
                        false
                }
        }
    }

    /**
      * Finds the best active data source ID for a given company. It picks the most frequently used one, and
      * if there isn't one it will pick the oldest active data source.
      *
      * @param compId Company ID.
      */
    @throws[NCE]
    def findActiveDataSourceId(compId: Long): Option[Long] = {
        ensureStarted()

        // First, check if we have already an active data source that is used the most.
        NCPsql.select[Long](
            """
              |SELECT u.active_ds_id
              |FROM
              |    company_user u,
              |    ds_instance d
              |WHERE
              |    u.company_id = ? AND
              |    u.deleted = FALSE AND
              |    u.active_ds_id != -1 AND
              |    u.active_ds_id = d.id AND
              |    d.deleted = FALSE AND
              |    d.enabled = TRUE
              |GROUP BY u.active_ds_id
              |ORDER BY COUNT(u.active_ds_id) DESC
            """.stripMargin,
            compId
        ) match {
            case head :: _ ⇒ Some(head) // Bingo!
            case Nil ⇒
                // Pick the oldest enabled data source for this company.
                NCPsql.select[Long](
                    """
                      |SELECT d.id
                      |FROM
                      |  ds_instance d,
                      |  company c,
                      |  company_user u
                      |WHERE
                      |    c.id = ? AND
                      |    c.id = u.id AND
                      |    d.user_id = u.id AND
                      |    u.deleted = FALSE AND
                      |    c.deleted = FALSE AND
                      |    d.enabled = TRUE AND
                      |    d.deleted = FALSE
                      |ORDER BY d.last_modified_on ASC;
                    """.stripMargin,
                    compId
                ).headOption
        }
    }

    /**
      * Adds new user with given parameters.
      *
      * @param origin User's origin, e.g. 'dev', 'web', etc.
      * @param firstName User's first name.
      * @param lastName User's last name.
      * @param activeDsId ID of the default active data source for this user.
      * @param email User's email.
      * @param title User's title.
      * @param department User's department.
      * @param companyId Company ID this user belongs to.
      * @param passwdSalt Optional salt for password Blowfish hashing.
      * @param referralCode Referral code.
      * @return Newly added user ID.
      */
    @throws[NCE]
    def addUser(
        origin: String,
        avatarUrl: String,
        firstName: String,
        lastName: String,
        activeDsId: Long,
        email: String,
        title: String,
        department: String,
        companyId: Long,
        admin: Boolean,
        prefsJson: String,
        referralCode: String,
        passwdSalt: String = null,
        isRoot: Boolean = false
        ): Long = {
        ensureStarted()

        // Insert user.
        NCPsql.insertGetKey[Long](
            """
               | INSERT INTO company_user
               | (
               |    origin,
               |    avatar_url,
               |    first_name,
               |    last_name,
               |    rank,
               |    active_ds_id,
               |    email,
               |    title,
               |    department,
               |    is_active,
               |    is_admin,
               |    company_id,
               |    is_first_login,
               |    prefs_json,
               |    referral_code,
               |    passwd_salt,
               |    is_root
               | )
               | VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, true, ?, ?, true, ?, ?, ?, ?)""".stripMargin,
            origin,
            avatarUrl,
            firstName,
            lastName,
            0, // Rank.
            activeDsId,
            G.normalizeEmail(email),
            title,
            department,
            admin,
            companyId,
            prefsJson,
            referralCode,
            passwdSalt,
            isRoot
        )
    }

    /**
     * Adds new feedback with given message.
     *
     * @param message Feedback message.
     */
    @throws[NCE]
    def addGeneralFeedback(usrId: Long, message: String): Unit = {
        ensureStarted()

        NCPsql.insert(
            """
              | INSERT INTO feedback (
              |     user_id,
              |     message
              | )
              | VALUES (?, ?)
            """.stripMargin,
            usrId,
            message
        )
    }

    /**
      * Gets extended data for given user.
      *
      * @param usrId User ID.
      */
    @throws[NCE]
    def getUserData(usrId: Long): Option[NCUserDataMdo] = {
        ensureStarted()
        
        NCPsql.selectSingle[NCUserDataMdo](
            s"""
               |SELECT
               |    u.id,
               |    u.avatar_url,
               |    u.first_name,
               |    u.last_name,
               |    u.title,
               |    u.phone,
               |    u.email,
               |    u.department,
               |    c.name as company,
               |    c.origin as company_origin,
               |    u.origin as user_origin,
               |    c.sign_up_domain sign_up_domain,
               |    u.created_on registration_date,
               |    u.is_active,
               |    u.is_first_login,
               |    u.referral_code,
               |    u.tmz_name,
               |    u.tmz_abbr,
               |    u.longitude,
               |    u.latitude,
               |    u.country_name,
               |    u.country_code,
               |    u.region_name,
               |    u.region_code,
               |    u.zip_code,
               |    u.metro_code,
               |    u.company_id,
               |    u.city,
               |    u.passwd_salt,
               |    u.active_ds_id,
               |    (
               |        SELECT MAX(h.tstamp)
               |        FROM login_history h
               |        WHERE h.user_id = u.id
               |    ) last_login_time,
               |    (
               |        SELECT MAX(h.recv_tstamp)
               |        FROM history h
               |        WHERE h.user_id = u.id
               |    ) last_question_time,
               |    (
               |        SELECT COUNT(*)
               |        FROM history h
               |        WHERE h.user_id = u.id
               |    ) total_question_count,
               |    u.is_admin,
               |    u.is_root,
               |    u.prefs_json
               |  FROM
               |    company_user u,
               |    company c
               |  WHERE
               |    u.id = ? AND
               |    u.company_id = c.id AND
               |    u.deleted = FALSE AND
               |    c.deleted = FALSE
            """.stripMargin,
            usrId
        )
    }

    /**
      * Gets extended data for given data source.
      *
      * @param dsId Data source ID to get extended data for.
      */
    @throws[NCE]
    def getDataSourceData(dsId: Long): Option[NCDataSourceDataMdo] = {
        ensureStarted()

        NCPsql.selectSingle[NCDataSourceDataMdo](
            s"""
               |SELECT
               |    d.id,
               |    d.name,
               |    d.short_desc,
               |    d.user_id,
               |    d.enabled,
               |    d.model_id,
               |    d.model_cfg,
               |    d.model_ver,
               |    d.model_name,
               |    u.id,
               |    u.avatar_url,
               |    u.first_name,
               |    u.last_name,
               |    u.email,
               |    c.name company,
               |    c.probe_token probe_token,
               |    c.origin as company_origin,
               |    u.origin as user_origin,
               |    c.sign_up_domain sign_up_domain,
               |    u.created_on registration_date,
               |    u.is_active,
               |    (
               |        SELECT MAX(h.tstamp)
               |            FROM login_history h
               |            WHERE h.user_id = u.id
               |    ) last_login_time,
               |    (
               |        SELECT MAX(h.recv_tstamp)
               |            FROM history h
               |            WHERE h.user_id = u.id
               |    ) last_question_time,
               |    (
               |        SELECT COUNT(*)
               |            FROM history h
               |            WHERE h.user_id = u.id
               |    ) total_question_count,
               |    u.is_admin,
               |    u.is_root,
               |    u.prefs_json
               |  FROM
               |    company_user u,
               |    company c,
               |    ds_instance d
               |  WHERE
               |    d.id = ? AND
               |    d.user_id = u.id AND
               |    u.company_id = c.id AND
               |    u.deleted = FALSE AND
               |    d.deleted = FALSE AND
               |    c.deleted = FALSE
            """.stripMargin,
            dsId
        )
    }
    
    /**
      * Gets extended data for all users in given company.
      *
      * @param compId Company ID.
      */
    @throws[NCE]
    def getUsersDataForCompany(compId: Long): List[NCUserDataMdo] = {
        ensureStarted()
        
        NCPsql.select[NCUserDataMdo](
            s"""
               |SELECT
               |    u.id,
               |    u.avatar_url,
               |    u.first_name,
               |    u.last_name,
               |    u.title,
               |    u.phone,
               |    u.email,
               |    u.company_id,
               |    u.department,
               |    c.name as company,
               |    c.origin as company_origin,
               |    u.origin as user_origin,
               |    c.sign_up_domain sign_up_domain,
               |    u.created_on registration_date,
               |    u.is_active,
               |    u.is_first_login,
               |    u.active_ds_id,
               |    u.referral_code,
               |    u.tmz_name,
               |    u.tmz_abbr,
               |    u.longitude,
               |    u.latitude,
               |    u.country_name,
               |    u.country_code,
               |    u.region_name,
               |    u.region_code,
               |    u.zip_code,
               |    u.metro_code,
               |    u.city,
               |    u.passwd_salt,
               |    (
               |        SELECT MAX(h.tstamp)
               |        FROM login_history h
               |        WHERE h.user_id = u.id
               |    ) last_login_time,
               |    (
               |        SELECT MAX(h.recv_tstamp)
               |        FROM history h
               |        WHERE h.user_id = u.id
               |    ) last_question_time,
               |    (
               |        SELECT COUNT(*)
               |        FROM history h
               |        WHERE h.user_id = u.id
               |    ) total_question_count,
               |    u.is_admin,
               |    u.is_root,
               |    u.prefs_json,
               |    (
               |        SELECT COUNT(*) > 0
               |        FROM ds_instance ds
               |        WHERE ds.user_id = u.id AND ds.deleted = FALSE
               |    ) is_ds_owner
               |  FROM
               |      company_user u
               |      INNER JOIN company c ON u.company_id = c.id
               |  WHERE
               |    c.id = ? AND
               |    u.deleted = FALSE AND
               |    c.deleted = FALSE
            """.stripMargin,
            compId
        )
    }
    
    /**
      * Gets admin users for given company ID.
      *
      * @param compId Company ID.
      */
    @throws[NCE]
    def getAdminsForCompany(compId: Long): List[NCUserMdo] = {
        ensureStarted()
    
        NCPsql.select[NCUserMdo](
            s"""
               |SELECT
               |  *
               |FROM
               |  company_user
               |WHERE
               |  company_id = ? AND
               |  is_admin = TRUE AND
               |  is_active = TRUE AND
               |  deleted = FALSE
            """.stripMargin,
            compId)
    }

    /**
     * Activates specified user.
     *
     * @param usrId User ID.
     * @return 'true' if user was successfully deactivated.
     */
    @throws[NCE]
    def activateUser(usrId: Long): Boolean =
        activate0(usrId, active = true)

    /**
     * Activates specified user.
     *
     * @param email User email.
     * @return 'true' if user was successfully deactivated.
     */
    @throws[NCE]
    def activateUser(email: String): Boolean =
        activate0(email, active = true)

    /**
     * Deactivates specified user.
     *
     * @param usrId User ID.
     * @return 'true' if user was successfully deactivated.
     */
    @throws[NCE]
    def deactivateUser(usrId: Long): Boolean =
        activate0(usrId, active = false)

    /**
     * Deactivates specified user.
     *
     * @param email User email.
     * @return 'true' if user was successfully deactivated.
     */
    @throws[NCE]
    def deactivateUser(email: String): Boolean =
        activate0(email, active = false)

    /**
     * Sets admin flag for specified user.
     *
     * @param usrId User ID.
     */
    @throws[NCE]
    def makeAdmin(usrId: Long): Unit =
        changeUserAdmin(usrId, flag = true)

    /**
     * Revokes admin flag from specified user.
     *
     * @param usrId User ID.
     */
    @throws[NCE]
    def revokeAdmin(usrId: Long): Unit =
        changeUserAdmin(usrId, flag = false)

    /**
      * Gets number of active (enabled) users in user's company.
      *
      * @param usrId User ID.
      */
    @throws[NCE]
    def userCount(usrId: Long): Long = {
        ensureStarted()

        NCPsql.selectSingle[Long](
            s"""
               |SELECT COUNT(*)
               |FROM
               |    company_user cu
               |WHERE
               |    cu.company_id = (SELECT company_id FROM company_user WHERE id = ?) AND
               |    cu.deleted = FALSE AND
               |    cu.is_active = TRUE
            """.stripMargin,
            usrId
        ).getOrElse(0)
    }
    
    /**
      * Deletes user record with given ID.
      *
      * @param usrId User ID.
      */
    @throws[NCE]
    def deleteUser(usrId: Long): Unit = {
        ensureStarted()
        
        NCPsql.markAsDeleted("company_user", "id", usrId)
    }
    
    /**
      * Deletes all invites with either given user ID  or email.
      * 
      * @param usrId User ID.
      * @param email User email.
      */
    @throws[NCE]
    def deleteAllInvites(usrId: Long, email: String): Unit = {
        ensureStarted()
        
        NCPsql.markAsDeleted("invite", "user_id", usrId)
        NCPsql.markAsDeleted("invite", "email", G.normalizeEmail(email))
    }
    
    /**
      * Deletes password reset flags for given user.
      *
      * @param usrId User ID.
      */
    @throws[NCE]
    def deletePasswordResets(usrId: Long): Unit = {
        ensureStarted()
        
        NCPsql.delete(
            """
              |DELETE FROM pwd_reset
              |  WHERE
              |    user_id = ?""".stripMargin,
            usrId
        )
    }
    
    /**
      * Deletes user - data source permissions.
      *
      * @param usrId User ID.
      */
    @throws[NCE]
    def deleteUserDataSourcePermissions(usrId: Long): Unit = {
        ensureStarted()
        
        NCPsql.delete(
            """
              |DELETE FROM user_ds
              |  WHERE
              |    user_id = ?""".stripMargin,
            usrId
        )
    }

    /**
      * Delete company record with given ID.
      *
      * @param compId Company ID.
      */
    @throws[NCE]
    def deleteCompany(compId: Long): Unit = {
        ensureStarted()
        
        NCPsql.markAsDeleted("company", "id", compId)
    }
        
    /**
      * Whether or not this is the last active user in its company.
      * 
      * @param usrId User ID.
      */
    @throws[NCE]
    def isLastUser(usrId: Long): Boolean = {
        ensureStarted()
    
        NCPsql.selectSingle[Long](
            """
              | SELECT id
              | FROM company c
              | WHERE
              |     id = (
              |         SELECT company_id
              |         FROM company_user
              |         WHERE
              |             id = ? AND
              |             deleted = FALSE
              |     ) AND
              |     NOT EXISTS (
              |         SELECT NULL
              |         FROM company_user u
              |         WHERE
              |             u.company_id = c.id AND
              |             u.id <> ? AND
              |             u.deleted = FALSE
              |     )
            """.stripMargin,
            usrId,
            usrId
        ).isDefined
    }
    
    /**
      * Deletes given data source.
      *
      * @param dsId ID of the data source to delete.
      */
    @throws[NCE]
    def deleteDataSource(dsId: Long): Unit = {
        ensureStarted()
        
        // Delete data source.
        NCPsql.markAsDeleted("ds_instance", "id", dsId)
    
        // Clean up user-ds mapping.
        NCPsql.delete(
            """
              |DELETE FROM user_ds
              |WHERE
              |  ds_id = ?""".stripMargin,
            dsId
        )
    }
    
    @throws[NCE]
    def deleteAllDataSources(compId: Long): Unit = {
        ensureStarted()
        
        val usrIds = NCPsql.select[Long](
            """
              |SELECT id
              |FROM company_user
              |WHERE
              |     company_id = ?     
            """.stripMargin,
            compId)
        
        for (usrId ← usrIds)
            NCPsql.markAsDeleted("ds_instance", "user_id", usrId)
    }
    
    /**
      * Enables or disables given data source.
      *
      * @param dsId ID of the data source to enable or disable.
      * @param enable Enable or disable flag.
      * @return True or false depending on whether the update occurred.
      */
    @throws[NCE]
    def enableDataSource(dsId: Long, enable: Boolean): Boolean = {
        NCPsql.update(
            s"""
               |UPDATE ds_instance
               |SET
               |    enabled = ?
               |WHERE
               |    DELETED = FALSE AND
               |    id = ?""".stripMargin,
            enable,
            dsId
        ) == 1
    }
    
    /**
      * Updates given data source.
      *
      * @param dsId ID of the data source to enable or disable.
      * @param dsName New data source name.
      * @param dsDesc New data source description.
      * @return True or false depending on whether the update occurred.
      */
    @throws[NCE]
    def updateDataSource(dsId: Long, dsName: String, dsDesc: String): Boolean = {
        NCPsql.update(
            s"""
               |UPDATE ds_instance
               |SET
               |    name = ?,
               |    short_desc = ?
               |WHERE
               |    DELETED = FALSE AND
               |    id = ?""".stripMargin,
            dsName,
            dsDesc,
            dsId
        ) == 1
    }

    /**
     * Checks if the company with given domain already exists.
     *
     * @param domain Domain to check.
     */
    @throws[NCE]
    def isCompanyExistByDomain(domain: String): Boolean = {
        ensureStarted()

        NCPsql.exists(
            """
              | company WHERE lower(sign_up_domain) = ? AND deleted = FALSE
            """.stripMargin,
            domain.toLowerCase
        )
    }

    /**
     * Checks if the DB is accessible.
     *
     * NOTE: this method will acquire SQL connection on its own (i.e. the calling method
     * should call 'NCPsql.sql {...}' as requires for all other functions in this class).
     *
     * NOTE: this method doesn't throw any exceptions.
     */
    def isDbAccessible: Boolean = {
        ensureStarted()

        try {
            NCPsql.sql {
                NCPsql.selectSingle[Long]("SELECT COUNT(*) FROM company")
            }

            true
        }
        catch {
            case _: Throwable ⇒ false
        }
    }

    /**
     * Gets submit keys.
     *
     * @param mainId Main cache ID.
     */
    @throws[NCE]
    def getSubmitKeys(mainId: Long): List[String] = {
        ensureStarted()

        NCPsql.select[String](
            """
              | SELECT cache_key
              | FROM submit_cache
              | WHERE main_cache_id = ?
            """.stripMargin,
            mainId
        )
    }

    /**
     * Gets submit keys.
     *
     * @param mainId Main cache ID.
     */
    def getSynonymsKeys(mainId: Long): List[NCSynonymCacheKeyMdo] = {
        ensureStarted()

        NCPsql.select[NCSynonymCacheKeyMdo](
            """
              | SELECT
              |     model_id,
              |     cache_key,
              |     base_words
              | FROM synonyms_cache
              | WHERE main_cache_id = ?
            """.stripMargin,
            mainId
        )
    }

    /**
     * Gets last questions asked by user with time.
     *
     * @param usrId USer ID.
     * @param from From date.
     * @param limit Max questions count.
     */
    @throws[NCE]
    def getLastUserQuestions(usrId: Long, from: Date, limit: Int): Seq[NCLastQuestionMdo] = {
        ensureStarted()

        Seq.empty
    }

    /**
     * Gets data sources for given user ID.
     *
     * @param usrId User ID.
     */
    @throws[NCE]
    def getUserDataSourceIds(usrId: Long): Seq[Long] = {
        NCPsql.select[Long](
            """
              |SELECT id
              |  FROM
              |    ds_instance
              |  WHERE
              |    deleted = FALSE AND
              |    user_id = ?""".stripMargin,
            usrId
        )
    }

    /**
     * Gets users identifiers, which use data sources of given owner.
     *
     * @param dsOwnerId Datasource owner user identifier.
     */
    @throws[NCE]
    def getUsersByDsOwner(dsOwnerId: Long): Seq[Long] = {
        ensureStarted()

        NCPsql.select[Long](
              s"""
               |SELECT id
               |FROM company_user
               |WHERE
               |    deleted = FALSE AND
               |    active_ds_id IN (SELECT id FROM ds_instance WHERE user_id = ?)""".stripMargin,
            dsOwnerId
        )
    }


    /**
     * Gets all timezones list (including `null`)
     */
    @throws[NCE]
    def getAllUsersTimezones: Seq[String] = {
        ensureStarted()

        NCPsql.select[String](
            """
              |SELECT DISTINCT tmz_name
              |  FROM
              |    company_user
              |  WHERE
              |    deleted = FALSE""".stripMargin)
    }

    /**
      * Gets maximum long column value.
      *
      * @param table Table name.
      * @param col Column name.
      */
    @throws[NCE]
    def getMaxColumnValue(table: String, col: String): Option[Long] = {
        ensureStarted()

        NCPsql.selectSingle[Long](s"SELECT max($col) FROM $table")
    }

    /**
      * Gets history records count.
      *
      * @param usrId User ID.
      */
    def getHistoryCount(usrId: Long): Int = {
        ensureStarted()

        val v = NCPsql.selectSingle[Int](
            "SELECT COUNT(*) FROM (SELECT DISTINCT srv_req_id FROM history WHERE user_id = ?) a",
            usrId
        )

        require(v.isDefined)

        v.get
    }

    /**
      * Gets last history timestamp.
      *
      * @param usrId User ID.
      */
    def getLastHistoryTimestamp(usrId: Long): Option[Timestamp] = {
        ensureStarted()

        NCPsql.selectSingle[Timestamp]("SELECT max(recv_tstamp) FROM history WHERE user_id = ?", usrId)
    }

    /**
      * Checks whether or not given server request ID exists in history.
      *
      * @param srvReqId Server request ID.
      * @return
      */
    def isHistoryExist(srvReqId: String): Boolean = {
        ensureStarted()

        NCPsql.selectSingle[Int]("SELECT 1 WHERE EXISTS (SELECT NULL FROM history WHERE srv_req_id = ?)", srvReqId) match {
            case Some(_) ⇒ true
            case None ⇒ false
        }
    }

    /**
      * Deletes sentence history.
      *
      * @param srvReqId Server request ID.
      */
    @throws[NCE]
    def deleteHistory(srvReqId: String): Unit = {
        ensureStarted()

        NCPsql.delete("DELETE FROM history WHERE srv_req_id = ?", srvReqId)
    }

    /**
      * Gets main cache ID for server request.
      *
      * @param srvReqId Server request ID.
      */
    @throws[NCE]
    def getCacheId(srvReqId: String): Option[Long] = {
        ensureStarted()

        NCPsql.selectSingle[Long](
            """
              | SELECT cache_id
              | FROM history
              | WHERE
              |     srv_req_id = ? AND
              |     cache_id IS NOT NULL
              | LIMIT 1
            """.stripMargin,
            srvReqId)
    }

    /**
      * Updates history result record.
      *
      * @param srvReqId
      * @param status
      * @param resType
      * @param resBody
      * @param error
      * @param curateTxt
      * @param curateHint
      * @param lingUsrId
      * @param lingOp
      * @param cacheId
      * @param probe
      */
    def updateHistory(
        srvReqId: String,
        status: Option[NCApiStatusCode] = None,
        resType: Option[String] = None,
        resBody: Option[String] = None,
        error: Option[String] = None,
        curateTxt: Option[String] = None,
        curateHint: Option[String] = None,
        lingUsrId: Option[Long] = None,
        lingOp: Option[String] = None,
        cacheId: Option[Long] = None,
        feedbackMsg: Option[String] = None,
        probe: Option[NCProbeMdo] = None
    ): Unit = {
        ensureStarted()
        
        if (resType.isDefined && resBody.isDefined)
            NCPsql.update(
                """
                  | UPDATE history
                  | SET
                  |      res_type = ?,
                  |      res_body_gzip = ?,
                  |      resp_tstamp = current_timestamp
                  | WHERE
                  |      srv_req_id = ?
                """.stripMargin,
                resType.get,
                G.compress(resBody.get),
                srvReqId
            )
        else if (error.isDefined)
            NCPsql.update(
                """
                  | UPDATE history
                  | SET
                  |      error = ?,
                  |      resp_tstamp = current_timestamp
                  | WHERE
                  |      srv_req_id = ?
                """.stripMargin,
                error.get,
                srvReqId
            )
    
        if (status.isDefined)
            NCPsql.update(
                """
                  | UPDATE history
                  | SET
                  |      status = ?
                  | WHERE
                  |      srv_req_id = ?
                """.stripMargin,
                status.get.toString,
                srvReqId
            )

        if (curateTxt.isDefined)
            NCPsql.update(
                """
                  | UPDATE history
                  | SET
                  |      curate_txt = ?
                  | WHERE
                  |      srv_req_id = ?
                """.stripMargin,
                curateTxt.get,
                srvReqId
            )
        if (curateHint.isDefined)
            NCPsql.update(
                """
                  | UPDATE history
                  | SET
                  |      curate_hint = ?
                  | WHERE
                  |      srv_req_id = ?
                """.stripMargin,
                curateHint.get,
                srvReqId
            )
        if (cacheId.isDefined)
            NCPsql.update(
                """
                  | UPDATE history
                  | SET
                  |      cache_id = ?
                  | WHERE
                  |      srv_req_id = ?
                """.stripMargin,
                cacheId.get,
                srvReqId
            )
        if (feedbackMsg.isDefined)
            NCPsql.update(
                """
                  | UPDATE history
                  | SET
                  |      feedback_msg = ?
                  | WHERE
                  |      srv_req_id = ?
                """.stripMargin,
                feedbackMsg.get,
                srvReqId
            )
        
        if (lingOp.isDefined || lingUsrId.isDefined)
            NCPsql.update(
                """
                  | UPDATE history
                  | SET
                  |      last_linguist_user_id = ?,
                  |      last_linguist_op = ?
                  | WHERE
                  |      srv_req_id = ?
                """.stripMargin,
                lingUsrId.get,
                lingOp.get,
                srvReqId
            )
        
        if (probe.isDefined) {
            val p = probe.get
            
            NCPsql.update(
                """
                  | UPDATE history
                  | SET
                  |    probe_token = ?,
                  |    probe_id = ?,
                  |    probe_guid = ?,
                  |    probe_api_version = ?,
                  |    probe_api_date = ?,
                  |    probe_os_version = ?,
                  |    probe_os_name = ?,
                  |    probe_os_arch = ?,
                  |    probe_start_tstamp = ?,
                  |    probe_tmz_id = ?,
                  |    probe_tmz_name = ?,
                  |    probe_tmz_abbr = ?,
                  |    probe_user_name = ?,
                  |    probe_java_version = ?,
                  |    probe_java_vendor = ?,
                  |    probe_host_name = ?,
                  |    probe_host_addr = ?,
                  |    probe_mac_addr = ?,
                  |    probe_email = ?
                  | WHERE
                  |      srv_req_id = ?
                """.stripMargin,
                p.probeToken,
                p.probeId,
                p.probeGuid,
                p.probeApiVersion,
                new Timestamp(p.probeApiDate),
                p.osVersion,
                p.osName,
                p.osArch,
                new Timestamp(p.startTstamp),
                p.tmzId,
                p.tmzName,
                p.tmzAbbr,
                p.userName,
                p.javaVersion,
                p.javaVendor,
                p.hostName,
                p.hostAddr,
                p.macAddr,
                p.email.orNull,
                srvReqId
            )
        }
    }
}