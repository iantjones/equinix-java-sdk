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

package api.equinix.javasdk.core.async;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

/**
 * A small, generic asynchronous facade over <em>any</em> Equinix SDK call, backed by Java 21
 * virtual threads. Rather than mirroring the entire API surface with {@code ...Async} variants, this
 * runs an arbitrary blocking SDK call on a virtual thread and hands the caller a
 * {@link CompletableFuture}, so a single small class covers Fabric, NetworkEdge, CustomerPortal and
 * every other domain.
 *
 * <p>Each submitted call runs on its own virtual thread from
 * {@link Executors#newVirtualThreadPerTaskExecutor()}. Virtual threads are cheap and park (rather
 * than pin a platform thread) while a request is blocked on I/O, so thousands of concurrent SDK
 * calls can be in flight without an explicit thread-pool size to tune.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * try (EquinixAsync async = EquinixAsync.create()) {
 *     CompletableFuture<Connection> f = async.call(() -> fabric.connections().getByUuid(id));
 *     CompletableFuture<Void> v = async.run(() -> fabric.connections().getByUuid(id).delete());
 *
 *     // Fan out, then join:
 *     List<CompletableFuture<Connection>> futures = ids.stream()
 *             .map(id -> async.call(() -> fabric.connections().getByUuid(id)))
 *             .toList();
 *     List<Connection> connections = EquinixAsync.awaitAll(futures);
 * }
 * }</pre>
 *
 * <p>Exceptions thrown by the supplied call (for example an
 * {@link api.equinix.javasdk.core.exception.EquinixServiceException} or any other
 * {@link RuntimeException}) are not swallowed: the returned future completes exceptionally with the
 * original throwable as its cause, surfaced through {@link CompletableFuture#join()} /
 * {@link CompletableFuture#get()} as usual.</p>
 *
 * <p>This class is {@link AutoCloseable}; closing it shuts down the backing executor. Calls
 * submitted after {@link #close()} are rejected. Instances are thread-safe and intended to be
 * shared, typically for the lifetime of the work being parallelised.</p>
 *
 * @author ianjones
 */
public final class EquinixAsync implements AutoCloseable {

    private final ExecutorService executor;

    private EquinixAsync(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Creates a new facade backed by a fresh virtual-thread-per-task executor.
     *
     * @return a new {@link EquinixAsync}; close it (ideally via try-with-resources) when done
     */
    public static EquinixAsync create() {
        return new EquinixAsync(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Runs a value-returning SDK call asynchronously on a virtual thread.
     *
     * <p>If {@code apiCall} throws, the returned future completes exceptionally with that throwable
     * as the cause; otherwise it completes with the supplied result (which may be {@code null}).</p>
     *
     * @param apiCall the blocking SDK call to run, e.g. {@code () -> fabric.connections().getByUuid(id)}
     * @param <T> the result type of the call
     * @return a future completed with the call's result, or completed exceptionally on error
     */
    public <T> CompletableFuture<T> call(Supplier<T> apiCall) {
        Objects.requireNonNull(apiCall, "apiCall must not be null");
        try {
            return CompletableFuture.supplyAsync(apiCall, executor);
        } catch (RejectedExecutionException ree) {
            // The facade has been closed (or the executor otherwise refuses work). Keep the contract
            // that failures always surface through the future rather than being thrown synchronously.
            return CompletableFuture.failedFuture(ree);
        }
    }

    /**
     * Runs a {@code void} SDK call (for example a delete or update) asynchronously on a virtual
     * thread.
     *
     * <p>If {@code apiCall} throws, the returned future completes exceptionally with that throwable
     * as the cause; otherwise it completes with {@code null} once the call returns.</p>
     *
     * @param apiCall the blocking SDK call to run, e.g. {@code () -> connection.delete()}
     * @return a future completed when the call returns, or completed exceptionally on error
     */
    public CompletableFuture<Void> run(Runnable apiCall) {
        Objects.requireNonNull(apiCall, "apiCall must not be null");
        try {
            return CompletableFuture.runAsync(apiCall, executor);
        } catch (RejectedExecutionException ree) {
            // See call(Supplier): keep submission failures inside the returned future.
            return CompletableFuture.failedFuture(ree);
        }
    }

    /**
     * Awaits a collection of futures and collects their results in iteration order. This is a
     * blocking convenience for the common "fan out, then join" pattern.
     *
     * <p>If any future completes exceptionally, the first such failure (in iteration order) is
     * rethrown as a {@link java.util.concurrent.CompletionException} whose cause is the original
     * throwable, exactly as {@link CompletableFuture#join()} would surface it. If the calling thread
     * is interrupted while waiting, an {@link AsyncException} is thrown and the interrupt flag is
     * restored.</p>
     *
     * @param futures the futures to await; not modified
     * @param <T> the result type of the futures
     * @return the results in the iteration order of {@code futures}
     */
    public static <T> List<T> awaitAll(Collection<? extends CompletableFuture<? extends T>> futures) {
        Objects.requireNonNull(futures, "futures must not be null");
        List<CompletableFuture<? extends T>> snapshot = new ArrayList<>(futures);
        List<T> results = new ArrayList<>(snapshot.size());
        try {
            for (CompletableFuture<? extends T> future : snapshot) {
                results.add(future.get());
            }
            return results;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new AsyncException("Interrupted while awaiting asynchronous Equinix calls.", ie);
        } catch (java.util.concurrent.ExecutionException ee) {
            // Surface the original failure the same way CompletableFuture#join() does.
            Throwable cause = ee.getCause();
            throw new java.util.concurrent.CompletionException(cause != null ? cause : ee);
        }
    }

    /**
     * Shuts down the backing virtual-thread executor. In-flight calls are allowed to finish; new
     * submissions via {@link #call} or {@link #run} after this point are rejected. Idempotent.
     */
    @Override
    public void close() {
        executor.shutdown();
    }
}
