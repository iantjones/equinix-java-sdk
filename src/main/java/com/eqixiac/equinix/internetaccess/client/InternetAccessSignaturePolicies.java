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

package com.eqixiac.equinix.internetaccess.client;

import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.internetaccess.model.SignaturePolicy;

/**
 * Client interface for the Equinix Internet Access (EIA) v1 signature-policies lookup
 * ({@code GET /internetAccess/v1/signaturePolicies}) — the signature policies governing order
 * acceptance.
 */
public interface InternetAccessSignaturePolicies {

    /**
     * Returns the signature policies governing order acceptance across all countries.
     *
     * @return a paginated list of signature policies
     */
    PaginatedList<SignaturePolicy> list();

    /**
     * Returns the signature policies governing order acceptance, narrowed to the given country.
     *
     * @param countryCode the two-letter ISO country code, or {@code null} for all countries
     * @return a paginated list of matching signature policies
     */
    PaginatedList<SignaturePolicy> list(String countryCode);
}
