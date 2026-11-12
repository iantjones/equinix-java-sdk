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

package com.eqixiac.equinix.core.async;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
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
    void awaitAllCollectsResultsInInputOrderNotCompletionOrder() throws Exception {
        try (EquinixAsync async = EquinixAsync.create()) {
            CountDownLatch othersDone = new CountDownLatch(2);
            // The first call cannot finish until the other two have, so completion order is
            // (second, third, first) while the input order is (first, second, third).
            CompletableFuture<String> first = async.call(() -> {
                awaitLatch(othersDone);
                return "first";
            });
            CompletableFuture<String> second = async.call(() -> {
                othersDone.countDown();
                return "second";
            });
            CompletableFuture<String> third = async.call(() -> {
                othersDone.countDown();
                return "third";
            });

            assertEquals(List.of("first", "second", "third"), EquinixAsync.awaitAll(List.of(first, second, third)));
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
    void closeAwaitsInFlightWork() throws Exception {
        AtomicBoolean finished = new AtomicBoolean(false);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        EquinixAsync async = EquinixAsync.create();

        // Fire-and-forget: the caller never joins the future, only closes the facade.
        async.run(() -> {
            started.countDown();
            awaitLatch(release);
            finished.set(true);
        });
        assertTrue(started.await(10, TimeUnit.SECONDS), "task did not start");

        Thread closer = new Thread(async::close, "closer");
        closer.start();

        // close() must block while the task is held. Wait (state-driven, no fixed sleep) until the
        // closer either parks in awaitTermination or wrongly returns.
        Thread.State state;
        while ((state = closer.getState()) != Thread.State.WAITING
                && state != Thread.State.TIMED_WAITING && state != Thread.State.TERMINATED) {
            Thread.onSpinWait();
        }
        assertFalse(state == Thread.State.TERMINATED || finished.get(),
                "close() returned while a call was still in flight");

        release.countDown();
        closer.join(TimeUnit.SECONDS.toMillis(10));

        assertFalse(closer.isAlive(), "close() did not return once the in-flight call finished");
        assertTrue(finished.get(),
                "close() must await in-flight calls; otherwise daemon virtual threads can be killed mid-flight at JVM exit");
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("latch not released");
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ie);
        }
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
