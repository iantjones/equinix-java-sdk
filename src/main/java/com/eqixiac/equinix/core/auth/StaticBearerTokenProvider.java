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

import java.util.Objects;

/**
 * A {@link BearerTokenProvider} that always returns one fixed {@link BearerToken}.
 *
 * @author ianjones
 */
public final class StaticBearerTokenProvider implements BearerTokenProvider {

    private final BearerToken token;

    /**
     * Creates a provider for a fixed token.
     *
     * @param token the token to return; must not be {@code null}
     */
    public StaticBearerTokenProvider(BearerToken token) {
        this.token = Objects.requireNonNull(token, "token");
    }

    /**
     * Creates a provider for a fixed token value with no known expiry.
     *
     * @param token the bearer token value
     */
    public StaticBearerTokenProvider(String token) {
        this(BearerToken.of(token));
    }

    @Override
    public BearerToken getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "StaticBearerTokenProvider[" + token + "]";
    }
}
