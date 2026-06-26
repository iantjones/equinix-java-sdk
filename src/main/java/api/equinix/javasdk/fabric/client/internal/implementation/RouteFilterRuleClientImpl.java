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
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.RouteFilterRuleClient;
import api.equinix.javasdk.fabric.model.RouteFilterRule;
import api.equinix.javasdk.fabric.model.json.RouteFilterRuleJson;
import api.equinix.javasdk.fabric.model.json.creators.RouteFilterRuleCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.RouteFilterRuleWrapper;

import java.util.Map;

public class RouteFilterRuleClientImpl extends ResourceClientBase<RouteFilterRule, RouteFilterRuleJson> implements RouteFilterRuleClient<RouteFilterRule> {

    public RouteFilterRuleClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "RouteFilterRules", RouteFilterRuleJson.class);
    }

    @Override
    protected RouteFilterRule wrap(RouteFilterRuleJson json) {
        return new RouteFilterRuleWrapper(json, this);
    }

    public Page<RouteFilterRule, RouteFilterRuleJson> list(String routeFilterId) {
        return listPagePath("GetRouteFilterRules", Map.of("routeFilterId", routeFilterId));
    }

    public RouteFilterRuleJson getByUuid(String routeFilterId, String uuid) {
        return getOne("GetRouteFilterRule", Map.of("routeFilterId", routeFilterId, "uuid", uuid));
    }

    public RouteFilterRuleJson create(String routeFilterId, RouteFilterRuleCreatorJson routeFilterRuleCreatorJson) {
        return postOne("PostRouteFilterRule", Map.of("routeFilterId", routeFilterId), routeFilterRuleCreatorJson);
    }

    public RouteFilterRuleJson delete(String routeFilterId, String uuid) {
        return deleteOne("DeleteRouteFilterRule", Map.of("routeFilterId", routeFilterId, "uuid", uuid));
    }

    public RouteFilterRuleJson refresh(String routeFilterId, String uuid) {
        return getByUuid(routeFilterId, uuid);
    }
}
