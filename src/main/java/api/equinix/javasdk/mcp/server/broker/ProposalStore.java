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

package api.equinix.javasdk.mcp.server.broker;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory, TTL-bounded, single-use store for the Safe Mutation Broker's pending proposals,
 * keyed by confirm token.
 *
 * <p><strong>Scope:</strong> single-process only, like the plan store. Tokens are valid only
 * for the lifetime of this server process; if the MCP host restarts the server, outstanding
 * proposals are gone and must be re-proposed. Entries expire after {@link #ttl()} (default
 * 10 minutes) and the store keeps at most {@link #capacity()} live proposals, evicting the
 * oldest.</p>
 *
 * <p><strong>Single use, consumed on attempt:</strong> {@link #consume(String)} removes a live
 * proposal on the <em>first</em> attempt, before the caller executes anything — a failed
 * execution does not resurrect the token. Recently consumed and recently expired tokens are
 * remembered (for one further TTL window) so a second confirm gets a precise
 * {@link Outcome#REPLAYED} or {@link Outcome#EXPIRED} answer instead of a generic
 * {@link Outcome#UNKNOWN}.</p>
 *
 * <p>Thread-safe; all methods synchronize on the store. The clock is injectable for
 * deterministic expiry tests.</p>
 */
public final class ProposalStore {

    /** Default time-to-live for a pending proposal. */
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    /** Default maximum number of concurrently pending proposals. */
    public static final int DEFAULT_CAPACITY = 20;

    private final Clock clock;
    private final Duration ttl;
    private final int capacity;
    private final AtomicLong sequence = new AtomicLong();
    private final SecureRandom random = new SecureRandom();

    /** Insertion-ordered so capacity eviction drops the oldest proposal first. */
    private final LinkedHashMap<String, PendingChange> live = new LinkedHashMap<>();

    /** Tokens consumed by a confirm attempt, remembered until the mapped instant. */
    private final LinkedHashMap<String, Instant> consumed = new LinkedHashMap<>();

    /** Tokens that expired unconfirmed, remembered until the mapped instant. */
    private final LinkedHashMap<String, Instant> expired = new LinkedHashMap<>();

    /** Creates a store with the default TTL (10 minutes) and capacity (20 proposals). */
    public ProposalStore() {
        this(Clock.systemUTC(), DEFAULT_TTL, DEFAULT_CAPACITY);
    }

    /**
     * Creates a store with explicit clock, TTL, and capacity.
     *
     * @param clock the time source used for expiry
     * @param ttl how long a minted proposal stays confirmable
     * @param capacity the maximum number of concurrently pending proposals
     */
    public ProposalStore(Clock clock, Duration ttl, int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        this.clock = clock;
        this.ttl = ttl;
        this.capacity = capacity;
    }

    /**
     * Mints a proposal: a fresh unguessable token bound to the given canonical spec and its
     * SHA-256, expiring one TTL from now.
     *
     * @param changeType the kind of create the proposal performs
     * @param canonicalSpec the canonical-form spec JSON the dry run validated
     * @param specSha256 the SHA-256 (hex) of {@code canonicalSpec}
     * @return the pending change, carrying the confirm token and expiry
     */
    public synchronized PendingChange mint(ChangeType changeType, String canonicalSpec, String specSha256) {
        Objects.requireNonNull(changeType, "changeType");
        Objects.requireNonNull(canonicalSpec, "canonicalSpec");
        Objects.requireNonNull(specSha256, "specSha256");
        prune();
        while (live.size() >= capacity) {
            Iterator<String> oldest = live.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
        byte[] entropy = new byte[12];
        random.nextBytes(entropy);
        String token = "chg-" + sequence.incrementAndGet() + "-" + HexFormat.of().formatHex(entropy);
        PendingChange change = new PendingChange(token, changeType, canonicalSpec, specSha256,
                clock.instant().plus(ttl));
        live.put(token, change);
        return change;
    }

    /**
     * Consumes a confirm token. A live token is removed <em>on this attempt</em> and returned
     * with {@link Outcome#CONSUMED}; every other outcome carries no change.
     *
     * @param token the confirm token presented by {@code fabric_confirm_change}
     * @return the consumption result — exactly one of consumed, expired, replayed, or unknown
     */
    public synchronized Consumption consume(String token) {
        prune();
        if (consumed.containsKey(token)) {
            return new Consumption(Outcome.REPLAYED, null);
        }
        PendingChange change = live.remove(token);
        if (change != null) {
            consumed.put(token, clock.instant().plus(ttl));
            return new Consumption(Outcome.CONSUMED, change);
        }
        if (expired.containsKey(token)) {
            return new Consumption(Outcome.EXPIRED, null);
        }
        return new Consumption(Outcome.UNKNOWN, null);
    }

    /**
     * @return the number of live (confirmable) proposals currently pending
     */
    public synchronized int size() {
        prune();
        return live.size();
    }

    /**
     * @return this store's time-to-live per proposal
     */
    public Duration ttl() {
        return ttl;
    }

    /**
     * @return this store's maximum number of concurrently pending proposals
     */
    public int capacity() {
        return capacity;
    }

    private void prune() {
        Instant now = clock.instant();
        Iterator<Map.Entry<String, PendingChange>> it = live.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, PendingChange> entry = it.next();
            if (!entry.getValue().expiresAt().isAfter(now)) {
                expired.put(entry.getKey(), now.plus(ttl));
                it.remove();
            }
        }
        consumed.values().removeIf(forgetAt -> !forgetAt.isAfter(now));
        expired.values().removeIf(forgetAt -> !forgetAt.isAfter(now));
    }

    /** How a {@link #consume(String)} attempt resolved. */
    public enum Outcome {

        /** The token was live: it is now consumed and the pending change is returned. */
        CONSUMED,

        /** The token existed but its TTL elapsed before any confirm attempt. */
        EXPIRED,

        /** The token was already consumed by an earlier confirm attempt. */
        REPLAYED,

        /** The token was never minted by this process (or is long forgotten). */
        UNKNOWN
    }

    /**
     * The result of one consume attempt.
     *
     * @param outcome how the attempt resolved
     * @param change the consumed proposal — non-null only when {@code outcome} is
     *        {@link Outcome#CONSUMED}
     */
    public record Consumption(Outcome outcome, PendingChange change) {
    }
}
