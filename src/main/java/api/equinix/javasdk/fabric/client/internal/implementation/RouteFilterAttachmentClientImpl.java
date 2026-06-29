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
import api.equinix.javasdk.fabric.client.internal.RouteFilterAttachmentClient;
import api.equinix.javasdk.fabric.enums.Direction;
import api.equinix.javasdk.fabric.model.RouteFilterAttachment;
import api.equinix.javasdk.fabric.model.implementation.RouteFilterAttachmentRequest;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.RouteFilterAttachmentJson;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Internal client for Fabric Route Filter attachments. The connection-scoped get / attach / detach
 * operations live under the {@code Connections} resource parent, while the cloud-router attachment
 * search lives under the {@code CloudRouters} parent, so an instance is constructed per parent (the
 * same approach as {@link RouteTableEntryClientImpl}). The JSON model implements the public
 * interface directly, so {@link #wrap(RouteFilterAttachmentJson)} is the identity.
 *
 * @author ianjones
 */
public class RouteFilterAttachmentClientImpl extends ResourceClientBase<RouteFilterAttachment, RouteFilterAttachmentJson> implements RouteFilterAttachmentClient<RouteFilterAttachment> {

    public RouteFilterAttachmentClientImpl(FabricConfigImpl configClient, String requestParent) {
        super(configClient, "Fabric", requestParent, RouteFilterAttachmentJson.class);
    }

    @Override
    protected RouteFilterAttachment wrap(RouteFilterAttachmentJson json) {
        return json;
    }

    public List<RouteFilterAttachment> getConnectionRouteFilters(String connectionId) {
        EquinixRequest<RouteFilterAttachment> request = buildRequestWithPathParams("GetConnectionRouteFilters", RequestType.PAGINATED,
                Map.of("connectionId", connectionId), RouteFilterAttachmentJson.class);
        Page<RouteFilterAttachment, RouteFilterAttachmentJson> page = Utils.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public RouteFilterAttachmentJson getConnectionRouteFilter(String connectionId, String routeFilterId) {
        return getOne("GetConnectionRouteFilter", Map.of("connectionId", connectionId, "routeFilterId", routeFilterId));
    }

    public RouteFilterAttachmentJson attachConnectionRouteFilter(String connectionId, String routeFilterId, Direction direction) {
        return updateOne("AttachConnectionRouteFilter", Map.of("connectionId", connectionId, "routeFilterId", routeFilterId),
                new RouteFilterAttachmentRequest(direction));
    }

    public boolean detachConnectionRouteFilter(String connectionId, String routeFilterId) {
        return booleanOp("DetachConnectionRouteFilter", RequestType.SINGLE,
                Map.of("connectionId", connectionId, "routeFilterId", routeFilterId), null, null);
    }

    public Page<RouteFilterAttachment, RouteFilterAttachmentJson> searchCloudRouterAttachments(String routerId, FilterPropertyList filter, SortPropertyList sort) {
        EquinixRequest<RouteFilterAttachment> request = buildRequestWithPathParams("SearchCloudRouterRouteFilterAttachments", RequestType.PAGINATED_POST,
                Map.of("routerId", routerId), RouteFilterAttachmentJson.class);
        Utils.serializeJson(request, new FilteredSortedPaginatedPost<>(filter, sort));
        return Utils.handlePaginatedListResponse(invoke(request), request);
    }
}
