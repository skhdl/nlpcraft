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
-- | Postgres SQL schema definition. |
-- +=================================+
--
-- NOTE: database 'nlpcraft' should be created and owned by 'nlpcraft' user.
--

--
-- User table.
--
CREATE TABLE nc_user (
    id SERIAL PRIMARY KEY,
    email VARCHAR(64) NOT NULL, -- Used as username during login.
    avatar_url TEXT NULL, -- URL or encoding of avatar for this user, if any.
    first_name VARCHAR(64) NOT NULL,
    last_name VARCHAR(64) NOT NULL,
    is_admin BOOL NOT NULL, -- Whether or not created with admin token.
    passwd_salt VARCHAR(64) NOT NULL,
    created_on TIMESTAMP NOT NULL DEFAULT current_timestamp,
    last_modified_on TIMESTAMP NOT NULL DEFAULT current_timestamp
);

CREATE INDEX nc_user_idx_1 ON nc_user(email);

--
-- Pool of password hashes.
--
CREATE TABLE passwd_pool (
    id SERIAL PRIMARY KEY,
    passwd_hash VARCHAR(64) NOT NULL
);

--
-- Processing log.
--
CREATE TABLE proc_log (
    id SERIAL PRIMARY KEY,
    srv_req_id VARCHAR(64) NOT NULL,
    txt VARCHAR(1024) NULL,
    user_id BIGINT NULL,
    model_id VARCHAR(64) NULL,
    status VARCHAR(32) NULL,
    user_agent VARCHAR(512) NULL,
    rmt_address VARCHAR(256) NULL,
    sen_data TEXT NULL,
    -- Ask and result timestamps.
    recv_tstamp TIMESTAMP NOT NULL, -- Initial receive timestamp.
    resp_tstamp TIMESTAMP NULL, -- Result or error response timestamp.
    cancel_tstamp TIMESTAMP NULL, -- Cancel timestamp.
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
    probe_start_tstamp TIMESTAMP NULL,
    probe_tmz_id VARCHAR(64) NULL,
    probe_tmz_abbr VARCHAR(64) NULL,
    probe_tmz_name VARCHAR(64) NULL,
    probe_user_name VARCHAR(512) NULL,
    probe_java_version VARCHAR(512) NULL,
    probe_java_vendor VARCHAR(512) NULL,
    probe_host_name VARCHAR(1024) NULL,
    probe_host_addr VARCHAR(512) NULL,
    probe_mac_addr VARCHAR(512) NULL
);

CREATE UNIQUE INDEX proc_log_idx_1 ON proc_log(srv_req_id);