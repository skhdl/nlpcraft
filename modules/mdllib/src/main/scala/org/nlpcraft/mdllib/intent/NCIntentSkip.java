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
 * Licensor:    Copyright (C) 2018 DataLingvo, Inc. https://www.datalingvo.com
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
