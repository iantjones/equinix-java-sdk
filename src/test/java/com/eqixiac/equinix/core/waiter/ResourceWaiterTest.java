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

package com.eqixiac.equinix.core.waiter;

import org.junit.jupiter.api.Test;

import com.eqixiac.equinix.core.ManualClock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceWaiterTest {

    @Test
    void returnsResourceOnceSuccessConditionHolds() {
        AtomicInteger calls = new AtomicInteger();
        // "PROVISIONING" for the first two polls, then "PROVISIONED".
        Supplier<String> fetch = () -> calls.incrementAndGet() < 3 ? "PROVISIONING" : "PROVISIONED";

        String result = ResourceWaiter.forResource(fetch)
                .until("PROVISIONED"::equals)
                .pollInterval(Duration.ZERO)
                .await();

        assertEquals("PROVISIONED", result);
        assertEquals(3, calls.get());
    }

    @Test
    void throwsFailedWhenFailureConditionHolds() {
        WaiterFailedException ex = assertThrows(WaiterFailedException.class, () ->
                ResourceWaiter.forResource(() -> "FAILED")
                        .until("PROVISIONED"::equals)
                        .failWhen("FAILED"::equals)
                        .pollInterval(Duration.ZERO)
                        .await());

        assertEquals("FAILED", ex.getResource());
    }

    @Test
    void throwsTimeoutWhenConditionNeverHolds() {
        WaiterTimeoutException ex = assertThrows(WaiterTimeoutException.class, () ->
                ResourceWaiter.forResource(() -> "PROVISIONING")
                        .until("PROVISIONED"::equals)
                        .timeout(Duration.ofMillis(30))
                        .pollInterval(Duration.ZERO)
                        .await());

        assertEquals("PROVISIONING", ex.getLastObserved());
    }

    @Test
    void requiresASuccessCondition() {
        assertThrows(IllegalStateException.class,
                () -> ResourceWaiter.forResource(() -> "x").await());
    }

    @Test
    void timeoutIsNotOverrunByAFullPollInterval() {
        // Timeout far smaller than the poll interval: the final sleep must be clamped to the
        // remaining budget instead of sleeping the whole interval past the deadline.
        ManualClock clock = new ManualClock();
        List<Long> sleeps = new ArrayList<>();

        assertThrows(WaiterTimeoutException.class, () ->
                ResourceWaiter.forResource(() -> "PROVISIONING")
                        .until("PROVISIONED"::equals)
                        .timeout(Duration.ofMillis(100))
                        .pollInterval(Duration.ofSeconds(30))
                        .clock(clock)
                        .sleeper(ms -> { sleeps.add(ms); clock.advanceMillis(ms); })
                        .await());

        assertEquals(List.of(100L), sleeps, "the only sleep must be clamped to the 100ms budget");
    }

    @Test
    void pollsOnTheIntervalUntilTheTimeoutElapses() {
        ManualClock clock = new ManualClock();
        AtomicInteger calls = new AtomicInteger();
        List<Long> sleeps = new ArrayList<>();

        assertThrows(WaiterTimeoutException.class, () ->
                ResourceWaiter.forResource(() -> { calls.incrementAndGet(); return "PROVISIONING"; })
                        .until("PROVISIONED"::equals)
                        .timeout(Duration.ofSeconds(25))
                        .pollInterval(Duration.ofSeconds(10))
                        .clock(clock)
                        .sleeper(ms -> { sleeps.add(ms); clock.advanceMillis(ms); })
                        .await());

        // Fetches at t=0, 10s, 20s, 25s; sleeps 10s, 10s, then the clamped 5s.
        assertEquals(List.of(10_000L, 10_000L, 5_000L), sleeps);
        assertEquals(4, calls.get());
    }

    @Test
    void rejectsNegativeTimeoutAndPollInterval() {
        ResourceWaiter<String> waiter = ResourceWaiter.forResource(() -> "x");

        assertThrows(IllegalArgumentException.class, () -> waiter.timeout(Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class, () -> waiter.pollInterval(Duration.ofMillis(-1)));
    }

    @Test
    void typedPayloadAccessorsCastTheResource() {
        WaiterFailedException failed = assertThrows(WaiterFailedException.class, () ->
                ResourceWaiter.forResource(() -> "FAILED")
                        .until("PROVISIONED"::equals)
                        .failWhen("FAILED"::equals)
                        .pollInterval(Duration.ZERO)
                        .await());

        assertEquals("FAILED", failed.getResource(String.class));
        assertThrows(ClassCastException.class, () -> failed.getResource(Integer.class));

        WaiterTimeoutException timedOut = assertThrows(WaiterTimeoutException.class, () ->
                ResourceWaiter.forResource(() -> "PROVISIONING")
                        .until("PROVISIONED"::equals)
                        .timeout(Duration.ofMillis(10))
                        .pollInterval(Duration.ZERO)
                        .await());

        assertEquals("PROVISIONING", timedOut.getLastObserved(String.class));
        assertThrows(ClassCastException.class, () -> timedOut.getLastObserved(Integer.class));
    }
}
