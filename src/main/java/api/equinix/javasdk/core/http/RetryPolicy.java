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

package api.equinix.javasdk.core.http;

import api.equinix.javasdk.core.enums.HttpMethod;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Controls automatic retry of transient failures. By default the SDK retries throttling (429) and
 * the common transient server errors (500, 502, 503, 504) as well as transient {@code IOException}s,
 * using exponential backoff with full jitter and honoring a {@code Retry-After} header when present.
 *
 * <p>To avoid duplicate side effects, retries are by default applied <em>only to idempotent
 * methods</em> (everything except {@code POST}): a transient failure that occurs after the server
 * has already processed a {@code POST} create would otherwise re-send it and create a second
 * resource. Set {@code retryNonIdempotentMethods} if your endpoints are safe to retry (e.g. they
 * dedupe via an idempotency key, or the POST is a side-effect-free search).</p>
 *
 * <p>Retries are bounded by {@link #getMaxRetries()} attempts beyond the initial request. Pass
 * {@link #none()} to disable retries entirely, or build a custom policy via the constructor.</p>
 *
 * @author ianjones
 */
public final class RetryPolicy {

    private final int maxRetries;
    private final long baseDelayMillis;
    private final long maxDelayMillis;
    private final Set<Integer> retryableStatusCodes;
    private final boolean retryOnIoException;
    private final boolean honorRetryAfter;
    private final boolean retryNonIdempotentMethods;

    /**
     * Constructor for RetryPolicy that retries only idempotent methods (POST is never retried).
     *
     * @param maxRetries maximum retry attempts beyond the initial request (0 disables retries)
     * @param baseDelayMillis base backoff delay; the n-th retry waits up to {@code base * 2^n}
     * @param maxDelayMillis ceiling on any single backoff wait
     * @param retryableStatusCodes HTTP status codes that trigger a retry
     * @param retryOnIoException whether transient {@link java.io.IOException}s are retried
     * @param honorRetryAfter whether a {@code Retry-After} response header overrides the computed backoff
     */
    public RetryPolicy(int maxRetries, long baseDelayMillis, long maxDelayMillis,
                       Set<Integer> retryableStatusCodes, boolean retryOnIoException, boolean honorRetryAfter) {
        this(maxRetries, baseDelayMillis, maxDelayMillis, retryableStatusCodes, retryOnIoException, honorRetryAfter, false);
    }

    /**
     * Full constructor.
     *
     * @param maxRetries maximum retry attempts beyond the initial request (0 disables retries)
     * @param baseDelayMillis base backoff delay; the n-th retry waits up to {@code base * 2^n}
     * @param maxDelayMillis ceiling on any single backoff wait
     * @param retryableStatusCodes HTTP status codes that trigger a retry
     * @param retryOnIoException whether transient {@link java.io.IOException}s are retried
     * @param honorRetryAfter whether a {@code Retry-After} response header overrides the computed backoff
     * @param retryNonIdempotentMethods whether non-idempotent methods (POST) are also retried — leave
     *                                  {@code false} unless your endpoints dedupe retried requests
     */
    public RetryPolicy(int maxRetries, long baseDelayMillis, long maxDelayMillis,
                       Set<Integer> retryableStatusCodes, boolean retryOnIoException, boolean honorRetryAfter,
                       boolean retryNonIdempotentMethods) {
        this.maxRetries = Math.max(0, maxRetries);
        this.baseDelayMillis = Math.max(0, baseDelayMillis);
        this.maxDelayMillis = Math.max(0, maxDelayMillis);
        this.retryableStatusCodes = Set.copyOf(retryableStatusCodes);
        this.retryOnIoException = retryOnIoException;
        this.honorRetryAfter = honorRetryAfter;
        this.retryNonIdempotentMethods = retryNonIdempotentMethods;
    }

    /**
     * Whether a request with the given HTTP method is eligible for retry. POST is treated as the
     * only non-idempotent method and is not retried unless {@code retryNonIdempotentMethods} is set.
     *
     * @param method the request method (may be {@code null}, treated as retryable)
     * @return {@code true} if retries are permitted for this method
     */
    public boolean isRetryableMethod(HttpMethod method) {
        return retryNonIdempotentMethods || method != HttpMethod.POST;
    }

    /**
     * The default policy: up to 3 retries on 429/500/502/503/504 and transient IO errors, 500ms base
     * backoff capped at 20s, with {@code Retry-After} honored.
     *
     * @return the default policy
     */
    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(3, 500, 20_000, Set.of(429, 500, 502, 503, 504), true, true);
    }

    /**
     * A policy that performs no retries.
     *
     * @return a no-retry policy
     */
    public static RetryPolicy none() {
        return new RetryPolicy(0, 0, 0, Set.of(), false, false);
    }

    /**
     *
     * @return the maximum number of retries
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Whether a response with the given status code should be retried.
     *
     * @param statusCode the HTTP status code
     * @return {@code true} if retryable
     */
    public boolean isRetryableStatus(int statusCode) {
        return retryableStatusCodes.contains(statusCode);
    }

    /**
     * <p>Whether transient IO exceptions are retried.</p>
     *
     * @return {@code true} if IO exceptions are retried
     */
    public boolean isRetryOnIoException() {
        return retryOnIoException;
    }

    /**
     * Computes the backoff wait before the given retry attempt.
     *
     * @param attempt zero-based retry attempt index
     * @param retryAfterMillis the server-provided {@code Retry-After} in millis, or {@code null}
     * @return the number of milliseconds to wait
     */
    public long computeBackoffMillis(int attempt, Long retryAfterMillis) {
        if (honorRetryAfter && retryAfterMillis != null && retryAfterMillis >= 0) {
            return Math.min(retryAfterMillis, maxDelayMillis);
        }
        double exp = baseDelayMillis * Math.pow(2.0, attempt);
        long capped = (long) Math.min(exp, (double) maxDelayMillis);
        if (capped <= 0) {
            return 0;
        }
        // Full jitter: a random wait in [0, capped] smooths out synchronized retry storms.
        return ThreadLocalRandom.current().nextLong(capped + 1);
    }
}
