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
import api.equinix.javasdk.fabric.client.internal.RouteAggregationRuleClient;
import api.equinix.javasdk.fabric.model.RouteAggregationRule;
import api.equinix.javasdk.fabric.model.json.RouteAggregationRuleJson;
import api.equinix.javasdk.fabric.model.json.creators.RouteAggregationRuleCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.RouteAggregationRuleWrapper;

import java.util.List;
import java.util.Map;

public class RouteAggregationRuleClientImpl extends ResourceClientBase<RouteAggregationRule, RouteAggregationRuleJson> implements RouteAggregationRuleClient<RouteAggregationRule> {

    public RouteAggregationRuleClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "RouteAggregationRules", RouteAggregationRuleJson.class);
    }

    @Override
    protected RouteAggregationRule wrap(RouteAggregationRuleJson json) {
        return new RouteAggregationRuleWrapper(json, this);
    }

    public Page<RouteAggregationRule, RouteAggregationRuleJson> list(String routeAggregationId) {
        return listPagePath("GetRouteAggregationRules", Map.of("routeAggregationId", routeAggregationId));
    }

    public RouteAggregationRuleJson getByUuid(String routeAggregationId, String uuid) {
        return getOne("GetRouteAggregationRule", Map.of("routeAggregationId", routeAggregationId, "uuid", uuid));
    }

    public RouteAggregationRuleJson create(String routeAggregationId, RouteAggregationRuleCreatorJson routeAggregationRuleCreatorJson) {
        return postOne("PostRouteAggregationRule", Map.of("routeAggregationId", routeAggregationId), routeAggregationRuleCreatorJson);
    }

    public RouteAggregationRuleJson update(String routeAggregationId, String uuid, List<PatchOperation> operations) {
        return updateOne("UpdateRouteAggregationRule", Map.of("routeAggregationId", routeAggregationId, "uuid", uuid), operations);
    }

    public RouteAggregationRuleJson delete(String routeAggregationId, String uuid) {
        return deleteOne("DeleteRouteAggregationRule", Map.of("routeAggregationId", routeAggregationId, "uuid", uuid));
    }

    public RouteAggregationRuleJson refresh(String routeAggregationId, String uuid) {
        return getByUuid(routeAggregationId, uuid);
    }
}
