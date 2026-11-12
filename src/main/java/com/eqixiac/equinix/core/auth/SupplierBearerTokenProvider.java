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

import com.eqixiac.equinix.core.exception.EquinixClientException;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A {@link BearerTokenProvider} that obtains every token from a {@link Supplier}. A supplier that
 * returns {@code null} fails the request with an {@link EquinixClientException}.
 *
 * @author ianjones
 */
public final class SupplierBearerTokenProvider implements BearerTokenProvider {

    private final Supplier<BearerToken> supplier;

    /**
     * Creates a provider over a token supplier.
     *
     * @param supplier supplies a token on each call; must not be {@code null}
     */
    public SupplierBearerTokenProvider(Supplier<BearerToken> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "supplier");
    }

    /**
     * Calls the supplier.
     *
     * @return the supplied token
     * @throws EquinixClientException if the supplier returns {@code null}
     */
    @Override
    public BearerToken getToken() {
        BearerToken token = supplier.get();
        if (token == null) {
            throw new EquinixClientException("The bearer token supplier returned null.");
        }
        return token;
    }
}
