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

package api.equinix.javasdk.fabric.client.internal.implementation;

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedPostRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.FilteredPaginatedPost;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.PricingClient;
import api.equinix.javasdk.fabric.model.Pricing;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.json.PricingJson;
import api.equinix.javasdk.fabric.model.wrappers.PricingWrapper;

public class PricingClientImpl extends PageableBase implements PricingClient<Pricing> {

    public PricingClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Pricing");
    }

    public Page<Pricing, PricingJson> list(FilterPropertyList filter) {
        EquinixRequest<Pricing> equinixRequest = this.buildRequest("GetPricing", RequestType.PAGINATED_POST, PricingJson.getPagedTypeRef());
        Utils.serializeJson(equinixRequest, new FilteredPaginatedPost(filter));
        EquinixResponse<Pricing> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<Pricing> nextPage(PaginatedRequest<Pricing> equinixRequest) {
        EquinixResponse<Pricing> equinixResponse = this.invoke(equinixRequest);
        Page<Pricing, PricingJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Pricing> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, PricingWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }

    public PaginatedFilteredList<Pricing> nextPage(PaginatedPostRequest<Pricing> equinixRequest) {
        Utils.serializeJson(equinixRequest, equinixRequest.getObjectToSerialize());
        EquinixResponse<Pricing> equinixResponse = this.invoke(equinixRequest);
        Page<Pricing, PricingJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedFilteredList<Pricing> newPaginatedFilteredList = Utils.mapPaginatedFilteredList(nextPage.getItems(), this, PricingWrapper::new);
        return new PaginatedFilteredList<>(newPaginatedFilteredList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
