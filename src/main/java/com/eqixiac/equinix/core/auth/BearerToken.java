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

package com.eqixiac.equinix.core.auth;

import java.time.Instant;
import java.util.Objects;

/**
 * A bearer access token supplied by a {@link BearerTokenProvider}: the token value sent as
 * {@code Authorization: Bearer <value>} and, optionally, the instant it expires.
 *
 * <p>When {@code expiresAt} is known, the SDK asks the provider for a new token shortly (30
 * seconds) before that instant. When it is {@code null}, the token is used until the service
 * answers HTTP 401, at which point the provider is asked once for a replacement.</p>
 *
 * @param value     the bearer token; must not be {@code null} or blank
 * @param expiresAt when the token expires, or {@code null} when unknown
 * @author ianjones
 */
public record BearerToken(String value, Instant expiresAt) {

    /**
     * Validates the token value.
     *
     * @throws NullPointerException     if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is blank
     */
    public BearerToken {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("bearer token value must not be blank");
        }
    }

    /**
     * A token with no known expiry.
     *
     * @param value the bearer token
     * @return the token
     */
    public static BearerToken of(String value) {
        return new BearerToken(value, null);
    }

    /**
     * Never prints the token value, so a token cannot leak into logs through {@code toString()}.
     *
     * @return a redacted description
     */
    @Override
    public String toString() {
        return "BearerToken[value=***, expiresAt=" + expiresAt + "]";
    }
}
