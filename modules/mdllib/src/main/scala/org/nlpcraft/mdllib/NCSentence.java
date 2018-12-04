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

package org.nlpcraft.mdllib;

import org.nlpcraft.mdllib.intent.*;
import java.io.*;
import java.util.*;

/**
 * Fully parsed representation of the user input.
 * <br><br>
 * Note that in general a given user input can have multiple {@link #variants() variants} of how it can be parsed
 * in isolation. These variants (always at least one) are available through {@link #variants()} method. Additional
 * mechanisms, like {@link NCIntentSolver intent-based matching}, utilize the external context (i.e. intents)
 * that simplify the processing and selection among multiple variants.
 * <br><br>
 * Sentence provides access to parsing {@link #variants() variants} representing parsed input sentence as
 * well as environment metadata such as user and caller information. Instance of {@link NCSentence} interface
 * is available through {@link NCQueryContext} object that is passed to {@link NCModel#query(NCQueryContext)}
 * method.
 * 
 * @see NCToken
 * @see NCIntentSolver
 */
public interface NCSentence extends Serializable {
    /**
     * Gets globally unique server ID of the current request.
     * <br><br>
     * Server request is defined as a processing of a one user input sentence (a session).
     * Note that model can be accessed multiple times during processing of a single user sentence
     * and therefor multiple instances of this interface can return the same server
     * request ID. In fact, users of this interfaces can use this fact by using this ID,
     * for example, as a map key for a session scoped storage.
     *
     * @return Server request ID.
     */
    String getServerRequestId();

    /**
     * Gets normalized text of the user input.
     *
     * @return Normalized text of the user input.
     */
    String getNormalizedText();

    /**
     * Gets string representation of the user client agent that made the call with
     * this sentence.
     *
     * @return User agent string from user client (web browser, REST client, etc.).
     */
    String getUserClientAgent();

    /**
     * Gets formal string code that defines the origin of the request.
     *
     * @return A string defining the origin of the request ('web', 'rest', etc.).
     */
    String getOrigin();

    /**
     * Gets local timestamp in msec when user input was received.
     *
     * @return Local timestamp in msec when user input was received.
     */
    long getReceiveTimestamp();

    /**
     * Gets optional address of the remote client.
     *
     * @return Optional address of the remote client.
     */
    Optional<String> getRemoteAddress();

    /**
     * Gets first name of the user that made the request.
     *
     * @return First name of the user that made the request.
     */
    String getUserFirstName();

    /**
     * Gets last name of the user that made the request.
     *
     * @return Last name of the user that made the request.
     */
    String getUserLastName();

    /**
     * Gets email of the user that made the request.
     *
     * @return Email of the user that made the request.
     */
    String getUserEmail();

    /**
     * Gets user company name that made the request.
     *
     * @return User company name that made the request.
     */
    String getUserCompany();

    /**
     * Gets user avatar URL ({@code data:} or {@code http:} scheme URLs).
     *
     * @return User avatar URL ({@code data:} or {@code http:} scheme URLs).
     */
    String getUserAvatarUrl();

    /**
     * Tests whether or not the user has administrative privileges.
     *
     * @return Whether or not the user has administrative privileges.
     */
    boolean isUserAdmin();

    /**
     * Gets signup date of the user that made the request.
     *
     * @return Signup date of the user that made the request.
     */
    long getUserSignupDate();

    /**
     * Gets timestamp of the last request issued by this user.
     *
     * @return Timestamp of the last request issued by this user, or {@code 0} (zero) if this
     *      is the first request.
     */
    long getUserLastQTimestamp();

    /**
     * Gets total number of questions issues by this user.
     *
     * @return Total number of questions issues by this user.
     */
    int getUserTotalQs();

    /**
     * Gets optional timezone name of the location from where user made its request.
     *
     * @return Timezone name of the location from where user made its request.
     */
    Optional<String> getTimezoneName();

    /**
     * Gets optional timezone abbreviation of the location from where user made its request.
     *
     * @return Timezone abbreviation of the location from where user made its request.
     */
    Optional<String> getTimezoneAbbreviation();

    /**
     * Gets optional latitude of the location from where user made its request.
     *
     * @return Latitude of the location from where user made its request.
     */
    Optional<Double> getLatitude();

    /**
     * Gets optional longitude of the location from where user made its request.
     *
     * @return Longitude of the location from where user made its request.
     */
    Optional<Double> getLongitude();

    /**
     * Gets optional country code (<a href="https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2">ISO 3166-1 alpha-2</a>) of the
     * location from where user made its request.
     *
     * @return Country code (<a href="https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2">ISO 3166-1 alpha-2</a>) of
     *      the location from where user made its request.
     */
    Optional<String> getCountryCode();

    /**
     * Gets optional country name of the location from where user made its request.
     *
     * @return Country name of the location from where user made its request.
     */
    Optional<String> getCountryName();

    /**
     * Gets optional region (state, province) name of the location from where user made its request.
     *
     * @return Region (state, province) name of the location from where user made its request.
     */
    Optional<String> getRegionName();

    /**
     * Gets optional city name of the location from where user made its request.
     *
     * @return City name of the location from where user made its request.
     */
    Optional<String> getCityName();

    /**
     * Gets optional zip or postal code of the location from where user made its request.
     *
     * @return Zip or postal code of the location from where user made its request.
     */
    Optional<String> getZipCode();

    /**
     * Gets optional metro (area) code of the location from where user made its request.
     *
     * @return Metro (area) code of the location from where user made its request.
     */
    Optional<java.lang.Long> getMetroCode();

    /**
     * Tests if given token is part of this sentence.
     *
     * @param tok Token to check.
     * @return {@code true} if given token is from this sentence, {@code false} otherwise.
     */
    boolean isOwnerOf(NCToken tok);

    /**
     * Gets the list of all parsing variants for this sentence.
     *
     * @return All parsing variants of this sentence. Always contains at least one variant.
     */
    List<NCVariant> variants();
}