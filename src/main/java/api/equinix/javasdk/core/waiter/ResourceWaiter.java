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
     * Maximum total time to wait (default 5 minutes).
     *
     * @param timeout the overall timeout
     * @return this waiter
     */
    public ResourceWaiter<T> timeout(Duration timeout) {
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
        return this;
    }

    /**
     * Delay between polls (default 5 seconds). Use {@link Duration#ZERO} to poll without sleeping.
     *
     * @param pollInterval the inter-poll delay
     * @return this waiter
     */
    public ResourceWaiter<T> pollInterval(Duration pollInterval) {
        this.pollInterval = Objects.requireNonNull(pollInterval, "poll interval must not be null");
        return this;
    }

    /**
     * Polls until the success condition holds (returning the resource), the failure condition holds
     * ({@link WaiterFailedException}), or the timeout elapses ({@link WaiterTimeoutException}).
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
            if (System.nanoTime() >= deadlineNanos) {
                throw new WaiterTimeoutException(
                        "Timed out after " + timeout + " (" + attempts + " attempt(s)) waiting for the resource condition.",
                        current);
            }
            sleep(pollMillis);
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
