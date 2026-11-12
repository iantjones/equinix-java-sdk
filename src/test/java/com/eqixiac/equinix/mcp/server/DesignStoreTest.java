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

import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link DesignStore}: content-hash ids, TTL expiry, capacity eviction reported, precise lookups. */
@DisplayName("DesignStore: ids, TTL, eviction reporting")
class DesignStoreTest {

    /** A clock the test moves forward. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-12T00:00:00Z");

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

        void advance(Duration d) {
            now = now.plus(d);
        }
    }

    private static DeploymentPlan plan(String name) {
        return DeploymentPlan.builder().deploymentName(name).cloudRouters(List.of()).providerConnections(List.of())
                .backboneLinks(List.of()).valid(true).build();
    }

    @Test
    @DisplayName("a plan is stored under its content hash; storing it again refreshes the one entry")
    void contentHashIds() {
        DesignStore store = new DesignStore();
        DesignStore.Put first = store.putPlan(plan("a"));
        assertEquals(plan("a").planId(), first.id());
        store.putPlan(plan("a"));
        assertEquals(1, store.size(DesignStore.Kind.PLAN));
        assertTrue(store.plan(first.id()).found());
    }

    @Test
    @DisplayName("an expired id answers EXPIRED, an unknown one UNKNOWN, and each says how to recover")
    void expiry() {
        MutableClock clock = new MutableClock();
        DesignStore store = new DesignStore(clock, Duration.ofMinutes(60), 20);
        String id = store.putPlan(plan("a")).id();
        clock.advance(Duration.ofMinutes(61));
        DesignStore.Lookup<DeploymentPlan> lookup = store.plan(id);
        assertEquals(DesignStore.Status.EXPIRED, lookup.status());
        assertTrue(store.explainMissing(DesignStore.Kind.PLAN, id, lookup.status()).contains("60 minutes"));
        assertEquals(DesignStore.Status.UNKNOWN, store.plan("ffffffffffffffff").status());
    }

    @Test
    @DisplayName("capacity eviction drops the oldest, is returned to the caller and answers EVICTED")
    void eviction() {
        DesignStore store = new DesignStore(Clock.systemUTC(), Duration.ofMinutes(60), 2);
        String a = store.putPlan(plan("a")).id();
        store.putPlan(plan("b"));
        DesignStore.Put third = store.putPlan(plan("c"));
        assertEquals(List.of(a), third.evicted());
        DesignStore.Lookup<DeploymentPlan> lookup = store.plan(a);
        assertEquals(DesignStore.Status.EVICTED, lookup.status());
        assertTrue(store.explainMissing(DesignStore.Kind.PLAN, a, lookup.status()).contains("evicted"));
        assertEquals(2, store.size(DesignStore.Kind.PLAN));
    }

    @Test
    @DisplayName("defaults and bounds")
    void defaults() {
        DesignStore store = new DesignStore();
        assertEquals(DesignStore.DEFAULT_TTL, store.ttl());
        assertEquals(DesignStore.DEFAULT_CAPACITY, store.capacity());
        assertThrows(IllegalArgumentException.class, () -> new DesignStore(Clock.systemUTC(), Duration.ofMinutes(1), 0));
    }
}
