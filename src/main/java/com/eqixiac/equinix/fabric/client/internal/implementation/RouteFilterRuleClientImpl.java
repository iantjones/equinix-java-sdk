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
import com.eqixiac.equinix.fabric.client.internal.RouteFilterRuleClient;
import com.eqixiac.equinix.fabric.model.RouteFilterRule;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.RouteFilterRulesBulkRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.RouteFilterRuleJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteFilterRuleCreatorJson;
import com.eqixiac.equinix.fabric.model.wrappers.RouteFilterRuleWrapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RouteFilterRuleClientImpl extends ResourceClientBase<RouteFilterRule, RouteFilterRuleJson> implements RouteFilterRuleClient<RouteFilterRule> {

    public RouteFilterRuleClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "RouteFilterRules", RouteFilterRuleJson.class);
    }

    @Override
    protected RouteFilterRule wrap(RouteFilterRuleJson json) {
        return new RouteFilterRuleWrapper(json, this);
    }

    public Page<RouteFilterRuleJson> list(String routeFilterId) {
        return listPagePath("GetRouteFilterRules", Map.of("routeFilterId", routeFilterId));
    }

    public RouteFilterRuleJson getByUuid(String routeFilterId, String uuid) {
        return getOne("GetRouteFilterRule", Map.of("routeFilterId", routeFilterId, "uuid", uuid));
    }

    public RouteFilterRuleJson create(String routeFilterId, RouteFilterRuleCreatorJson routeFilterRuleCreatorJson) {
        return postOne("PostRouteFilterRule", Map.of("routeFilterId", routeFilterId), routeFilterRuleCreatorJson);
    }

    public RouteFilterRuleJson update(String routeFilterId, String uuid, List<PatchOperation> operations) {
        // PATCH /routeFilters/{routeFilterId}/routeFilterRules/{uuid} with an op/path/value array sent
        // as application/json (not json-patch+json), so updateOne (default content-type) is correct here.
        return updateOne("UpdateRouteFilterRule", Map.of("routeFilterId", routeFilterId, "uuid", uuid), operations);
    }

    public RouteFilterRuleJson delete(String routeFilterId, String uuid) {
        return deleteOne("DeleteRouteFilterRule", Map.of("routeFilterId", routeFilterId, "uuid", uuid));
    }

    public RouteFilterRuleJson refresh(String routeFilterId, String uuid) {
        return getByUuid(routeFilterId, uuid);
    }

    public RouteFilterRuleJson replace(String routeFilterId, String uuid, RouteFilterRuleCreatorJson routeFilterRuleCreatorJson) {
        // PUT /routeFilters/{routeFilterId}/routeFilterRules/{uuid} replaces the rule's configuration.
        return updateOne("ReplaceRouteFilterRule", Map.of("routeFilterId", routeFilterId, "uuid", uuid), routeFilterRuleCreatorJson);
    }

    public List<RouteFilterRuleJson> createBulk(String routeFilterId, List<RouteFilterRuleCreatorJson> routeFilterRuleCreatorJsonList) {
        EquinixRequest<RouteFilterRule> request = buildRequestWithPathParams("PostRouteFilterRulesBulk", RequestType.PAGINATED_POST,
                Map.of("routeFilterId", routeFilterId), RouteFilterRuleJson.class);
        SerializationHelper.serializeJson(request, new RouteFilterRulesBulkRequest(routeFilterRuleCreatorJsonList));
        Page<RouteFilterRuleJson> page = ResponseHandler.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public Page<RouteFilterRuleJson> search(String routeFilterId, FilterPropertyList filter, SortPropertyList sort) {
        // POST /routeFilters/{routeFilterId}/routeFilterRules/search — searchPage cannot carry path
        // params, so build the paginated-post request manually (mirrors RouteFilter.search otherwise).
        EquinixRequest<RouteFilterRule> request = buildRequestWithPathParams("SearchRouteFilterRules", RequestType.PAGINATED_POST,
                Map.of("routeFilterId", routeFilterId), RouteFilterRuleJson.class);
        SerializationHelper.serializeJson(request, new FilteredSortedPaginatedPost<>(filter, sort));
        return ResponseHandler.handlePaginatedListResponse(invoke(request), request);
    }

    public List<Change> getChanges(String routeFilterId, String uuid) {
        EquinixRequest<Change> request = buildRequestWithPathParams("GetRouteFilterRuleChanges", RequestType.PAGINATED,
                Map.of("routeFilterId", routeFilterId, "uuid", uuid), Change.class);
        Page<Change> page = ResponseHandler.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public Change getChange(String routeFilterId, String uuid, String changeId) {
        return getAs("GetRouteFilterRuleChange",
                Map.of("routeFilterId", routeFilterId, "uuid", uuid, "changeId", changeId), null, Change.class);
    }
}
