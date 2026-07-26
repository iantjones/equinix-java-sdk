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

package com.eqixiac.equinix.internetaccess.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.internetaccess.client.implementation.InternetAccessConfigImpl;
import com.eqixiac.equinix.internetaccess.client.internal.PriceClient;
import com.eqixiac.equinix.internetaccess.model.Price;
import com.eqixiac.equinix.internetaccess.model.json.PriceJson;
import com.eqixiac.equinix.internetaccess.model.json.creators.PriceSearchRequest;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1 price search
 * {@code POST /internetAccess/v1/prices/search}. The {@code Price} response is read-only, so the
 * deserialized {@link PriceJson} (which implements {@link Price} directly) is returned without a
 * wrapper.
 */
public class PriceClientImpl extends ResourceClientBase<Price, PriceJson> implements PriceClient {

    public PriceClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "Prices", PriceJson.class);
    }

    @Override
    protected Price wrap(PriceJson json) {
        return json;
    }

    public Page<PriceJson> search(PriceSearchRequest searchRequest) {
        // internetaccessv1 paginates the prices search via offset/limit QUERY PARAMETERS (the
        // FilterBody carries only the filter), so this builds a query-paginated request: the
        // shared paging pipeline advances the query offset between pages, re-sending the same body.
        return searchPageQueryPaginated("SearchPrices", searchRequest);
    }
}
