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

package api.equinix.javasdk.fabric.client.implementation;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.client.Prices;
import api.equinix.javasdk.fabric.client.internal.PricingClient;
import api.equinix.javasdk.fabric.model.Pricing;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.json.PricingJson;
import api.equinix.javasdk.fabric.model.wrappers.PricingWrapper;

public class PricesImpl implements Prices {

    private final Fabric serviceManager;

    private final PricingClient<Pricing> serviceClient;

    public PricesImpl(PricingClient<Pricing> serviceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedFilteredList<Pricing> list(FilterPropertyList filter) {
        Page<Pricing, PricingJson> responsePage = serviceClient.list(filter);
        PaginatedFilteredList<Pricing> PricingList = Utils.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, PricingWrapper::new);
        return new PaginatedFilteredList<>(PricingList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }
}
