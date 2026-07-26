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
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.SerializationHelper;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.model.FilteredSortedPaginatedPost;
import com.eqixiac.equinix.fabric.client.implementation.FabricConfigImpl;
import com.eqixiac.equinix.fabric.client.internal.RouteAggregationRuleClient;
import com.eqixiac.equinix.fabric.model.RouteAggregationRule;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.RouteAggregationRulesBulkRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.RouteAggregationRuleJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteAggregationRuleCreatorJson;
import com.eqixiac.equinix.fabric.model.wrappers.RouteAggregationRuleWrapper;

import java.util.Collections;
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

    public Page<RouteAggregationRuleJson> list(String routeAggregationId) {
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

    public RouteAggregationRuleJson replace(String routeAggregationId, String uuid, RouteAggregationRuleCreatorJson routeAggregationRuleCreatorJson) {
        // PUT /routeAggregations/{routeAggregationId}/routeAggregationRules/{uuid} replaces the rule's configuration.
        return updateOne("ReplaceRouteAggregationRule", Map.of("routeAggregationId", routeAggregationId, "uuid", uuid), routeAggregationRuleCreatorJson);
    }

    public List<RouteAggregationRuleJson> createBulk(String routeAggregationId, List<RouteAggregationRuleCreatorJson> routeAggregationRuleCreatorJsonList) {
        EquinixRequest<RouteAggregationRule> request = buildRequestWithPathParams("PostRouteAggregationRulesBulk", RequestType.PAGINATED_POST,
                Map.of("routeAggregationId", routeAggregationId), RouteAggregationRuleJson.class);
        SerializationHelper.serializeJson(request, new RouteAggregationRulesBulkRequest(routeAggregationRuleCreatorJsonList));
        Page<RouteAggregationRuleJson> page = ResponseHandler.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public Page<RouteAggregationRuleJson> search(String routeAggregationId, FilterPropertyList filter, SortPropertyList sort) {
        // POST /routeAggregations/{routeAggregationId}/routeAggregationRules/search — searchPage cannot carry
        // path params, so build the paginated-post request manually (mirrors RouteAggregation.search otherwise).
        EquinixRequest<RouteAggregationRule> request = buildRequestWithPathParams("SearchRouteAggregationRules", RequestType.PAGINATED_POST,
                Map.of("routeAggregationId", routeAggregationId), RouteAggregationRuleJson.class);
        SerializationHelper.serializeJson(request, new FilteredSortedPaginatedPost<>(filter, sort));
        return ResponseHandler.handlePaginatedListResponse(invoke(request), request);
    }

    public List<Change> getChanges(String routeAggregationId, String uuid) {
        EquinixRequest<Change> request = buildRequestWithPathParams("GetRouteAggregationRuleChanges", RequestType.PAGINATED,
                Map.of("routeAggregationId", routeAggregationId, "uuid", uuid), Change.class);
        Page<Change> page = ResponseHandler.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public Change getChange(String routeAggregationId, String uuid, String changeId) {
        return getAs("GetRouteAggregationRuleChange",
                Map.of("routeAggregationId", routeAggregationId, "uuid", uuid, "changeId", changeId), null, Change.class);
    }
}
