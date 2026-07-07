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

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A small, generic poller for asynchronous Equinix resources: re-fetch a resource on an interval
 * until it reaches a desired state, fails, or a timeout elapses. Most provisioning operations are
 * asynchronous (a created connection starts {@code PROVISIONING} and later becomes
 * {@code PROVISIONED}); this removes the hand-rolled sleep/poll loops callers would otherwise write.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * Connection conn = fabric.connections().define(ConnectionType.EVPL_VC)... .create();
 *
 * Connection ready = ResourceWaiter.forResource(() -> fabric.connections().getByUuid(conn.getUuid()))
 *         .until(c -> c.getState() == ConnectionState.PROVISIONED)
 *         .failWhen(c -> c.getState() == ConnectionState.FAILED)
 *         .timeout(Duration.ofMinutes(10))
 *         .pollInterval(Duration.ofSeconds(15))
 *         .await();
 * }</pre>
 *
 * <p>{@link #await()} returns the resource once the success condition holds, throws
 * {@link WaiterFailedException} if the failure condition is reached first, and
 * {@link WaiterTimeoutException} if neither happens before the timeout.</p>
 *
 * @param <T> the resource type
 * @author ianjones
 */
public final class ResourceWaiter<T> {

    private final Supplier<T> fetch;
    private Predicate<T> successCondition;
    private Predicate<T> failureCondition = t -> false;
    private Duration timeout = Duration.ofMinutes(5);
    private Duration pollInterval = Duration.ofSeconds(5);

    private ResourceWaiter(Supplier<T> fetch) {
        this.fetch = Objects.requireNonNull(fetch, "fetch supplier must not be null");
    }

    /**
     * Begins a waiter that re-fetches the resource via the given supplier (e.g.
     * {@code () -> fabric.connections().getByUuid(uuid)}).
     *
     * @param fetch supplies the current resource state on each poll
     * @param <T> the resource type
     * @return a new waiter to configure
     */
    public static <T> ResourceWaiter<T> forResource(Supplier<T> fetch) {
        return new ResourceWaiter<>(fetch);
    }

    /**
     * The success condition; {@link #await()} returns once it holds. Required.
     *
     * @param successCondition predicate over the fetched resource
     * @return this waiter
     */
    public ResourceWaiter<T> until(Predicate<T> successCondition) {
        this.successCondition = Objects.requireNonNull(successCondition, "success condition must not be null");
        return this;
    }

    /**
     * An optional terminal failure condition; if it holds, {@link #await()} throws
     * {@link WaiterFailedException} immediately rather than polling to timeout.
     *
     * @param failureCondition predicate over the fetched resource
     * @return this waiter
     */
    public ResourceWaiter<T> failWhen(Predicate<T> failureCondition) {
        this.failureCondition = Objects.requireNonNull(failureCondition, "failure condition must not be null");
        return this;
    }

    /**
     * Maximum total time to wait (default 5 minutes). The final sleep is clamped to the remaining
     * budget, so {@link #await()} never overruns the timeout by a full poll interval.
     *
     * @param timeout the overall timeout; must not be negative
     * @return this waiter
     */
    public ResourceWaiter<T> timeout(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout must not be null");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must not be negative: " + timeout);
        }
        this.timeout = timeout;
        return this;
    }

    /**
     * Delay between polls (default 5 seconds). Use {@link Duration#ZERO} to poll without sleeping.
     *
     * @param pollInterval the inter-poll delay; must not be negative
     * @return this waiter
     */
    public ResourceWaiter<T> pollInterval(Duration pollInterval) {
        Objects.requireNonNull(pollInterval, "poll interval must not be null");
        if (pollInterval.isNegative()) {
            throw new IllegalArgumentException("poll interval must not be negative: " + pollInterval);
        }
        this.pollInterval = pollInterval;
        return this;
    }

    /**
     * Polls until the success condition holds (returning the resource), the failure condition holds
     * ({@link WaiterFailedException}), or the timeout elapses ({@link WaiterTimeoutException}).
     * Each sleep is clamped to the remaining timeout budget, so the total wait never exceeds the
     * configured timeout by more than one final fetch.
     *
     * <p>If the polling thread is interrupted while sleeping, a plain {@link WaiterException} is
     * thrown and the thread's interrupt flag is restored.</p>
     *
     * @return the resource once the success condition is met
     */
    public T await() {
        if (successCondition == null) {
            throw new IllegalStateException("A success condition is required; call until(...) before await().");
        }

        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        long pollMillis = Math.max(0L, pollInterval.toMillis());
        int attempts = 0;

        while (true) {
            T current = fetch.get();
            attempts++;

            if (successCondition.test(current)) {
                return current;
            }
            if (failureCondition.test(current)) {
                throw new WaiterFailedException(
                        "Resource reached a failure state after " + attempts + " attempt(s).", current);
            }
            long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime());
            if (remainingMillis <= 0L) {
                throw new WaiterTimeoutException(
                        "Timed out after " + timeout + " (" + attempts + " attempt(s)) waiting for the resource condition.",
                        current);
            }
            // Never sleep past the deadline: the last sleep is clamped to the remaining budget so
            // the configured timeout is an actual upper bound (plus at most one final fetch).
            sleep(Math.min(pollMillis, remainingMillis));
        }
    }

    private static void sleep(long millis) {
        if (millis <= 0L) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new WaiterException("Interrupted while waiting for the resource condition.", ie);
        }
    }
}
