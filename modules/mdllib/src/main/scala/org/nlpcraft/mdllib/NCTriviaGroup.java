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
