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

package api.equinix.javasdk.core.waiter;

import org.junit.jupiter.api.Test;

import java.time.Duration;
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
}
