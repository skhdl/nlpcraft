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

package org.nlpcraft.mdllib.intent;

/**
 * Control flow exception to skip current intent. This exception can be thrown by the intent
 * {@link NCIntentSolver.IntentCallback callback} to indicate that current intent should be skipped (even though
 * it was matched and its callback was called). If there's more than one intent matched the next best matching intent
 * will be selected and its callback will be called.
 * <br><br>
 * This exception becomes useful when it is hard or impossible to encode the entire matching logic using just
 * declarative intent DSL. In these cases the intent definition can be relaxed and the "last mile" of intent
 * matching can happen inside of the intent callback's user logic. If it is determined that intent in fact does
 * not match throwing this exception allows to try next best matching intent, if any.
 */
public class NCIntentSkip extends RuntimeException {
    /**
     * Creates new intent skip exception.
     */
    public NCIntentSkip() {
        // No-op.
    }

    /**
     * Creates new intent skip exception with given debug message.
     * 
     * @param msg Skip message for debug output.
     */
    public NCIntentSkip(String msg) {
        super(msg);
    }
}
