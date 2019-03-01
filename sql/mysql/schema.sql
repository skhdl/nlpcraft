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
--
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
--

--
-- +=================================+
-- | MYSQL SQL schema definition. |
-- +=================================+
--
-- NOTE: database 'nlpcraft' should be created.
--

USE nlpcraft;

--
-- User table.
--
DROP TABLE IF EXISTS nc_user CASCADE;
CREATE TABLE nc_user (
    id SERIAL PRIMARY KEY,
    email VARCHAR(64) NOT NULL, -- Used as username during login.
    avatar_url TEXT NULL, -- URL or encoding of avatar for this user, if any.
    first_name VARCHAR(64) NOT NULL,
    last_name VARCHAR(64) NOT NULL,
    last_ds_id BIGINT NULL,
    is_admin BOOLEAN NOT NULL, -- Whether or not created with admin token.
    passwd_salt VARCHAR(64) NOT NULL,
    created_on TIMESTAMP(3) NOT NULL DEFAULT current_timestamp(3),
    last_modified_on TIMESTAMP(3) NOT NULL DEFAULT current_timestamp(3)
);

CREATE UNIQUE INDEX nc_user_idx_1 ON nc_user(email);
CREATE INDEX nc_user_idx_3 ON nc_user(last_ds_id);

--
-- Pool of password hashes.
--
DROP TABLE IF EXISTS passwd_pool CASCADE;
CREATE TABLE passwd_pool (
    passwd_hash VARCHAR(64) NOT NULL
);

--
-- Instance of data source.
--
DROP TABLE IF EXISTS ds_instance CASCADE;
CREATE TABLE ds_instance (
    id SERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL, -- User friendly (non-unique) name of the data source.
    short_desc VARCHAR(128) NULL, -- Short, optional description additional to the name.
    model_id VARCHAR(32) NOT NULL,
    model_name VARCHAR(64) NOT NULL,
    model_ver VARCHAR(16) NOT NULL,
    model_cfg TEXT NULL,
    created_on TIMESTAMP(3) NOT NULL DEFAULT current_timestamp(3),
    last_modified_on TIMESTAMP(3) NOT NULL DEFAULT current_timestamp(3)
);

--
-- Processing log.
--
DROP TABLE IF EXISTS proc_log CASCADE;
CREATE TABLE proc_log (
    srv_req_id VARCHAR(64) PRIMARY KEY,
    txt VARCHAR(1024) NULL,
    user_id BIGINT NULL,
    ds_id BIGINT NULL,
    model_id VARCHAR(64) NULL,
    status VARCHAR(32) NULL,
    user_agent VARCHAR(512) NULL,
    rmt_address VARCHAR(256) NULL,
    -- Ask and result timestamps.
    recv_tstamp TIMESTAMP(3) NOT NULL, -- Initial receive timestamp.
    resp_tstamp TIMESTAMP(3) NULL, -- Result or error response timestamp.
    cancel_tstamp TIMESTAMP(3) NULL, -- Cancel timestamp.
    -- Result parts.
    res_type VARCHAR(32) NULL,
    res_body_gzip TEXT NULL, -- GZIP-ed result body.
    error TEXT NULL,
    -- Probe information for this request.
    probe_token VARCHAR(256) NULL,
    probe_id VARCHAR(512) NULL,
    probe_guid VARCHAR(512) NULL,
    probe_api_version VARCHAR(512) NULL,
    probe_api_date DATE NULL,
    probe_os_version VARCHAR(512) NULL,
    probe_os_name VARCHAR(512) NULL,
    probe_os_arch VARCHAR(512) NULL,
    probe_start_tstamp TIMESTAMP(3) NULL,
    probe_tmz_id VARCHAR(64) NULL,
    probe_tmz_abbr VARCHAR(64) NULL,
    probe_tmz_name VARCHAR(64) NULL,
    probe_user_name VARCHAR(512) NULL,
    probe_java_version VARCHAR(512) NULL,
    probe_java_vendor VARCHAR(512) NULL,
    probe_host_name VARCHAR(1024) NULL,
    probe_host_addr VARCHAR(512) NULL,
    probe_mac_addr VARCHAR(512) NULL,
    -- Whether or not this is a test run.
    is_test BOOLEAN NOT NULL DEFAULT FALSE
);

