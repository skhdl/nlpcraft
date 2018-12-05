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

package org.nlpcraft.probe.dev;

import org.nlpcraft.probe.*;
import org.nlpcraft.probe.mgrs.exit.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Convenient in-process probe launcher. This class should be used to conveniently start a probe
 * from user's code, e.g. unit tests, during development, debugging and testing of the models.
 * Note that only one probe can be stared per JVM process lifecycle.
 *
 * @see NCProbeConfig
 */
public class NCProbeDevApp {
    /** Probe exit code indicating error status. */
    static public int ERROR_EXIT = 0;

    /** Probe exit code indicating OK status. */
    static public int OK_EXIT = 1;

    /** Probe exit code indicating restart request. */
    static public int RESTART_EXIT = 2;

    static private final Object mux = new Object();

    static private boolean used = false;

    static private final AtomicBoolean done = new AtomicBoolean(false);

    static private int code = Integer.MIN_VALUE;

    static private Throwable asyncEx = null;

    /**
     * Starts in-process probe with given configuration. This method will
     * block until probe exits.
     *
     * @param cfg Configuration to start probe with.
     * @return Probe's exit code.
     *
     * @see #ERROR_EXIT
     * @see #OK_EXIT
     * @see #RESTART_EXIT
     */
    public static int start(NCProbeConfig cfg) {
        synchronized (mux) {
            if (used)
                throw new IllegalStateException("Probe can only be started once in JVM process.");

            used = true;

            code = NCProbeRunner.startProbe(cfg);

            if (code == RESTART_EXIT)
                throw new IllegalStateException("Restarts are not supported by in-process probe runner.");

            return code;
        }
    }

    /**
     * Asynchronously starts probe with given configuration. This method will
     * NOT block and will return immediately. Caller should use returned future to observe
     * probe's lifecycle. Cancelling returned future will stop the probe. The future will
     * be completed with probe's exit code when probe exits.
     *
     * @param cfg Configuration to start probe with.
     * @return A future that will be completed with probe's exit code when probe exits.
     * @see #ERROR_EXIT
     * @see #OK_EXIT
     * @see #RESTART_EXIT
     */
    public static Future<Integer> startAsync(NCProbeConfig cfg) {
        new Thread(() -> {
            try {
                code = start(cfg);
            }
            catch (Throwable e) {
                asyncEx = e;
            }
            finally {
                synchronized (done) {
                    done.set(true);

                    done.notifyAll();
                }
            }
        }).start();

        return new Future<Integer>() {
            private volatile boolean cancelled = false;

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                NCExitManager.exit();
                
                cancelled = true;

                return true;
            }

            @Override
            public boolean isCancelled() {
                return cancelled;
            }

            @Override
            public boolean isDone() {
                synchronized (done) {
                    return done.get();
                }
            }

            /**
             * 
             * @return
             * @throws ExecutionException
             */
            private int handleDone() throws ExecutionException {
                if (asyncEx != null)
                    throw new ExecutionException(asyncEx);
                else
                    return code;
            }

            @Override
            public Integer get() throws InterruptedException, ExecutionException {
                synchronized (done) {
                    while (!done.get())
                        done.wait();

                    return handleDone();
                }
            }

            @Override
            public Integer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                synchronized (done) {
                    if (!done.get()) {
                        done.wait(unit.toMillis(timeout), 0);

                        if (done.get())
                            return handleDone();
                        else
                            throw new TimeoutException();
                    }

                    return handleDone();
                }
            }
        };
    }
}
