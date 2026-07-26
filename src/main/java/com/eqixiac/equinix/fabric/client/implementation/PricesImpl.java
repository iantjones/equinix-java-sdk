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

package com.eqixiac.equinix.fabric.client.implementation;

import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.client.Prices;
import com.eqixiac.equinix.fabric.client.internal.PricingClient;
import com.eqixiac.equinix.fabric.model.Pricing;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.json.PricingJson;
import com.eqixiac.equinix.fabric.model.wrappers.PricingWrapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PricesImpl implements Prices {

    private final PricingClient<Pricing> serviceClient;

    public PaginatedFilteredList<Pricing> list(FilterPropertyList filter) {
        Page<PricingJson> responsePage = serviceClient.list(filter);
        PaginatedFilteredList<Pricing> PricingList = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, PricingWrapper::new);
        return new PaginatedFilteredList<>(PricingList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }
}
