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

import org.nlpcraft.common.NCException;
import org.nlpcraft.model.NCModel;
import org.nlpcraft.model.tools.dump.scala.NCDumpReaderScala;
import java.io.File;

/**
 * Data model dump reader.
 * <br><br>
 * Data model dump allows to export the model and intent configuration sans the callback implementations. Data
 * model dumps can be used to safely test model's intent-based matching logic by a 3-rd party.
 * 
 * @see NCDumpWriter
 */
public class NCDumpReader {
    /**
     * Reads the data model dump and creates data model proxy.
     *
     * @param filePath Data model dump file path to read.
     * @return Data model proxy. Proxy will have a no-op callback implementations for intent and will return the
     *      following JSON response:
     * <pre class="brush: js">
     * {
     *     "modelId": "model-id",
     *     "intentId": "intent-id",
     *     "modelFile": "model-id-01:01:01:123.gz"
     *  }
     * </pre>
     * @throws NCException Thrown in case of any errors.
     */
    public static NCModel read(String filePath) throws NCException {
        return NCDumpReaderScala.read(filePath);
    }
    
    /**
     * Reads the data model dump and creates data model proxy.
     *
     * @param file Data model dump file to read.
     * @return Data model proxy. Proxy will have a no-op callback implementations for intent and will return the
     *      following JSON response:
     * <pre class="brush: js">
     * {
     *     "modelId": "model-id",
     *     "intentId": "intent-id",
     *     "modelFile": "model-id-01:01:01:123.gz"
     *  }
     * </pre>
     * @throws NCException Thrown in case of any errors.
     */
    public static NCModel read(File file) throws NCException {
        return NCDumpReaderScala.read(file);
    }
}
