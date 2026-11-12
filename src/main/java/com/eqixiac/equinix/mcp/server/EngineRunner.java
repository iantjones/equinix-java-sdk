/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.mcp.server;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs a long, read-only engine call (an optimization, a resilience simulation, an estate scan) so that
 * a client {@code notifications/cancelled} ends the tool call promptly.
 *
 * <p>The engines have no cancellation hooks of their own, so the body runs on a worker thread while the
 * calling thread waits on the call's {@link CancellationSignal}. On cancel the worker is interrupted
 * and abandoned and the call fails with {@link ToolCallCancelledException}. Abandoning is safe because
 * every body run here only reads: the worker can at most finish a read nobody waits for. Live pricing
 * lookups made by the worker also stop at once, because a cancelled call's {@link PricingBudget} has no
 * time left.</p>
 *
 * <p>The worker does not see the call's thread-bound state ({@link ServerContext#currentExchange()},
 * {@link ServerContext#pricingBudget()}): read those on the calling thread and pass them in.</p>
 */
final class EngineRunner {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "mcp-design-engine-" + SEQUENCE.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });

    /** How often the waiting thread re-checks the signal, in ms (the cancel listener usually wins). */
    private static final long POLL_MILLIS = 100;

    private EngineRunner() {
    }

    /**
     * Runs {@code body} and returns its result, unless the call is cancelled first.
     *
     * @param what         what is running, for the cancellation message (e.g. "the optimization")
     * @param cancellation the call's signal
     * @param body         the read-only work
     * @param <T>          the result type
     * @return the result
     * @throws ToolCallCancelledException when the client cancelled the call
     * @throws Exception                  whatever the body threw
     */
    static <T> T run(String what, CancellationSignal cancellation, Callable<T> body) throws Exception {
        cancellation.throwIfCancelled("before " + what + " started");
        Future<T> future = EXECUTOR.submit(body);
        cancellation.onCancel(() -> future.cancel(true));
        while (true) {
            try {
                return future.get(POLL_MILLIS, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                if (cancellation.isCancelled()) {
                    future.cancel(true);
                    throw new ToolCallCancelledException("while " + what + " was running (read-only work, "
                            + "abandoned)", cancellation.reason());
                }
            } catch (CancellationException e) {
                throw new ToolCallCancelledException("while " + what + " was running (read-only work, abandoned)",
                        cancellation.reason());
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                if (cause instanceof Exception exception) {
                    throw exception;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw new IllegalStateException(cause);
            } catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                throw new ToolCallCancelledException("while " + what + " was running (interrupted)", null);
            }
        }
    }
}
