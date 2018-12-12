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