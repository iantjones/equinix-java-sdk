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

package com.eqixiac.equinix.core.http;

import com.eqixiac.equinix.core.exception.CircuitOpenException;

/**
 * A simple, opt-in consecutive-failure circuit breaker for the SDK's request execution path.
 *
 * <p>Disabled by default; enable it via
 * {@code EquinixConfig.builder().circuitBreaker(new CircuitBreaker(5, 30_000)).build()}. When
 * enabled it is consulted for every request attempt (including retry attempts), alongside
 * {@link RetryPolicy}:</p>
 *
 * <ul>
 *   <li><b>CLOSED</b> (normal): requests flow through. Each <em>service failure</em> — a 5xx
 *       response or a transport {@code IOException} — increments a consecutive-failure counter;
 *       any completed HTTP exchange with a status below 500 resets it. Once the counter reaches
 *       {@code failureThreshold}, the breaker opens.</li>
 *   <li><b>OPEN</b>: every request is rejected immediately with a {@link CircuitOpenException}
 *       (no HTTP exchange happens) until {@code openCooldownMillis} elapses. Outcomes reported by
 *       <em>straggler</em> requests — attempts admitted before the circuit opened that complete
 *       afterwards — are ignored: a straggler success must not snap the circuit shut (that would
 *       bypass the cooldown and the half-open probe) and a straggler failure must not extend the
 *       cooldown.</li>
 *   <li><b>HALF_OPEN</b>: after the cooldown, a single probe request is admitted (further requests
 *       are rejected while the probe is pending — a probe slot expires after another cooldown, so
 *       a probe that dies without reporting cannot wedge the breaker). Only the probe's completion
 *       moves the breaker on: a successful probe closes it; a failed probe re-opens it for another
 *       cooldown.</li>
 * </ul>
 *
 * <p>Client-side errors (4xx) are deliberately <em>not</em> counted as failures — they indicate the
 * service is up and answering. Rate limiting (429) is likewise left to {@link RetryPolicy}'s
 * backoff. Instances are thread-safe and may be shared by all requests of a client (which is
 * exactly how the SDK uses them).</p>
 *
 * @author ianjones
 * @see CircuitOpenException
 */
public final class CircuitBreaker {

    /**
     * The breaker's lifecycle state.
     */
    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final long openCooldownMillis;

    private State state = State.CLOSED;
    private int consecutiveFailures = 0;
    private long openedAtNanos;
    private long probeStartedAtNanos;

    /**
     * Creates a breaker that opens after {@code failureThreshold} consecutive service failures and
     * admits a half-open probe after {@code openCooldownMillis} of rejection.
     *
     * @param failureThreshold number of consecutive failures that opens the circuit (must be &gt;= 1)
     * @param openCooldownMillis how long the circuit stays open before a probe is admitted (must be &gt;= 1)
     */
    public CircuitBreaker(int failureThreshold, long openCooldownMillis) {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("failureThreshold must be >= 1, was: " + failureThreshold);
        }
        if (openCooldownMillis < 1) {
            throw new IllegalArgumentException("openCooldownMillis must be >= 1, was: " + openCooldownMillis);
        }
        this.failureThreshold = failureThreshold;
        this.openCooldownMillis = openCooldownMillis;
    }

    /**
     * Requests permission to execute one attempt. Returns normally when the attempt may proceed;
     * throws when the circuit is open (or a half-open probe is already pending).
     *
     * @throws CircuitOpenException if the request must not be sent
     */
    public synchronized void acquire() throws CircuitOpenException {
        long now = System.nanoTime();
        switch (state) {
            case CLOSED:
                return;
            case OPEN: {
                long remaining = remainingMillis(now, openedAtNanos);
                if (remaining > 0) {
                    throw new CircuitOpenException(
                            "Circuit breaker is open after " + consecutiveFailures
                                    + " consecutive failures; next probe permitted in " + remaining + "ms.",
                            remaining);
                }
                state = State.HALF_OPEN;
                probeStartedAtNanos = now;
                return;
            }
            case HALF_OPEN:
            default: {
                long remaining = remainingMillis(now, probeStartedAtNanos);
                if (remaining > 0) {
                    throw new CircuitOpenException(
                            "Circuit breaker is half-open with a probe request pending; "
                                    + "next probe permitted in " + remaining + "ms.",
                            remaining);
                }
                // The previous probe never reported back (e.g. died mid-flight); its slot has
                // expired, so this caller becomes the new probe.
                probeStartedAtNanos = now;
            }
        }
    }

    /**
     * Records a healthy outcome (any completed HTTP exchange with status &lt; 500). In CLOSED this
     * resets the consecutive-failure count; in HALF_OPEN it is the probe's success and closes the
     * circuit. In OPEN it is ignored — it can only come from a straggler request admitted before
     * the circuit opened, and closing on it would bypass the cooldown and the half-open probe.
     */
    public synchronized void recordSuccess() {
        switch (state) {
            case CLOSED:
                consecutiveFailures = 0;
                return;
            case OPEN:
                // A straggler's late success says nothing about current service health; only the
                // half-open probe may close the circuit.
                return;
            case HALF_OPEN:
            default:
                // The probe succeeded: the service has recovered.
                consecutiveFailures = 0;
                state = State.CLOSED;
        }
    }

    /**
     * Records a service failure (5xx response or transport {@code IOException}). In CLOSED this
     * opens the circuit once the consecutive-failure count reaches the threshold; in HALF_OPEN it
     * is the probe's failure and re-opens the circuit for a fresh cooldown. In OPEN it is ignored —
     * a straggler's late failure must not keep extending the cooldown.
     */
    public synchronized void recordFailure() {
        switch (state) {
            case CLOSED:
                consecutiveFailures++;
                if (consecutiveFailures >= failureThreshold) {
                    state = State.OPEN;
                    openedAtNanos = System.nanoTime();
                }
                return;
            case OPEN:
                // Straggler outcome while already open: ignore, so the cooldown is not extended.
                return;
            case HALF_OPEN:
            default:
                // The probe failed: re-open for another full cooldown.
                consecutiveFailures++;
                state = State.OPEN;
                openedAtNanos = System.nanoTime();
        }
    }

    /**
     * The current state, for diagnostics. Note that an elapsed-cooldown OPEN circuit reports
     * {@code OPEN} until the next {@link #acquire()} transitions it to {@code HALF_OPEN}.
     *
     * @return the current {@link State}
     */
    public synchronized State getState() {
        return state;
    }

    private long remainingMillis(long nowNanos, long sinceNanos) {
        long elapsedMillis = (nowNanos - sinceNanos) / 1_000_000L;
        return openCooldownMillis - elapsedMillis;
    }
}
