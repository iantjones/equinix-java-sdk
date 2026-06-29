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

import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.internetaccess.model.Price;
import api.equinix.javasdk.internetaccess.model.json.creators.PriceSearchRequest;

/**
 * Client interface for the Equinix Internet Access (EIA) v1 price search
 * ({@code POST /internetAccess/v1/prices/search}). This is a v1-only surface with no v2
 * equivalent: it returns the prices of EIA products and IP blocks matching the supplied criteria.
 */
public interface InternetAccessPrices {

    /**
     * Searches for Equinix Internet Access prices matching the specified criteria. Currency
     * depends on the billing account.
     *
     * @param searchRequest the price search filter criteria
     * @return a paginated, filtered list of matching prices
     */
    PaginatedFilteredList<Price> search(PriceSearchRequest searchRequest);
}
