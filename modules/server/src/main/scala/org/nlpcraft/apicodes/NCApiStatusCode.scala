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

package org.nlpcraft.apicodes

import org.nlpcraft.NCE

/**
 * Enumeration for all APIs status codes.
 */
object NCApiStatusCode extends Enumeration {
    type DLApiStatusCode = Value

    // General API codes.
    val API_SYS_FAILURE: Value = Value
    val API_AUTH_FAILED: Value = Value
    val API_ADMIN_REQUIRED: Value = Value
    val API_ROOT_REQUIRED: Value = Value
    val API_OFFLINE: Value = Value
    val API_NOT_SUPPORTED_DEMO: Value = Value // Operation not supported for demo account.
    val API_FIELD_INVALID: Value = Value
    val API_FIELD_MISSED: Value = Value

    // Public API codes.
    val PUB_API_OK: Value = Value

    // Usage limitation failures.
    val USAGE_LIMIT_VIOLATION: Value = Value

    // Query has been registered and about to begin processing.
    val ASK_WAIT_REGISTERED: Value = Value
    // Query is dispatched for human curation.
    val ASK_WAIT_LINGUIST: Value = Value
    // Final result (positive or negative) is ready.
    val ASK_READY: Value = Value

    val ASK_CANCEL_OK: Value = Value
    val ASK_CHECK_OK: Value = Value
    val ASK_RESULT_OK: Value = Value

    // Linguist status codes.
    val LINGUIST_OK: Value = Value

    val CLEAR_CONV_CTX_OK: Value = Value

    // Dereferencing
    val DEREF_OK: Value = Value

    // Client-side models.
    val REST_GEO_MODEL_OK: Value = Value

    // Suggesting popular questions.
    val REST_SUGGEST_OK: Value = Value

    // User server codes.
    val USER_QUESTIONS_OK: Value = Value
    val USER_GET_ALL_OK: Value = Value
    val USER_GET_OK: Value = Value
    val USER_CHECK_DELETE_YES: Value = Value
    val USER_CHECK_DELETE_NO: Value = Value
    val USER_UPDATE_OK: Value = Value
    val USER_EMAIL_UNIQUE: Value = Value
    val USER_EMAIL_DUP: Value = Value
    val USER_ADD_OK: Value = Value
    val USER_SET_PASSWD_OK: Value = Value
    val USER_CLEAR_PASSWD_OK: Value = Value
    val USER_PASSWD_RESET_ON: Value = Value
    val USER_PASSWD_RESET_OFF: Value = Value
    val USER_AVATAR_OK: Value = Value
    val USER_AVATAR_NOT_FOUND: Value = Value
    val USER_DS_LINK_OK: Value = Value
    val USER_DS_ALL_OK: Value = Value

    // Company server codes.
    val COMPANY_GET_ALL_OK: Value = Value
    val COMPANY_DELETE_OK: Value = Value
    val COMPANY_UPDATE_OK: Value = Value
    val COMPANY_GET_OK: Value = Value
    val COMPANY_RESET_TOKEN_OK: Value = Value

    // Fail '/login/xxx' codes.
    val LOGIN_FAILED_EMAIL: Value = Value
    val LOGIN_FAILED_AUTH: Value = Value
    val LOGIN_FAILED_NOT_ACTIVE: Value = Value
    val LOGIN_FAILED_CHANGE_PASSWORD: Value = Value
    val LOGIN_FAILED_SYS: Value = Value

    // OK '/login/xxx' codes.
    val LOGIN_OK: Value = Value

    // Fail '/logout' codes.
    val LOGOUT_FAILED: Value = Value

    // OK '/logout' codes.
    val LOGOUT_OK: Value = Value

    // Invites codes.
    val INVITE_ERR_DUP_EMAIL: Value = Value
    val INVITE_ERR_DOMAIN_UNKNOWN: Value = Value
    val INVITE_ERR_INVALID_EMAIL: Value = Value
    val INVITE_REVOKE_OK: Value = Value
    val INVITE_REMIND_OK: Value = Value
    val INVITE_OK: Value = Value
    val INVITE_ACTIVATE_OK: Value = Value
    val INVITE_DELETE_OK: Value = Value
    val INVITE_GET_ALL_OK: Value = Value

    // Constants codes
    val CONST_DATA_OK: Value = Value

    // Data source codes.
    val DS_ALL_OK: Value = Value
    val DS_ADD_OK: Value = Value
    val DS_DUP_NAME: Value = Value
    val DS_SET_ACTIVE_OK: Value = Value
    val DS_SET_ACTIVE_FAILED: Value = Value
    val DS_DELETE_OK: Value = Value
    val DS_UPDATE_OK: Value = Value
    val DS_ENABLE_OK: Value = Value

    // Probes codes.
    val PROBE_ALL_OK: Value = Value
    val PROBE_ALL_RELEASES_OK: Value = Value
    val PROBE_MODELS_OK: Value = Value
    val PROBE_MODEL_USAGE_OK: Value = Value
    val PROBE_RESTART_OK: Value = Value
    val PROBE_STOP_OK: Value = Value

    // Signup codes.
    val SIGNUP_TOKEN_UNKNOWN: Value = Value
    val SIGNUP_TOKEN_OK: Value = Value
    val SIGNUP_TOKEN_USED: Value = Value
    val SIGNUP_USER_EMAIL_EXISTS: Value = Value
    val SIGNUP_INIT_CRED_OK: Value = Value
    val SIGNUP_DOMAIN_KNOWN: Value = Value
    val SIGNUP_DOMAIN_UNKNOWN: Value = Value
    val SIGNUP_DOMAIN_CHECK_OK: Value = Value
    val SIGNUP_DOMAIN_VIOLATION: Value = Value
    val SIGNUP_NO_DS: Value = Value
    val SIGNUP_ADMIN_INVITE_REQUIRED: Value = Value
    val SIGNUP_NO_DEFAULT_ROLE: Value = Value
    val SIGNUP_USER_KNOWN: Value = Value
    val SIGNUP_USER_TOKEN_EMAIL_MISMATCH: Value = Value
    val SIGNUP_USER_TOKEN_EMAIL_MATCH: Value = Value
    val SIGNUP_USER_UNKNOWN: Value = Value
    val SIGNUP_VERIFY_CODE_SENT: Value = Value
    val SIGNUP_VERIFY_CODE_OK: Value = Value
    val SIGNUP_VERIFY_CODE_INVALID: Value = Value
    val SIGNUP_OK: Value = Value

    // Password reset codes.
    val PASSWORD_RESET_EMAIL_KNOWN: Value = Value
    val PASSWORD_RESET_EMAIL_UNKNOWN: Value = Value
    val PASSWORD_RESET_VERIFY_CODE_SENT: Value = Value
    val PASSWORD_RESET_VERIFY_CODE_INVALID: Value = Value
    val PASSWORD_RESET_OK: Value = Value

    // Dashboard codes.
    val DASHBOARD_OK: Value = Value
    val DASHBOARD_DUP: Value = Value
    val DASHBOARD_ACK_OK: Value = Value
    val DASHBOARD_FAILED: Value = Value

    // CSV codes.
    val CSV_OK: Value = Value
    val CSV_AUTH_FAILED: Value = Value

    // OK '/feedback' codes.
    val FEEDBACK_OK: Value = Value

    // No-op value.
    val NOOP: Value = Value

    /**
     * Gets enum value for given string name with proper error handling.
     *
     * @param name Enumeration's name.
     */
    def byName(name: String): DLApiStatusCode = {
        try {
            NCApiStatusCode.withName(name)
        }
        catch {
            case _: NoSuchElementException ⇒ throw new NCE(s"Unknown API status code: $name")
        }
    }
    
    implicit def toString0: String = toString()
}
