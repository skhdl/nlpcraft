/*
 * “Commons Clause” License, https://commonsclause.com/
 *
 * The Software is provided to you by the Licensor under the License,
 * as defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software:    NLPCraft
 * License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
 * Licensor:    Copyright (C) 2018 DataLingvo, Inc. https://www.datalingvo.com
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
 * Note that in general a given user input can have multiple {@link #getVariants() variants} of how it can be parsed
 * in isolation. These variants (always at least one) are available through {@link #getVariants()} method. Additional
 * mechanisms, like {@link NCIntentSolver intent-based matching}, utilize the external context (i.e. intents)
 * that simplify the processing and selection among multiple variants.
 * <br><br>
 * Instance of {@link NCSentence} interface is available through {@link NCQueryContext} object that is
 * passed to {@link NCModel#query(NCQueryContext)} method.
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
     * Gets UTC/GMT timestamp in ms when user input was received.
     *
     * @return UTC/GMT timestamp in ms when user input was received.
     */
    long getReceiveTimestamp();

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
    List<NCVariant> getVariants();
    
    /**
     * Gets optional address of the remote client.
     *
     * @return Optional address of the remote client.
     */
    Optional<String> getRemoteAddress();
    
    /**
     * Gets string representation of the user client agent that made the call with
     * this sentence.
     *
     * @return User agent string from user client (web browser, REST client, etc.).
     */
    Optional<String> getUserClientAgent();
}