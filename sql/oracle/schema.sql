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
-- | ORACLE SQL schema definition. |
-- +=================================+
--
-- NOTE: connect as 'nlpcraft'.
--

--
-- User table.
--
CREATE TABLE nc_user (
    id NUMBER PRIMARY KEY,
    email VARCHAR2(64) NOT NULL, -- Used as username during login.
    avatar_url VARCHAR(2083) NULL, -- URL or encoding of avatar for this user, if any.
    first_name VARCHAR2(64) NOT NULL,
    last_name VARCHAR2(64) NOT NULL,
    is_admin NUMBER(1) NOT NULL, -- Whether or not created with admin token.
    passwd_salt VARCHAR2(64) NOT NULL,
    created_on DATE DEFAULT sysdate NOT NULL,
    last_modified_on DATE DEFAULT sysdate NOT NULL
);

CREATE UNIQUE INDEX nc_user_idx_1 ON nc_user(email);

--
-- Pool of password hashes.
--
CREATE TABLE passwd_pool (
    id NUMBER PRIMARY KEY,
    passwd_hash VARCHAR2(64) NOT NULL
);

--
-- Processing log.
--
CREATE TABLE proc_log (
    id NUMBER PRIMARY KEY,
    srv_req_id VARCHAR2(64) NOT NULL,
    txt VARCHAR2(1024) NULL,
    user_id NUMBER NULL,
    model_id VARCHAR2(64) NULL,
    status VARCHAR2(32) NULL,
    user_agent VARCHAR2(512) NULL,
    rmt_address VARCHAR2(256) NULL,
    sen_data CLOB NULL,
    -- Ask and result timestamps.
    recv_tstamp DATE NOT NULL, -- Initial receive timestamp.
    resp_tstamp DATE NULL, -- Result or error response timestamp.
    cancel_tstamp DATE NULL, -- Cancel timestamp.
    -- Result parts.
    res_type VARCHAR2(32) NULL,
    res_body_gzip CLOB NULL, -- GZIP-ed result body.
    error CLOB NULL,
    -- Probe information for this request.
    probe_token VARCHAR2(256) NULL,
    probe_id VARCHAR2(512) NULL,
    probe_guid VARCHAR2(512) NULL,
    probe_api_version VARCHAR2(512) NULL,
    probe_api_date DATE NULL,
    probe_os_version VARCHAR2(512) NULL,
    probe_os_name VARCHAR2(512) NULL,
    probe_os_arch VARCHAR2(512) NULL,
    probe_start_tstamp DATE NULL,
    probe_tmz_id VARCHAR2(64) NULL,
    probe_tmz_abbr VARCHAR2(64) NULL,
    probe_tmz_name VARCHAR2(64) NULL,
    probe_user_name VARCHAR2(512) NULL,
    probe_java_version VARCHAR2(512) NULL,
    probe_java_vendor VARCHAR2(512) NULL,
    probe_host_name VARCHAR2(1024) NULL,
    probe_host_addr VARCHAR2(512) NULL,
    probe_mac_addr VARCHAR2(512) NULL
);

CREATE UNIQUE INDEX proc_log_idx_1 ON proc_log(srv_req_id);