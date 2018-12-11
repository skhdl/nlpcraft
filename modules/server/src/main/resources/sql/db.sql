--
--  Licensed to the Apache Software Foundation (ASF) under one
--  or more contributor license agreements.  See the NOTICE file
--  distributed with this work for additional information
--  regarding copyright ownership.  The ASF licenses this file
--  to you under the Apache License, Version 2.0 (the
--  "License"); you may not use this file except in compliance
--  with the License.  You may obtain a copy of the License at
--
--  http://www.apache.org/licenses/LICENSE-2.0
--
--  Unless required by applicable law or agreed to in writing,
--  software distributed under the License is distributed on an
--  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
--  KIND, either express or implied.  See the License for the
--  specific language governing permissions and limitations
--  under the License.
--
--      _   ____      ______           ______
--     / | / / /___  / ____/________ _/ __/ /_
--    /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
--   / /|  / / /_/ / /___/ /  / /_/ / __/ /_
--  /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
--         /_/
--

SET client_min_messages TO WARNING;

--
-- +=================================+
-- | Postgres SQL schema definition. |
-- +=================================+
--
-- NOTE: database 'nlpcraft' should be created and owned by 'nlpcraft' user.
--
-- Supported types:
--
-- VARCHAR 1, 32, 64, 256, 512, 1024, 5120
-- BOOLEAN, DATE, TIMESTAMP, INTEGER, BIGINT(SERIAL), TEXT, BYTEA

-- Base entity type.
DROP TABLE IF EXISTS base;
CREATE TABLE base (
    deleted BOOL NOT NULL DEFAULT false,
    created_on TIMESTAMP NOT NULL DEFAULT current_timestamp,
    deleted_on TIMESTAMP,
    last_modified_on TIMESTAMP NOT NULL DEFAULT current_timestamp
);

--
-- Probe (mdllib/probe/global) releases.
--
DROP TABLE IF EXISTS probe_release CASCADE;
CREATE TABLE probe_release (
    version VARCHAR(16) NOT NULL,
    date TIMESTAMP NOT NULL DEFAULT current_timestamp, -- Latest date indicates latest version.
    size_bytes BIGINT NOT NULL,
    filename VARCHAR(64) NOT NULL, -- ZIP archive file name (no path).
    md5_sig_filename VARCHAR(64) NOT NULL, -- Just a file name (in the same folder as `filename`).
    sha1_sig_filename VARCHAR(64) NOT NULL, -- Just a file name (in the same folder as `filename`).
    sha256_sig_filename VARCHAR(64) NOT NULL, -- Just a file name (in the same folder as `filename`).
    pgp_sig_filename VARCHAR(64) NOT NULL -- Just a file name (in the same folder as `filename`).
);

--
-- Company table.
--
DROP TABLE IF EXISTS company CASCADE;
CREATE TABLE company (
    -- Inherit columns from base entity.
    LIKE base INCLUDING DEFAULTS,

    id SERIAL PRIMARY KEY,

    -- Sign up domain (must be UNIQUE).
    sign_up_domain VARCHAR(256) NOT NULL,

    -- Name & contact info (all optional).
    origin VARCHAR(16) NOT NULL, -- From where this company has been created, e.g. 'web', 'test', 'dev', etc.
    name VARCHAR(64),
    website VARCHAR(256),
    address VARCHAR(256),
    city VARCHAR(256),
    region VARCHAR(64),
    postal_code VARCHAR(32),
    country VARCHAR(256),
    probe_token VARCHAR(256) NOT NULL,
    probe_token_hash VARCHAR(64) NOT NULL
);

CREATE INDEX company_idx_1 ON company(sign_up_domain);
CREATE INDEX company_idx_2 ON company(origin);

CREATE UNIQUE INDEX company_uk_1 ON company(sign_up_domain) WHERE deleted = false;
CREATE UNIQUE INDEX company_uk_2 ON company(probe_token) WHERE deleted = false;
CREATE UNIQUE INDEX company_uk_3 ON company(probe_token_hash) WHERE deleted = false;

--
-- Invite table.
--
DROP TABLE IF EXISTS invite CASCADE;
CREATE TABLE invite (
    -- Inherit columns from base entity.
    LIKE base INCLUDING DEFAULTS,

    id SERIAL PRIMARY KEY,

    first_name VARCHAR(64) NOT NULL,
    last_name VARCHAR(64),
    email VARCHAR(64) NOT NULL,
    token VARCHAR(64),
    requested_on TIMESTAMP NOT NULL DEFAULT current_timestamp,
    invited_on TIMESTAMP,
    signed_up_on TIMESTAMP,
    user_id BIGINT
);

CREATE INDEX invite_idx_1 ON invite(email);

--
-- Dashboard item table.
--
DROP TABLE IF EXISTS dashboard_item CASCADE;
CREATE TABLE dashboard_item (
    -- Inherit columns from base entity.
    LIKE base INCLUDING DEFAULTS,

    id SERIAL PRIMARY KEY,

    text VARCHAR(1024) NOT NULL,
    user_id BIGINT NOT NULL,
    ds_id BIGINT NOT NULL
);

CREATE INDEX dashboard_item_idx_1 ON dashboard_item(text);
CREATE INDEX dashboard_item_idx_2 ON dashboard_item(ds_id);
CREATE INDEX dashboard_item_idx_3 ON dashboard_item(user_id);

--
-- Pool of password hashes.
--
DROP TABLE IF EXISTS passwd_pool CASCADE;
CREATE TABLE passwd_pool (
    passwd_hash VARCHAR(64) NOT NULL
);

--
-- Company user table.
--
DROP TABLE IF EXISTS company_user CASCADE;
CREATE TABLE company_user (
    -- Inherit columns from base entity.
    LIKE base INCLUDING DEFAULTS,

    id SERIAL PRIMARY KEY,
    origin VARCHAR(16) NOT NULL, -- From where this user has been created, e.g. 'web', 'test', 'dev', etc.
    avatar_url TEXT, -- URL or encoding of avatar for this user, if any.
    first_name VARCHAR(64) NOT NULL,
    last_name VARCHAR(64) NOT NULL,
    email VARCHAR(64) NOT NULL, -- Used as username during login.
    company_id BIGINT REFERENCES company, -- Company it belongs to.
    department VARCHAR(64), -- Company department or group (sales, marketing, etc.).
    title VARCHAR(64),
    passwd_salt VARCHAR(64), -- Optional salt for password hash (if password signup was used).
    is_active BOOL NOT NULL,
    is_first_login BOOL NOT NULL DEFAULT true,
    active_ds_id BIGINT NOT NULL,
    is_admin BOOL NOT NULL, -- Admin for the company.
    is_root BOOL NOT NULL, -- Global system root.
    rank INTEGER NOT NULL, -- Internal rank. The higher the rank, the mort important user.
    phone VARCHAR(64),
    prefs_json TEXT NOT NULL, -- JSON object containing user preferences.
    referral_code VARCHAR(32) NULL,

    -- All-optional IP-based GEO location information from last web access.
    tmz_name VARCHAR(64) NULL,
    tmz_abbr VARCHAR(8) NULL,
    latitude FLOAT NULL,
    longitude FLOAT NULL,
    city VARCHAR(64) NULL,
    country_name VARCHAR(64) NULL,
    country_code VARCHAR(8) NULL,
    region_name VARCHAR(64) NULL,
    region_code VARCHAR(8) NULL,
    zip_code VARCHAR(16) NULL,
    metro_code BIGINT NULL
);

CREATE INDEX company_user_idx_1 ON company_user(origin);
CREATE INDEX company_user_idx_2 ON company_user(email);
CREATE INDEX company_user_idx_3 ON company_user(company_id);
CREATE INDEX company_user_idx_4 ON company_user(is_active);
CREATE INDEX company_user_idx_5 ON company_user(is_first_login);
CREATE INDEX company_user_idx_6 ON company_user(active_ds_id);
CREATE INDEX company_user_idx_7 ON company_user(is_admin);
CREATE INDEX company_user_idx_8 ON company_user(rank);
CREATE INDEX company_user_idx_10 ON company_user(tmz_name);

CREATE UNIQUE INDEX company_user_uk_1 ON company_user(email) WHERE deleted = false;

--
-- Forced password reset table.
--
DROP TABLE IF EXISTS pwd_reset CASCADE;
CREATE TABLE pwd_reset (
    user_id BIGINT REFERENCES company_user,
    created_on TIMESTAMP NOT NULL DEFAULT current_timestamp
);


--
-- Instance (i.e. company specific instance) of data source.
--
DROP TABLE IF EXISTS ds_instance CASCADE;
CREATE TABLE ds_instance (
    -- Inherit columns from base entity.
    LIKE base INCLUDING DEFAULTS,

    id SERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL, -- User friendly (non-unique) name of the data source.
    short_desc VARCHAR(128) NULL, -- Short, optional description additional to the name.
    user_id BIGINT NOT NULL REFERENCES company_user, -- Company user that created that data source.
    enabled BOOL NOT NULL,
    model_id VARCHAR(32) NOT NULL,
    model_name VARCHAR(64) NOT NULL,
    model_ver VARCHAR(16) NOT NULL,
    model_cfg VARCHAR(5120) NOT NULL -- Empty if configuration is not used.
);

CREATE INDEX ds_instance_idx_1 ON ds_instance(user_id);
CREATE INDEX ds_instance_idx_2 ON ds_instance(enabled);


--
-- Links users and data sources.
--
DROP TABLE IF EXISTS user_ds CASCADE;
CREATE TABLE user_ds (
    ds_id BIGINT REFERENCES ds_instance,
    user_id BIGINT REFERENCES company_user,
    UNIQUE (ds_id, user_id)
);

--
-- Feedback table.
--
DROP TABLE IF EXISTS feedback CASCADE;
CREATE TABLE feedback (
    -- Inherit columns from base entity.
    LIKE base INCLUDING DEFAULTS,

    id SERIAL PRIMARY KEY,

    user_id BIGINT REFERENCES company_user,
    message VARCHAR(1024) NOT NULL
);

CREATE INDEX feedback_idx_1 ON feedback(user_id);

--
-- Main cache.
--
DROP TABLE IF EXISTS main_cache CASCADE;
CREATE TABLE main_cache (
    id SERIAL PRIMARY KEY,
    json BYTEA NOT NULL,
    tstamp TIMESTAMP NOT NULL DEFAULT current_timestamp,
    model_id VARCHAR(64) NOT NULL
);

CREATE INDEX main_cache_idx_1 ON main_cache(model_id);

--
-- Synonyms cache table.
--
DROP TABLE IF EXISTS synonyms_cache CASCADE;
CREATE TABLE synonyms_cache (
    id SERIAL PRIMARY KEY,
    main_cache_id BIGINT NOT NULL REFERENCES main_cache ON DELETE CASCADE,
    cache_key VARCHAR(5120) NOT NULL,
    base_words VARCHAR(5120) NOT NULL,
    sorted BOOL NOT NULL,
    model_id VARCHAR(64) NOT NULL,
    UNIQUE (model_id, cache_key, base_words)
);

CREATE INDEX synonyms_cache_idx_1 ON synonyms_cache(cache_key);
CREATE INDEX synonyms_cache_idx_2 ON synonyms_cache(main_cache_id);
CREATE INDEX synonyms_cache_idx_3 ON synonyms_cache(model_id);

--
-- Submit cache table.
--
DROP TABLE IF EXISTS submit_cache CASCADE;
CREATE TABLE submit_cache (
    id SERIAL PRIMARY KEY,
    main_cache_id BIGINT NOT NULL REFERENCES main_cache ON DELETE CASCADE,
    cache_key VARCHAR(5120) NOT NULL,
    sorted BOOL NOT NULL,
    model_id VARCHAR(64) NOT NULL,
    UNIQUE (model_id, cache_key)
);

CREATE INDEX submit_cache_idx_1 ON submit_cache(main_cache_id);
CREATE INDEX submit_cache_idx_2 ON submit_cache(model_id);

--
-- Input sentence history (log).
--
DROP TABLE IF EXISTS history CASCADE;
CREATE TABLE history (
    -- Common part.
    id SERIAL PRIMARY KEY,
    srv_req_id VARCHAR(64),
    orig_txt VARCHAR(1024), -- NLP server normalized text.
    user_id BIGINT,
    ds_id BIGINT,
    user_agent VARCHAR(1024),
    origin VARCHAR(16), -- 'web', 'test', 'dev', etc.
    rmt_addr VARCHAR(256),
    status VARCHAR(32), -- Current status of processing ('ASK_READY', 'ASK_WAIT_LINGUIST', etc.).
    -- In and out timestamps.
    recv_tstamp TIMESTAMP NOT NULL DEFAULT current_timestamp, -- Initial receive timestamp.
    resp_tstamp TIMESTAMP NULL, -- Result or error response timestamp.
    -- Feedback part.
    feedback_msg VARCHAR(1024) NULL,
    -- Linguist part.
    last_linguist_user_id BIGINT NULL, -- Last linguist user ID, if any.
    last_linguist_op VARCHAR(32) NULL, -- Last linguist operation ('curate', 'talkback' or 'reject'), if any.
    -- Curation part.
    curate_txt VARCHAR(1024) NULL, -- Last curate text, NULL if there was no curation.
    curate_hint VARCHAR(1024) NULL, -- Last curate hint, NULL if there was no curation or no hint.
    -- Result parts.
    res_type VARCHAR(32) NULL,
    res_body_gzip TEXT NULL, -- GZIP-ed result body.
    error TEXT NULL,
    cache_id BIGINT NULL,
    -- All-optional IP-based GEO location information (collected on the last user web sign in).
    tmz_name VARCHAR(64) NULL,
    tmz_abbr VARCHAR(8) NULL,
    latitude FLOAT NULL,
    longitude FLOAT NULL,
    city VARCHAR(64) NULL,
    country_name VARCHAR(64) NULL,
    country_code VARCHAR(8) NULL,
    region_name VARCHAR(64) NULL,
    region_code VARCHAR(8) NULL,
    zip_code VARCHAR(16) NULL,
    metro_code BIGINT NULL,
    -- All-optional probe information for this request.
    probe_token VARCHAR(256) NULL,
    probe_id VARCHAR(512) NULL,
    probe_guid VARCHAR(512) NULL,
    probe_api_version BIGINT NULL,
    probe_api_date TIMESTAMP NULL,
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
    probe_mac_addr VARCHAR(512) NULL,
    probe_email VARCHAR(512) NULL,

    is_test BOOL NOT NULL DEFAULT FALSE
);

CREATE INDEX history_idx_1 ON history(srv_req_id);
CREATE INDEX history_idx_2 ON history(user_id);
CREATE INDEX history_idx_3 ON history(origin);
CREATE INDEX history_idx_4 ON history(recv_tstamp);
CREATE INDEX history_idx_5 ON history(srv_req_id);

--
-- Input sentence suggestion.
--
DROP TABLE IF EXISTS suggestion CASCADE;
CREATE TABLE suggestion (
    -- Common suggestion.
    id SERIAL PRIMARY KEY,
    ds_id BIGINT,
    text VARCHAR(1024) NOT NULL,
    count INTEGER NOT NULL CHECK (count >= 0),
    search VARCHAR(1025) NOT NULL,
    direct BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL,
    last_modified_on TIMESTAMP NOT NULL DEFAULT current_timestamp
);

CREATE INDEX suggestion_idx_1 ON suggestion(count);
-- Note that GIS addon should be installed.
CREATE INDEX suggestion_idx_2 ON suggestion USING gin(search gin_trgm_ops);
CREATE UNIQUE INDEX suggestion_idx_3 ON suggestion (ds_id, text);
CREATE INDEX suggestion_idx_4 ON suggestion (direct);
CREATE INDEX suggestion_idx_5 ON suggestion (active);

--
-- Login/Logout history (log).
--
DROP TABLE IF EXISTS login_history CASCADE;
CREATE TABLE login_history (
    user_id BIGINT NOT NULL,
    user_email VARCHAR(64),
    act VARCHAR(32) NOT NULL, -- "LOGIN" or "LOGOUT"
    user_agent VARCHAR(1024),
    tstamp TIMESTAMP NOT NULL DEFAULT current_timestamp,
    rmt_addr VARCHAR(256)
);
