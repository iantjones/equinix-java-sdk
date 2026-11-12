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

import java.util.ArrayList;
import java.util.List;

/**
 * The cancellation state of one MCP tool call.
 *
 * <p>The client cancels an in-flight request with a {@code notifications/cancelled} notification
 * naming the request id. The MCP Java SDK (2.1.0) registers no handler for that notification, so
 * this server reads it off the stdio input itself ({@link StdioCancellationInterceptor}) and cancels
 * the signal of the matching {@code tools/call}. A handler observes the signal through
 * {@link ToolHandler#handle(com.fasterxml.jackson.databind.JsonNode, ServerContext, CancellationSignal)}
 * and through {@link ServerContext#currentCancellation()}.</p>
 *
 * <h2>Point of no return</h2>
 * <p>A handler that sends an irreversible request (the Safe Mutation Broker's create) calls
 * {@link #commit()} immediately before sending it. {@code commit()} and {@link #cancel(String)}
 * are mutually ordered: when {@code commit()} returns {@code true}, a later cancel is recorded
 * ({@link #cancelRequestedAfterCommit()}) but the handler finishes and reports what it executed;
 * when it returns {@code false}, the call was already cancelled and nothing may be sent.</p>
 *
 * <p>Thread-safe. Listeners registered with {@link #onCancel(Runnable)} run once, on the thread
 * that cancels (or immediately when the signal is already cancelled).</p>
 */
public final class CancellationSignal {

    private final boolean cancellable;
    private final List<Runnable> listeners = new ArrayList<>();
    private boolean cancelled;
    private boolean committed;
    private boolean cancelledAfterCommit;
    private String reason;

    private CancellationSignal(boolean cancellable) {
        this.cancellable = cancellable;
    }

    /**
     * @return a fresh signal that a client {@code notifications/cancelled} (or {@link #cancel}) can trip
     */
    public static CancellationSignal create() {
        return new CancellationSignal(true);
    }

    /**
     * @return a signal that is never cancelled: for direct handler invocations outside a served call
     */
    public static CancellationSignal none() {
        return new CancellationSignal(false);
    }

    /**
     * Cancels the call. Has no effect on a {@link #none()} signal or a signal that is already
     * cancelled. After {@link #commit()} the cancel is only recorded.
     *
     * @param why the client's reason, or {@code null}
     */
    public void cancel(String why) {
        List<Runnable> toRun;
        synchronized (this) {
            if (!cancellable || cancelled || cancelledAfterCommit) {
                return;
            }
            reason = why;
            if (committed) {
                cancelledAfterCommit = true;
                return;
            }
            cancelled = true;
            toRun = new ArrayList<>(listeners);
            listeners.clear();
        }
        for (Runnable listener : toRun) {
            try {
                listener.run();
            }
            catch (RuntimeException ignored) {
                // A listener failure must not stop the others.
            }
        }
    }

    /**
     * @return {@code true} once the call was cancelled before any irreversible step
     */
    public synchronized boolean isCancelled() {
        return cancelled;
    }

    /**
     * @return the reason the client gave, or {@code null}
     */
    public synchronized String reason() {
        return reason;
    }

    /**
     * Marks the point of no return.
     *
     * @return {@code true} when the handler may send its irreversible request; {@code false} when
     *         the call was cancelled first (send nothing)
     */
    public synchronized boolean commit() {
        if (cancelled) {
            return false;
        }
        committed = true;
        return true;
    }

    /**
     * @return {@code true} when a cancel arrived after {@link #commit()}: the irreversible request
     *         was already sent and the result must say so
     */
    public synchronized boolean cancelRequestedAfterCommit() {
        return cancelledAfterCommit;
    }

    /**
     * Registers a listener that runs when the call is cancelled before commit; runs it at once when
     * already cancelled.
     *
     * @param listener the action, for example cancelling a pending prompt
     */
    public void onCancel(Runnable listener) {
        boolean runNow;
        synchronized (this) {
            runNow = cancelled;
            if (!runNow) {
                listeners.add(listener);
            }
        }
        if (runNow) {
            listener.run();
        }
    }

    /**
     * Throws {@link ToolCallCancelledException} when the call was cancelled.
     *
     * @param stage what was about to happen, for the message (for example "before the dry run")
     */
    public void throwIfCancelled(String stage) {
        if (isCancelled()) {
            throw new ToolCallCancelledException(stage, reason());
        }
    }
}
