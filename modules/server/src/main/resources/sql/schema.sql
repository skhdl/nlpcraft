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
-- Pool of password hashes.
--
DROP TABLE IF EXISTS passwd_pool CASCADE;
CREATE TABLE passwd_pool (
    passwd_hash VARCHAR(64) NOT NULL
);

--
-- Company table.
--
DROP TABLE IF EXISTS company CASCADE;
CREATE TABLE company (
    -- Inherit columns from base entity.
    LIKE base INCLUDING DEFAULTS,

    id SERIAL PRIMARY KEY,

    sign_up_domain VARCHAR(256) NOT NULL, -- Must be unique.
    name VARCHAR(64),
    probe_token VARCHAR(256) NOT NULL,
    probe_token_hash VARCHAR(64) NOT NULL
);

CREATE INDEX company_idx_1 ON company(sign_up_domain);

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
    email VARCHAR(64) NOT NULL, -- Used as username during login.
    avatar_url TEXT, -- URL or encoding of avatar for this user, if any.
    first_name VARCHAR(64) NOT NULL,
    last_name VARCHAR(64) NOT NULL,
    last_ds_id BIGINT REFERENCES company, -- Company it belongs to.
    company_id BIGINT REFERENCES company, -- Company it belongs to.
    passwd_salt VARCHAR(64) NOT NULL
);

CREATE INDEX company_user_idx_1 ON company_user(email);
CREATE INDEX company_user_idx_2 ON company_user(company_id);
CREATE INDEX company_user_idx_3 ON company_user(last_ds_id);

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
    user_id BIGINT NOT NULL REFERENCES company_user, -- User that created that data source.
    enabled BOOL NOT NULL,
    model_id VARCHAR(32) NOT NULL,
    model_name VARCHAR(64) NOT NULL,
    model_ver VARCHAR(16) NOT NULL,
    model_cfg VARCHAR(5120) NOT NULL -- Empty if configuration is not used.
);

CREATE INDEX ds_instance_idx_1 ON ds_instance(user_id);
CREATE INDEX ds_instance_idx_2 ON ds_instance(enabled);

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
