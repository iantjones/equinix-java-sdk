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
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.SerializationHelper;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.FilteredSortedPaginatedPost;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.RouteTableEntryClient;
import api.equinix.javasdk.fabric.model.RouteTableEntry;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.RouteTableEntryJson;

import java.util.Map;

/**
 * Internal client for searching read-only route table entries. The JSON model implements the
 * public interface directly, so {@link #wrap(RouteTableEntryJson)} is the identity.
 *
 * <p>The route-search endpoints live under two different resource parents in {@code apiParams}
 * ({@code Connections} for advertised/received routes and {@code CloudRouters} for the Fabric
 * Cloud Router route table), so a single instance is constructed per parent.</p>
 *
 * @author ianjones
 */
public class RouteTableEntryClientImpl extends ResourceClientBase<RouteTableEntry, RouteTableEntryJson> implements RouteTableEntryClient<RouteTableEntry> {

    public RouteTableEntryClientImpl(FabricConfigImpl configClient, String requestParent) {
        super(configClient, "Fabric", requestParent, RouteTableEntryJson.class);
    }

    @Override
    protected RouteTableEntry wrap(RouteTableEntryJson json) {
        return json;
    }

    public Page<RouteTableEntryJson> searchAdvertisedRoutes(String connectionId, FilterPropertyList filter, SortPropertyList sort) {
        return searchUnder("SearchAdvertisedRoutes", Map.of("uuid", connectionId), filter, sort);
    }

    public Page<RouteTableEntryJson> searchReceivedRoutes(String connectionId, FilterPropertyList filter, SortPropertyList sort) {
        return searchUnder("SearchReceivedRoutes", Map.of("uuid", connectionId), filter, sort);
    }

    public Page<RouteTableEntryJson> searchCloudRouterRoutes(String routerId, FilterPropertyList filter, SortPropertyList sort) {
        return searchUnder("SearchCloudRouterRoutes", Map.of("uuid", routerId), filter, sort);
    }

    private Page<RouteTableEntryJson> searchUnder(String serviceEndpoint, Map<String, String> pathParams,
                                                                  FilterPropertyList filter, SortPropertyList sort) {
        EquinixRequest<RouteTableEntry> request = buildRequestWithPathParams(serviceEndpoint, RequestType.PAGINATED_POST, pathParams, RouteTableEntryJson.class);
        SerializationHelper.serializeJson(request, new FilteredSortedPaginatedPost<>(filter, sort));
        return ResponseHandler.handlePaginatedListResponse(invoke(request), request);
    }
}
