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
import com.eqixiac.equinix.core.model.FilteredPaginatedPost;
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

    public Page<PricingJson> list(FilterPropertyList filter) {
        return searchPage("GetPricing", new FilteredPaginatedPost<>(filter));
    }
}
