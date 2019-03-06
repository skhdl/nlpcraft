--
--   “Commons Clause” License, https://commonsclause.com/
--
--   The Software is provided to you by the Licensor under the License,
--   as defined below, subject to the following condition.
--
--   Without limiting other conditions in the License, the grant of rights
--   under the License will not include, and the License does not grant to
--   you, the right to Sell the Software.
--
--   For purposes of the foregoing, “Sell” means practicing any or all of
--   the rights granted to you under the License to provide to third parties,
--   for a fee or other consideration (including without limitation fees for
--   hosting or consulting/support services related to the Software), a
--   product or service whose value derives, entirely or substantially, from
--   the functionality of the Software. Any license notice or attribution
--   required by the License must also include this Commons Clause License
--   Condition notice.

--   Software:    NLPCraft
--   License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
--   Licensor:    Copyright (C) 2018 DataLingvo, Inc. https://www.datalingvo.com
--
--       _   ____      ______           ______
--      / | / / /___  / ____/________ _/ __/ /_
--     /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
--    / /|  / / /_/ / /___/ /  / /_/ / __/ /_
--   /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
--          /_/

DROP TABLE IF EXISTS nc_user;
CREATE TABLE nc_user (
    id LONG PRIMARY KEY,
    email VARCHAR NOT NULL, -- Used as username during login.
    avatar_url VARCHAR NULL, -- URL or encoding of avatar for this user, if any.
    first_name VARCHAR NOT NULL,
    last_name VARCHAR NOT NULL,
    last_ds_id LONG NULL ,
    is_admin BOOL NOT NULL, -- Whether or not created with admin token.
    passwd_salt VARCHAR NOT NULL,
    created_on TIMESTAMP NOT NULL,
    last_modified_on TIMESTAMP NOT NULL
) WITH "template=replicated, atomicity=transactional";

CREATE INDEX nc_user_idx_1 ON nc_user(email);
CREATE INDEX nc_user_idx_3 ON nc_user(last_ds_id);

DROP TABLE IF EXISTS passwd_pool;
CREATE TABLE passwd_pool (
    id LONG PRIMARY KEY,
    passwd_hash VARCHAR NOT NULL
) WITH "template=replicated, atomicity=transactional";

DROP TABLE IF EXISTS ds_instance;
CREATE TABLE ds_instance (
    id LONG PRIMARY KEY,
    name VARCHAR NOT NULL, -- User friendly (non-unique) name of the data source.
    short_desc VARCHAR, -- Short, optional description additional to the name.
    model_id VARCHAR NOT NULL,
    model_name VARCHAR NOT NULL,
    model_ver VARCHAR NOT NULL,
    model_cfg VARCHAR NULL,
    created_on TIMESTAMP NOT NULL,
    last_modified_on TIMESTAMP NOT NULL
) WITH "template=replicated, atomicity=transactional";

DROP TABLE IF EXISTS proc_log;
CREATE TABLE proc_log (
    id LONG PRIMARY KEY,
    srv_req_id VARCHAR,
    txt VARCHAR NULL,
    user_id LONG NULL,
    ds_id LONG NULL,
    model_id VARCHAR NULL,
    status VARCHAR NULL,
    user_agent VARCHAR NULL,
    rmt_address VARCHAR NULL,
    -- Ask and result timestamps.
    recv_tstamp TIMESTAMP NOT NULL, -- Initial receive timestamp.
    resp_tstamp TIMESTAMP NULL, -- Result or error response timestamp.
    cancel_tstamp TIMESTAMP NULL, -- Cancel timestamp.
    -- Result parts.
    res_type VARCHAR NULL,
    res_body_gzip VARCHAR NULL, -- GZIP-ed result body.
    error VARCHAR NULL,
    -- Probe information for this request.
    probe_token VARCHAR NULL,
    probe_id VARCHAR NULL,
    probe_guid VARCHAR NULL,
    probe_api_version VARCHAR NULL,
    probe_api_date DATE NULL,
    probe_os_version VARCHAR NULL,
    probe_os_name VARCHAR NULL,
    probe_os_arch VARCHAR NULL,
    probe_start_tstamp TIMESTAMP NULL,
    probe_tmz_id VARCHAR NULL,
    probe_tmz_abbr VARCHAR NULL,
    probe_tmz_name VARCHAR NULL,
    probe_user_name VARCHAR NULL,
    probe_java_version VARCHAR NULL,
    probe_java_vendor VARCHAR NULL,
    probe_host_name VARCHAR NULL,
    probe_host_addr VARCHAR NULL,
    probe_mac_addr VARCHAR NULL
) WITH "template=replicated, atomicity=transactional";

