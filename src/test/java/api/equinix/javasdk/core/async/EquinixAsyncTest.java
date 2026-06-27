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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain unit tests for {@link EquinixAsync}. These exercise the facade with ordinary suppliers and
 * runnables (no real HTTP), so they run in the default profile without any WireMock harness.
 */
class EquinixAsyncTest {

    @Test
    void callReturnsTheSuppliedValue() throws Exception {
        try (EquinixAsync async = EquinixAsync.create()) {
            CompletableFuture<String> future = async.call(() -> "PROVISIONED");

            assertEquals("PROVISIONED", future.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void callRunsOnAVirtualThread() throws Exception {
        try (EquinixAsync async = EquinixAsync.create()) {
            CompletableFuture<Boolean> future = async.call(() -> Thread.currentThread().isVirtual());

            assertTrue(future.get(5, TimeUnit.SECONDS), "call() should execute on a virtual thread");
        }
    }

    @Test
    void callCompletesExceptionallyWithOriginalExceptionAsCause() {
        IllegalStateException boom = new IllegalStateException("boom");

        try (EquinixAsync async = EquinixAsync.create()) {
            CompletableFuture<String> future = async.call(() -> {
                throw boom;
            });

            ExecutionException ex = assertThrows(ExecutionException.class,
                    () -> future.get(5, TimeUnit.SECONDS));
            assertSame(boom, ex.getCause(), "the original throwable must be preserved as the cause");
            assertTrue(future.isCompletedExceptionally());
        }
    }

    @Test
    void runCompletesAndExecutesTheSideEffect() throws Exception {
        AtomicBoolean ran = new AtomicBoolean(false);

        try (EquinixAsync async = EquinixAsync.create()) {
            CompletableFuture<Void> future = async.run(() -> ran.set(true));

            assertNull(future.get(5, TimeUnit.SECONDS));
            assertTrue(ran.get(), "the runnable side effect must have executed");
        }
    }

    @Test
    void runCompletesExceptionallyWithOriginalExceptionAsCause() {
        RuntimeException boom = new RuntimeException("delete failed");

        try (EquinixAsync async = EquinixAsync.create()) {
            CompletableFuture<Void> future = async.run(() -> {
                throw boom;
            });

            ExecutionException ex = assertThrows(ExecutionException.class,
                    () -> future.get(5, TimeUnit.SECONDS));
            assertSame(boom, ex.getCause());
        }
    }

    @Test
    void awaitAllCollectsResultsInOrder() {
        try (EquinixAsync async = EquinixAsync.create()) {
            AtomicInteger counter = new AtomicInteger();
            List<CompletableFuture<Integer>> futures = List.of(
                    async.call(counter::incrementAndGet),
                    async.call(counter::incrementAndGet),
                    async.call(counter::incrementAndGet));

            List<Integer> results = EquinixAsync.awaitAll(futures);

            assertEquals(3, results.size());
            // Order is the iteration order of the input collection, not completion order.
            assertEquals(List.of(results.get(0), results.get(1), results.get(2)), results);
            assertEquals(6, results.stream().mapToInt(Integer::intValue).sum());
        }
    }

    @Test
    void awaitAllRethrowsFirstFailureAsCompletionException() {
        IllegalStateException boom = new IllegalStateException("second failed");

        try (EquinixAsync async = EquinixAsync.create()) {
            List<CompletableFuture<String>> futures = List.of(
                    async.call(() -> "ok"),
                    async.call(() -> {
                        throw boom;
                    }));

            CompletionException ex = assertThrows(CompletionException.class,
                    () -> EquinixAsync.awaitAll(futures));
            assertSame(boom, ex.getCause());
        }
    }

    @Test
    void closeShutsDownTheExecutorAndRejectsNewWork() {
        EquinixAsync async = EquinixAsync.create();
        async.close();

        // After shutdown, supplyAsync rejects the task, so the future completes exceptionally.
        CompletableFuture<String> future = async.call(() -> "too late");

        assertTrue(future.isCompletedExceptionally());
        CompletionException ex = assertThrows(CompletionException.class, future::join);
        assertTrue(ex.getCause() instanceof RejectedExecutionException,
                "submitting after close() should yield a RejectedExecutionException cause");
    }

    @Test
    void closeIsIdempotent() {
        EquinixAsync async = EquinixAsync.create();
        async.close();
        async.close(); // second close must not throw
        assertFalse(async.call(() -> "x").isCancelled());
    }

    @Test
    void callRejectsNullSupplier() {
        try (EquinixAsync async = EquinixAsync.create()) {
            assertThrows(NullPointerException.class, () -> async.call(null));
        }
    }

    @Test
    void runRejectsNullRunnable() {
        try (EquinixAsync async = EquinixAsync.create()) {
            assertThrows(NullPointerException.class, () -> async.run(null));
        }
    }
}
