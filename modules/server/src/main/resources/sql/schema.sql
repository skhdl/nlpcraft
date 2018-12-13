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
    avatar_url TEXT, -- URL or encoding of avatar for this user, if any.
    first_name VARCHAR(64) NOT NULL,
    last_name VARCHAR(64) NOT NULL,
    email VARCHAR(64) NOT NULL, -- Used as username during login.
    company_id BIGINT REFERENCES company, -- Company it belongs to.
    department VARCHAR(64), -- Company department or group (sales, marketing, etc.).
    title VARCHAR(64),
    passwd_salt VARCHAR(64), -- Optional salt for password hash (if password signup was used).
    phone VARCHAR(64),
    prefs_json TEXT NOT NULL -- JSON object containing user preferences.
);

CREATE INDEX company_user_idx_1 ON company_user(origin);
CREATE INDEX company_user_idx_2 ON company_user(email);
CREATE INDEX company_user_idx_3 ON company_user(company_id);
CREATE INDEX company_user_idx_6 ON company_user(active_ds_id);

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
