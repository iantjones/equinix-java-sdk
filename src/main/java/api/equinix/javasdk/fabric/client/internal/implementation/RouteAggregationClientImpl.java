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
import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.RouteAggregationClient;
import api.equinix.javasdk.fabric.model.RouteAggregation;
import api.equinix.javasdk.fabric.model.json.RouteAggregationJson;
import api.equinix.javasdk.fabric.model.json.creators.RouteAggregationCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.RouteAggregationWrapper;

import java.util.List;

public class RouteAggregationClientImpl extends ResourceClientBase<RouteAggregation, RouteAggregationJson> implements RouteAggregationClient<RouteAggregation> {

    public RouteAggregationClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "RouteAggregations", RouteAggregationJson.class);
    }

    @Override
    protected RouteAggregation wrap(RouteAggregationJson json) {
        return new RouteAggregationWrapper(json, this);
    }

    public Page<RouteAggregation, RouteAggregationJson> list() {
        return listPage("GetRouteAggregations");
    }

    public RouteAggregationJson getByUuid(String uuid) {
        return getOne("GetRouteAggregation", uuid);
    }

    public RouteAggregationJson create(RouteAggregationCreatorJson routeAggregationCreatorJson) {
        return postOne("PostRouteAggregation", routeAggregationCreatorJson);
    }

    public RouteAggregationJson update(String uuid, List<PatchOperation> operations) {
        return updateOne("UpdateRouteAggregation", uuid, operations);
    }

    public RouteAggregationJson delete(String uuid) {
        return deleteOne("DeleteRouteAggregation", uuid);
    }

    public RouteAggregationJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
