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

/**
 * Contains model testing framework.
 * <br><br>
 * Here's an code snippet from <code>Alarm Clock</code> example illustrating
 * the usage of test framework together with JUnit 5:
 * <pre class="brush: java, highlight: [6, 8, 13, 19, 22, 25, 26, 27]">
 * public class AlarmTest {
 *     private NCTestClient client;
 *
 *     &#64;BeforeEach
 *     void setUp() throws NCException, IOException {
 *         client = new NCTestClientBuilder().newBuilder().build();
 *
 *         client.open("nlpcraft.alarm.ex");
 *     }
 *
 *     &#64;AfterEach
 *     void tearDown() throws NCException, IOException {
 *         client.close();
 *     }
 *
 *     &#64;Test
 *     public void test() throws NCException, IOException {
 *         // Empty parameter.
 *         assertTrue(client.ask("").isFailed());
 *
 *         // Only latin charset is supported.
 *         assertTrue(client.ask("El tiempo en España").isFailed());
 *
 *         // Should be passed.
 *         assertTrue(client.ask("Ping me in 3 minutes").isSuccessful());
 *         assertTrue(client.ask("Buzz me in an hour and 15mins").isSuccessful());
 *         assertTrue(client.ask("Set my alarm for 30s").isSuccessful());
 *     }
 * }
 * </pre>
 */
package org.nlpcraft.model.test;