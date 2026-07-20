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

package api.equinix.javasdk.mcp.server;

import api.equinix.javasdk.design.optimizer.wizard.model.DeploymentPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PlanStore — TTL expiry, capacity eviction, id uniqueness")
class PlanStoreTest {

    /** A mutable test clock. */
    private static final class SteppingClock extends Clock {
        private Instant now = Instant.parse("2026-07-20T12:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration by) {
            now = now.plus(by);
        }
    }

    private static DeploymentPlan plan() {
        return DeploymentPlan.builder().build();
    }

    @Test
    @DisplayName("resolves a stored plan until the TTL elapses, then forgets it")
    void ttlExpiry() {
        SteppingClock clock = new SteppingClock();
        PlanStore store = new PlanStore(clock, Duration.ofMinutes(30), 20);

        String id = store.put(plan());
        assertTrue(store.get(id).isPresent(), "fresh plan resolves");

        clock.advance(Duration.ofMinutes(29));
        assertTrue(store.get(id).isPresent(), "still resolvable just before the TTL");

        clock.advance(Duration.ofMinutes(2));
        assertTrue(store.get(id).isEmpty(), "expired plan no longer resolves");
        assertEquals(0, store.size(), "expired entries are pruned");
    }

    @Test
    @DisplayName("evicts the oldest plan once capacity is reached")
    void capacityEviction() {
        SteppingClock clock = new SteppingClock();
        PlanStore store = new PlanStore(clock, Duration.ofMinutes(30), 2);

        String first = store.put(plan());
        String second = store.put(plan());
        String third = store.put(plan());

        assertTrue(store.get(first).isEmpty(), "oldest plan was evicted at capacity");
        assertTrue(store.get(second).isPresent(), "newer plans survive");
        assertTrue(store.get(third).isPresent(), "newest plan survives");
        assertEquals(2, store.size());
    }

    @Test
    @DisplayName("mints unique ids and reports its configuration")
    void idsAndConfiguration() {
        PlanStore store = new PlanStore();
        String a = store.put(plan());
        String b = store.put(plan());
        assertNotEquals(a, b, "each put mints a fresh id");
        assertEquals(PlanStore.DEFAULT_TTL, store.ttl());
        assertEquals(PlanStore.DEFAULT_CAPACITY, store.capacity());
        assertTrue(store.get("plan-does-not-exist").isEmpty(), "unknown ids resolve to empty");
    }
}
