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

import java.util.function.Supplier;

/**
 * Supplies the bearer token that signs every authenticated request, as an alternative to OAuth2
 * client credentials.
 *
 * <p>Use it when a client-credentials token is not accepted. The live case (2026-11-11): IAM
 * project-scoped endpoints such as {@code /v1/projects/{project}/roles} and
 * {@code principalPolicies} answer "Project arg only supported when using an access-policy auth
 * token" to a client-credentials token, and need an STS-issued access-policy token instead.
 * Another case is a token minted outside the SDK by a workload-identity flow.</p>
 *
 * <p>The SDK calls {@link #getToken()} on the first authenticated request, again shortly before a
 * known expiry, and once more after a request is answered with HTTP 401 (that request is then
 * replayed once with the new token; a second 401 is thrown). Calls are single-flight: concurrent
 * requests that need a token cause one call. Implementations should be thread-safe.</p>
 *
 * <pre>{@code
 * IAM iam = new IAM(BearerTokenProvider.of(accessPolicyToken));
 * IAM rotating = new IAM(BearerTokenProvider.from(() -> sts.exchange(...)));
 * try (Equinix eq = new Equinix(BearerTokenProvider.from(myTokenSource))) { ... }
 * }</pre>
 *
 * @author ianjones
 * @see StaticBearerTokenProvider
 * @see SupplierBearerTokenProvider
 */
@FunctionalInterface
public interface BearerTokenProvider {

    /**
     * Returns the token to sign requests with. Called once per (re-)authentication.
     *
     * @return the current token; must not be {@code null}
     */
    BearerToken getToken();

    /**
     * A provider that always returns the same token (no known expiry). After a 401 the SDK asks
     * again, gets the same token, replays once, and then surfaces the second 401.
     *
     * @param token the bearer token value
     * @return the provider
     */
    static BearerTokenProvider of(String token) {
        return new StaticBearerTokenProvider(BearerToken.of(token));
    }

    /**
     * A provider that obtains each token from a supplier, for example one that performs an STS
     * token exchange or reads a rotated file.
     *
     * @param supplier supplies a fresh token on each call
     * @return the provider
     */
    static BearerTokenProvider from(Supplier<BearerToken> supplier) {
        return new SupplierBearerTokenProvider(supplier);
    }
}
