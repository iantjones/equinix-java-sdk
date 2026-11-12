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

package com.eqixiac.equinix.fabric.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.http.response.Page;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.eqixiac.equinix.fabric.client.implementation.FabricConfigImpl;
import com.eqixiac.equinix.fabric.client.internal.PricingClient;
import com.eqixiac.equinix.fabric.model.Pricing;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.json.PricingJson;
import com.eqixiac.equinix.fabric.model.wrappers.PricingWrapper;

public class PricingClientImpl extends ResourceClientBase<Pricing, PricingJson> implements PricingClient<Pricing> {

    public PricingClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Pricing", PricingJson.class);
    }

    @Override
    protected Pricing wrap(PricingJson json) {
        return new PricingWrapper(json, this);
    }

    /**
     * {@code POST /fabric/v4/prices/search} with a {@code {"filter": ...}} body only. The spec's
     * {@code FilterBody} declares no {@code pagination}, and the live service ignores one: with
     * {@code pagination.offset=1, limit=1} it still answered {@code offset 0} (observed 2026-11-11),
     * so the response is a single page.
     */
    public Page<PricingJson> list(FilterPropertyList filter) {
        return searchPage("GetPricing", new PriceSearchBody(filter));
    }

    /** The spec's {@code FilterBody}: just {@code filter}. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static final class PriceSearchBody {
        @JsonProperty("filter")
        private final FilterPropertyList filter;

        PriceSearchBody(FilterPropertyList filter) {
            this.filter = filter;
        }

        public FilterPropertyList getFilter() {
            return filter;
        }
    }
}
