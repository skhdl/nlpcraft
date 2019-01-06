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
 * Software:    NlpCraft
 * License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
 * Licensor:    DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.mdllib;

import org.nlpcraft.mdllib.tools.builder.*;
import java.io.*;
import java.util.*;

/**
 * Group of trivia user inputs and corresponding randomly chosen responses.
 * <p>
 * Trivia is an automatic answer for some simple, common, short sentences like {@code hi, bye, hello}. Note
 * that NlpCraft comes with default set of trivia (see {@link NCModelBuilder#loadDefaultTrivia()}).
 * 
 * @see NCModel#getTrivia()
 */
public interface NCTriviaGroup extends Serializable {
    /**
     * Collection of trivia user inputs for this group. Macros are supported. Note that
     * inputs will be matched on their stemmatized form. See {@link NCElement} for more information
     * on macros support.
     *
     * @return Trivia inputs for this group.
     */
    Collection<String> getInputs();

    /**
     * Collection of trivia responses for {@link #getInputs()}. Responses will be randomly chosen.
     * Macros are supported. See {@link NCElement} for more information on macros support.
     * Although minimal HTML markup is supported it will only be rendered
     * by the webapp or by compatible user REST applications. Other client devices like voice-based
     * assistants may not support that. For cross-platform compatibility it is recommended to stick
     * with a simple text.
     *
     * @return Collection of trivia responses.
     */
    Collection<String> getResponses();
}
