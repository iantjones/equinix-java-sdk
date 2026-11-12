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

import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The design tools' single in-memory store: deployment plans keyed by their {@code plan_id} and
 * optimization results keyed by the fingerprint of the spec they were run for, so the tools can be
 * chained ({@code design_optimize} then {@code design_plan_deployment}, {@code design_get_plan},
 * {@code design_plan_drift}, {@code design_export_terraform}) without repeating work.
 *
 * <p><strong>Scope:</strong> single-process only. Ids resolve only inside this server process; when
 * the MCP host restarts the server they are gone. A plan id is a content hash
 * ({@code DeploymentPlan.planId()}), so storing the same plan again refreshes its entry rather than
 * creating a second one.</p>
 *
 * <p><strong>Bounds and honesty:</strong> each kind keeps at most {@link #capacity()} entries for
 * {@link #ttl()} (default 60 minutes). Storing beyond capacity evicts the oldest entry of that kind and
 * {@link #putPlan}/{@link #putOptimization} return what was evicted so the tool result can say so, as
 * the broker's {@code ProposalStore} does. Expired and evicted ids are remembered for one further TTL
 * window, so a later lookup answers {@link Status#EXPIRED} or {@link Status#EVICTED} instead of a bare
 * {@link Status#UNKNOWN}.</p>
 *
 * <p>Thread-safe; every method synchronizes on the store. The clock is injectable for tests.</p>
 */
public final class DesignStore {

    /** Default time-to-live of a stored plan or optimization result. */
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(60);

    /** Default maximum number of stored entries per kind. */
    public static final int DEFAULT_CAPACITY = 20;

    /** What a store entry holds. */
    public enum Kind {
        /** A deployment plan, keyed by {@code plan_id}. */
        PLAN,
        /** An optimization result, keyed by the spec fingerprint it was run for. */
        OPTIMIZATION
    }

    /** How a lookup resolved. */
    public enum Status {
        /** The entry is live. */
        FOUND,
        /** The entry existed and its TTL elapsed. */
        EXPIRED,
        /** The entry was evicted because the store reached capacity. */
        EVICTED,
        /** This process never stored the id (or forgot it long ago). */
        UNKNOWN
    }

    private final Clock clock;
    private final Duration ttl;
    private final int capacity;
    private final Bucket<DeploymentPlan> plans = new Bucket<>();
    private final Bucket<OptimizationResult> optimizations = new Bucket<>();

    /** Creates a store with the default TTL (60 minutes) and capacity (20 per kind). */
    public DesignStore() {
        this(Clock.systemUTC(), DEFAULT_TTL, DEFAULT_CAPACITY);
    }

    /**
     * Creates a store with an explicit clock, TTL and capacity.
     *
     * @param clock    the time source used for expiry
     * @param ttl      how long an entry stays resolvable
     * @param capacity the maximum number of entries per kind
     */
    public DesignStore(Clock clock, Duration ttl, int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        this.clock = Objects.requireNonNull(clock, "clock");
        this.ttl = Objects.requireNonNull(ttl, "ttl");
        this.capacity = capacity;
    }

    /**
     * Stores a plan under its content-hash id.
     *
     * @param plan the plan
     * @return the id and any plans evicted to make room
     */
    public synchronized Put putPlan(DeploymentPlan plan) {
        return plans.put(plan.planId(), plan);
    }

    /**
     * Stores an optimization result under its spec fingerprint.
     *
     * @param result the result; its {@code specFingerprint} must be set
     * @return the id and any results evicted to make room
     */
    public synchronized Put putOptimization(OptimizationResult result) {
        String id = result.getSpecFingerprint();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("An optimization result without a spec fingerprint cannot be stored");
        }
        return optimizations.put(id, result);
    }

    /**
     * @param planId a plan id
     * @return the lookup outcome; {@link Lookup#entry()} is set only when {@link Status#FOUND}
     */
    public synchronized Lookup<DeploymentPlan> plan(String planId) {
        return plans.get(planId);
    }

    /**
     * @param specFingerprint a spec fingerprint
     * @return the lookup outcome; {@link Lookup#entry()} is set only when {@link Status#FOUND}
     */
    public synchronized Lookup<OptimizationResult> optimization(String specFingerprint) {
        return optimizations.get(specFingerprint);
    }

    /**
     * @return the live plans, oldest first
     */
    public synchronized List<Entry<DeploymentPlan>> plans() {
        return plans.live();
    }

    /**
     * @return the live optimization results, oldest first
     */
    public synchronized List<Entry<OptimizationResult>> optimizations() {
        return optimizations.live();
    }

    /**
     * @param kind the kind
     * @return the number of live entries of that kind
     */
    public synchronized int size(Kind kind) {
        return (kind == Kind.PLAN ? plans : optimizations).live().size();
    }

    /**
     * @return the time-to-live of an entry
     */
    public Duration ttl() {
        return ttl;
    }

    /**
     * @return the maximum number of entries per kind
     */
    public int capacity() {
        return capacity;
    }

    /**
     * An instruction for the calling model when a lookup did not find a live entry.
     *
     * @param kind   what was looked up
     * @param id     the id that was given
     * @param status the lookup status
     * @return a sentence saying what happened and how to recover
     */
    public String explainMissing(Kind kind, String id, Status status) {
        String what = kind == Kind.PLAN ? "plan_id" : "optimization (spec fingerprint)";
        String redo = kind == Kind.PLAN
                ? "Run design_plan_deployment again and use the plan_id it returns."
                : "Run design_optimize again for the spec, or pass the spec itself instead of the fingerprint.";
        String why = switch (status) {
            case EXPIRED -> "expired: entries are kept for " + ttl.toMinutes() + " minutes";
            case EVICTED -> "was evicted because the store keeps at most " + capacity + " entries of this kind";
            case UNKNOWN -> "is unknown to this server process (ids do not survive a server restart and are "
                    + "never shared between processes)";
            case FOUND -> "is present";
        };
        return what + " '" + id + "' " + why + ". " + redo;
    }

    /**
     * A stored entry.
     *
     * @param id        the id
     * @param value     the stored value
     * @param storedAt  when it was stored
     * @param expiresAt when it expires
     * @param <T>       the value type
     */
    public record Entry<T>(String id, T value, Instant storedAt, Instant expiresAt) {
    }

    /**
     * The outcome of a lookup.
     *
     * @param status how it resolved
     * @param entry  the entry when {@link Status#FOUND}, else {@code null}
     * @param <T>    the value type
     */
    public record Lookup<T>(Status status, Entry<T> entry) {

        /**
         * @return {@code true} when the entry is live
         */
        public boolean found() {
            return status == Status.FOUND;
        }
    }

    /**
     * The outcome of a store.
     *
     * @param id      the id the value is stored under
     * @param evicted ids evicted (oldest first) to stay within capacity; usually empty
     */
    public record Put(String id, List<String> evicted) {
    }

    /** One kind's entries, insertion-ordered so capacity eviction drops the oldest first. */
    private final class Bucket<T> {
        private final LinkedHashMap<String, Entry<T>> live = new LinkedHashMap<>();
        private final LinkedHashMap<String, Instant> expired = new LinkedHashMap<>();
        private final LinkedHashMap<String, Instant> evicted = new LinkedHashMap<>();

        Put put(String id, T value) {
            prune();
            Instant now = clock.instant();
            live.remove(id);
            expired.remove(id);
            evicted.remove(id);
            List<String> dropped = new ArrayList<>();
            while (live.size() >= capacity) {
                Iterator<Map.Entry<String, Entry<T>>> oldest = live.entrySet().iterator();
                String droppedId = oldest.next().getKey();
                oldest.remove();
                evicted.put(droppedId, now.plus(ttl));
                dropped.add(droppedId);
            }
            live.put(id, new Entry<>(id, value, now, now.plus(ttl)));
            return new Put(id, List.copyOf(dropped));
        }

        Lookup<T> get(String id) {
            prune();
            if (id == null) {
                return new Lookup<>(Status.UNKNOWN, null);
            }
            Entry<T> entry = live.get(id);
            if (entry != null) {
                return new Lookup<>(Status.FOUND, entry);
            }
            if (expired.containsKey(id)) {
                return new Lookup<>(Status.EXPIRED, null);
            }
            if (evicted.containsKey(id)) {
                return new Lookup<>(Status.EVICTED, null);
            }
            return new Lookup<>(Status.UNKNOWN, null);
        }

        List<Entry<T>> live() {
            prune();
            return List.copyOf(live.values());
        }

        private void prune() {
            Instant now = clock.instant();
            Iterator<Map.Entry<String, Entry<T>>> it = live.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Entry<T>> e = it.next();
                if (!e.getValue().expiresAt().isAfter(now)) {
                    expired.put(e.getKey(), now.plus(ttl));
                    it.remove();
                }
            }
            expired.values().removeIf(forgetAt -> !forgetAt.isAfter(now));
            evicted.values().removeIf(forgetAt -> !forgetAt.isAfter(now));
        }
    }
}
