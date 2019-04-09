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

package org.nlpcraft.model.tools.dump;

import org.nlpcraft.common.*;
import org.nlpcraft.model.*;
import org.nlpcraft.model.intent.*;
import org.nlpcraft.model.tools.dump.scala.*;

/**
 * Data model dump writer.
 * <br><br>
 * Data model dump allows to export the model and intent configuration sans the callback implementations. Data
 * model dumps can be used to safely test model's intent-based matching logic by a 3-rd party.
 *
 * @see NCDumpReader
 */
public class NCDumpWriter {
    /**
     * Writes data model dump file into specified directory. Dump file will only contain static model configuration
     * and intent descriptors. It will not serialize any code logic.
     *
     * @param mdl Model to dump.
     * @param solver Intent solver to dump.
     * @param dirPath Directory path where dump file will be created.
     * @return Name of the created file. File name contains model ID and the timestamp.
     * @throws NCException Thrown in case of any errors.
     */
    public static String write(NCModel mdl, NCIntentSolver solver, String dirPath) throws NCException {
        return NCDumpWriterScala.write(mdl, solver, dirPath);
    }
}
