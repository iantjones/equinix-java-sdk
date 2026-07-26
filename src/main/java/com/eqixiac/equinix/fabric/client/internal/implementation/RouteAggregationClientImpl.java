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
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.model.FilteredSortedPaginatedPost;
import com.eqixiac.equinix.fabric.client.implementation.FabricConfigImpl;
import com.eqixiac.equinix.fabric.client.internal.RouteAggregationClient;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.RouteAggregation;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.ConnectionJson;
import com.eqixiac.equinix.fabric.model.json.RouteAggregationJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteAggregationCreatorJson;
import com.eqixiac.equinix.fabric.model.wrappers.ConnectionWrapper;
import com.eqixiac.equinix.fabric.model.wrappers.RouteAggregationWrapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RouteAggregationClientImpl extends ResourceClientBase<RouteAggregation, RouteAggregationJson> implements RouteAggregationClient<RouteAggregation> {

    private final FabricConfigImpl configClient;

    public RouteAggregationClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "RouteAggregations", RouteAggregationJson.class);
        this.configClient = configClient;
    }

    @Override
    protected RouteAggregation wrap(RouteAggregationJson json) {
        return new RouteAggregationWrapper(json, this);
    }

    public Page<RouteAggregationJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage("SearchRouteAggregations", new FilteredSortedPaginatedPost<>(filter, sort));
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

    public List<Change> getChanges(String uuid) {
        EquinixRequest<Change> request = buildRequestWithPathParams("GetRouteAggregationChanges", RequestType.PAGINATED,
                Map.of("uuid", uuid), Change.class);
        Page<Change> page = ResponseHandler.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public Change getChange(String uuid, String changeId) {
        return getAs("GetRouteAggregationChange", Map.of("uuid", uuid, "changeId", changeId), null, Change.class);
    }

    public List<Connection> getConnections(String uuid) {
        EquinixRequest<Connection> request = buildRequestWithPathParams("GetRouteAggregationConnections", RequestType.PAGINATED,
                Map.of("uuid", uuid), ConnectionJson.class);
        Page<ConnectionJson> page = ResponseHandler.handlePaginatedListResponse(invoke(request), request);
        if (page == null || page.getItems() == null) {
            return Collections.emptyList();
        }
        return page.getItems().stream()
                .map(json -> (Connection) new ConnectionWrapper(json, this.configClient.getConnectionsClient()))
                .collect(Collectors.toList());
    }
}
