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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory, TTL-bounded store for {@link DeploymentPlan}s produced by
 * {@code design_plan_deployment}, keyed by the {@code plan_id} that tool returns so a later
 * {@code design_export_terraform} call can pick the plan back up.
 *
 * <p><strong>Scope:</strong> single-process only. Plan ids are valid only for the lifetime of
 * this server process and cannot be resolved by any other process or server instance — if the
 * MCP host restarts the server, previously issued ids are gone and the plan must be re-created.
 * Entries expire after {@link #ttl()} (default 30 minutes) and the store keeps at most
 * {@link #capacity()} plans, evicting the oldest.</p>
 *
 * <p>Thread-safe; all methods synchronize on the store.</p>
 */
public final class PlanStore {

    /** Default time-to-live for a stored plan. */
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    /** Default maximum number of concurrently stored plans. */
    public static final int DEFAULT_CAPACITY = 20;

    private final Clock clock;
    private final Duration ttl;
    private final int capacity;
    private final AtomicLong sequence = new AtomicLong();

    /** Insertion-ordered so capacity eviction drops the oldest plan first. */
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();

    /** Creates a store with the default TTL (30 minutes) and capacity (20 plans). */
    public PlanStore() {
        this(Clock.systemUTC(), DEFAULT_TTL, DEFAULT_CAPACITY);
    }

    /**
     * Creates a store with explicit clock, TTL, and capacity — the clock is injectable for
     * deterministic expiry tests.
     *
     * @param clock the time source used for expiry
     * @param ttl how long a stored plan stays resolvable
     * @param capacity the maximum number of concurrently stored plans
     */
    public PlanStore(Clock clock, Duration ttl, int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        this.clock = clock;
        this.ttl = ttl;
        this.capacity = capacity;
    }

    /**
     * Stores a plan and returns its freshly minted id.
     *
     * @param plan the deployment plan to keep
     * @return the {@code plan_id} to hand back to the caller
     */
    public synchronized String put(DeploymentPlan plan) {
        prune();
        while (entries.size() >= capacity) {
            Iterator<String> oldest = entries.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
        String id = "plan-" + sequence.incrementAndGet();
        entries.put(id, new Entry(plan, clock.instant().plus(ttl)));
        return id;
    }

    /**
     * Resolves a previously stored plan.
     *
     * @param planId the id returned by {@link #put(DeploymentPlan)}
     * @return the plan, or empty when the id is unknown, expired, or evicted
     */
    public synchronized Optional<DeploymentPlan> get(String planId) {
        prune();
        Entry entry = entries.get(planId);
        return entry == null ? Optional.empty() : Optional.of(entry.plan);
    }

    /**
     * @return the number of live (non-expired) plans currently stored
     */
    public synchronized int size() {
        prune();
        return entries.size();
    }

    /**
     * @return this store's time-to-live per plan
     */
    public Duration ttl() {
        return ttl;
    }

    /**
     * @return this store's maximum number of concurrently stored plans
     */
    public int capacity() {
        return capacity;
    }

    private void prune() {
        Instant now = clock.instant();
        entries.values().removeIf(e -> !e.expiresAt.isAfter(now));
    }

    private record Entry(DeploymentPlan plan, Instant expiresAt) {
    }
}
