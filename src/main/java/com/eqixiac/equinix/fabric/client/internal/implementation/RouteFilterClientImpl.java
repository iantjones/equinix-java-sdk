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
import com.eqixiac.equinix.fabric.client.internal.RouteFilterClient;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.RouteFilter;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.ConnectionJson;
import com.eqixiac.equinix.fabric.model.json.RouteFilterJson;
import com.eqixiac.equinix.fabric.model.json.creators.RouteFilterCreatorJson;
import com.eqixiac.equinix.fabric.model.wrappers.ConnectionWrapper;
import com.eqixiac.equinix.fabric.model.wrappers.RouteFilterWrapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RouteFilterClientImpl extends ResourceClientBase<RouteFilter, RouteFilterJson> implements RouteFilterClient<RouteFilter> {

    private final FabricConfigImpl configClient;

    public RouteFilterClientImpl(FabricConfigImpl configClient) {
        // "RouteFilter" drives the derived endpoint names (SearchRouteFilters, GetRouteFilter,
        // DeleteRouteFilter).
        super(configClient, "Fabric", "RouteFilters", RouteFilterJson.class, "RouteFilter");
        this.configClient = configClient;
    }

    @Override
    protected RouteFilter wrap(RouteFilterJson json) {
        return new RouteFilterWrapper(json, this);
    }

    public Page<RouteFilterJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage(new FilteredSortedPaginatedPost<>(filter, sort));
    }

    public RouteFilterJson getByUuid(String uuid) {
        return getOneByUuid(uuid);
    }

    public RouteFilterJson create(RouteFilterCreatorJson routeFilterCreatorJson) {
        return postOne("PostRouteFilter", routeFilterCreatorJson);
    }

    public RouteFilterJson update(String uuid, List<PatchOperation> operations) {
        // PATCH /routeFilters/{uuid} with an op/path/value array sent as application/json
        // (not json-patch+json), so updateOne (default content-type) is correct here.
        return updateOne("UpdateRouteFilter", uuid, operations);
    }

    public RouteFilterJson delete(String uuid) {
        return deleteOneByUuid(uuid);
    }

    public RouteFilterJson refresh(String uuid) {
        return getByUuid(uuid);
    }

    public List<Change> getChanges(String uuid) {
        EquinixRequest<Change> request = buildRequestWithPathParams("GetRouteFilterChanges", RequestType.PAGINATED,
                Map.of("uuid", uuid), Change.class);
        Page<Change> page = ResponseHandler.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public Change getChange(String uuid, String changeId) {
        return getAs("GetRouteFilterChange", Map.of("uuid", uuid, "changeId", changeId), null, Change.class);
    }

    public List<Connection> getConnections(String uuid) {
        EquinixRequest<Connection> request = buildRequestWithPathParams("GetRouteFilterConnections", RequestType.PAGINATED,
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
