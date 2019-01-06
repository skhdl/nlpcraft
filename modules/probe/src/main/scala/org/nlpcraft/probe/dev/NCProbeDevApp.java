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
