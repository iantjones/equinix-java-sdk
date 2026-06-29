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

package api.equinix.javasdk.internetaccess.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.enums.TermsProduct;
import api.equinix.javasdk.internetaccess.enums.TermsType;
import api.equinix.javasdk.internetaccess.model.TermsAndConditions;

/**
 * Client interface for the Equinix Internet Access (EIA) v1 terms-and-conditions lookup
 * ({@code GET /internetAccess/v1/terms}) — the terms and conditions applicable to an account at a
 * given IBX for a given product.
 */
public interface InternetAccessTerms {

    /**
     * Returns the terms and conditions applicable to the given account, IBX and product.
     *
     * @param accountNumber the Equinix account number
     * @param ibx the IBX data center code (e.g. {@code WA1})
     * @param product the Equinix product ({@code IA_C}, {@code IA_VC} or {@code MC_C})
     * @return a paginated list of matching terms and conditions
     */
    PaginatedList<TermsAndConditions> list(String accountNumber, String ibx, TermsProduct product);

    /**
     * Returns the terms and conditions applicable to the given account, IBX and product, optionally
     * narrowed by terms category and language.
     *
     * @param accountNumber the Equinix account number
     * @param ibx the IBX data center code (e.g. {@code WA1})
     * @param product the Equinix product ({@code IA_C}, {@code IA_VC} or {@code MC_C})
     * @param type the terms category ({@code TERMS_AND_CONDITIONS}, {@code RENEWAL_TERMS} or
     *             {@code PRICE_INCREASE_TERMS}), or {@code null} for all categories
     * @param language the language to return the terms in, or {@code null} for the default
     * @return a paginated list of matching terms and conditions
     */
    PaginatedList<TermsAndConditions> list(String accountNumber, String ibx, TermsProduct product, TermsType type, String language);
}
