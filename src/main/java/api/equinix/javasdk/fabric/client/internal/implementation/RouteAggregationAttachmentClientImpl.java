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
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.FilteredSortedPaginatedPost;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.RouteAggregationAttachmentClient;
import api.equinix.javasdk.fabric.model.RouteAggregationAttachment;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.RouteAggregationAttachmentJson;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Internal client for Fabric Route Aggregation attachments. The connection-scoped get / attach /
 * detach operations live under the {@code Connections} resource parent, while the cloud-router
 * attachment search lives under the {@code CloudRouters} parent, so an instance is constructed per
 * parent (the same approach as {@link RouteTableEntryClientImpl}). The JSON model implements the
 * public interface directly, so {@link #wrap(RouteAggregationAttachmentJson)} is the identity.
 *
 * @author ianjones
 */
public class RouteAggregationAttachmentClientImpl extends ResourceClientBase<RouteAggregationAttachment, RouteAggregationAttachmentJson> implements RouteAggregationAttachmentClient<RouteAggregationAttachment> {

    public RouteAggregationAttachmentClientImpl(FabricConfigImpl configClient, String requestParent) {
        super(configClient, "Fabric", requestParent, RouteAggregationAttachmentJson.class);
    }

    @Override
    protected RouteAggregationAttachment wrap(RouteAggregationAttachmentJson json) {
        return json;
    }

    public List<RouteAggregationAttachment> getConnectionRouteAggregations(String connectionId) {
        EquinixRequest<RouteAggregationAttachment> request = buildRequestWithPathParams("GetConnectionRouteAggregations", RequestType.PAGINATED,
                Map.of("connectionId", connectionId), RouteAggregationAttachmentJson.class);
        Page<RouteAggregationAttachment, RouteAggregationAttachmentJson> page = Utils.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public RouteAggregationAttachmentJson getConnectionRouteAggregation(String connectionId, String routeAggregationId) {
        return getOne("GetConnectionRouteAggregation", Map.of("connectionId", connectionId, "routeAggregationId", routeAggregationId));
    }

    public RouteAggregationAttachmentJson attachConnectionRouteAggregation(String connectionId, String routeAggregationId) {
        // PUT with no request body, so build the request directly rather than via updateOne
        // (which would serialize a literal "null" body).
        EquinixRequest<RouteAggregationAttachmentJson> request = buildRequestWithPathParams("AttachConnectionRouteAggregation",
                RequestType.SINGLE, Map.of("connectionId", connectionId, "routeAggregationId", routeAggregationId), RouteAggregationAttachmentJson.class);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    public boolean detachConnectionRouteAggregation(String connectionId, String routeAggregationId) {
        return booleanOp("DetachConnectionRouteAggregation", RequestType.SINGLE,
                Map.of("connectionId", connectionId, "routeAggregationId", routeAggregationId), null, null);
    }

    public Page<RouteAggregationAttachment, RouteAggregationAttachmentJson> searchCloudRouterAttachments(String routerId, FilterPropertyList filter, SortPropertyList sort) {
        EquinixRequest<RouteAggregationAttachment> request = buildRequestWithPathParams("SearchCloudRouterRouteAggregationAttachments", RequestType.PAGINATED_POST,
                Map.of("routerId", routerId), RouteAggregationAttachmentJson.class);
        Utils.serializeJson(request, new FilteredSortedPaginatedPost<>(filter, sort));
        return Utils.handlePaginatedListResponse(invoke(request), request);
    }
}
