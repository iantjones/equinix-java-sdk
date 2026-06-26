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

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.FilteredSortedPaginatedPost;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.FabricGatewayClient;
import api.equinix.javasdk.fabric.model.FabricGateway;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.FabricGatewayJson;
import api.equinix.javasdk.fabric.model.wrappers.FabricGatewayWrapper;

public class FabricGatewayClientImpl extends ResourceClientBase<FabricGateway, FabricGatewayJson> implements FabricGatewayClient<FabricGateway> {

    public FabricGatewayClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "FabricGateways", FabricGatewayJson.class);
    }

    @Override
    protected FabricGateway wrap(FabricGatewayJson json) {
        return new FabricGatewayWrapper(json, this);
    }

    public Page<FabricGateway, FabricGatewayJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage("SearchFabricGateways", new FilteredSortedPaginatedPost<>(filter, sort));
    }

    public FabricGatewayJson getByUuid(String uuid) {
        return getOne("GetFabricGateway", uuid);
    }

    public FabricGatewayJson delete(String uuid) {
        return deleteOne("DeleteFabricGateway", uuid);
    }

    public FabricGatewayJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
