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

package com.eqixiac.equinix.core.exception;

/**
 * Thrown when the opt-in {@link com.eqixiac.equinix.core.http.CircuitBreaker} is open and a request
 * was rejected <em>without</em> being sent to the API. The breaker opens after a configured number
 * of consecutive service failures (5xx responses or transport {@code IOException}s) and rejects
 * requests until its cooldown elapses, at which point a single probe request is let through.
 *
 * <p>This is a client-side rejection — no HTTP exchange took place — so it extends
 * {@link EquinixClientException} rather than {@link EquinixServiceException}. Callers can wait for
 * {@link #getRemainingCooldownMillis()} before retrying.</p>
 *
 * @author ianjones
 * @see com.eqixiac.equinix.core.http.CircuitBreaker
 */
public class CircuitOpenException extends EquinixClientException {
    private static final long serialVersionUID = 1L;

    private final long remainingCooldownMillis;

    /**
     * Creates a rejection for a request refused while the circuit is open.
     *
     * @param message a human-readable summary of why the request was rejected
     * @param remainingCooldownMillis how long (in millis) until the breaker will admit a probe request
     */
    public CircuitOpenException(String message, long remainingCooldownMillis) {
        super(message);
        this.remainingCooldownMillis = Math.max(0L, remainingCooldownMillis);
    }

    /**
     * How long until the breaker transitions to half-open and admits a probe request.
     *
     * @return the remaining cooldown in milliseconds (never negative)
     */
    public long getRemainingCooldownMillis() {
        return remainingCooldownMillis;
    }
}
