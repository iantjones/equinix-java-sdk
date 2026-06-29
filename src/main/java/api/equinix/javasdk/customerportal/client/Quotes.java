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

package api.equinix.javasdk.customerportal.client;

import api.equinix.javasdk.customerportal.model.Quote;

import java.util.List;

/**
 * Client interface for retrieving quotes in the Equinix Customer Portal.
 *
 * <p>Backed by the Quotes v2 API at {@code /v2/quotes/{quoteId}}. The API exposes only retrieval
 * of an individual quote by id (there is no collection, create or refresh endpoint).</p>
 */
public interface Quotes {

    /**
     * Retrieves a specific quote by its identifier.
     *
     * <p>Maps to {@code GET /v2/quotes/{quoteId}} ({@code Retrieve a quote}).</p>
     *
     * @param quoteId the identifier of the quote
     * @return the matching quote
     */
    Quote getByUuid(String quoteId);

    /**
     * Retrieves a specific quote by its identifier, scoped to the supplied IBXs.
     *
     * <p>Maps to {@code GET /v2/quotes/{quoteId}} ({@code Retrieve a quote}) with the {@code ibxs}
     * query parameter.</p>
     *
     * @param quoteId the identifier of the quote
     * @param ibxs    the IBX codes to scope the quote to, or {@code null}/empty for all
     * @return the matching quote
     */
    Quote getByUuid(String quoteId, List<String> ibxs);
}
