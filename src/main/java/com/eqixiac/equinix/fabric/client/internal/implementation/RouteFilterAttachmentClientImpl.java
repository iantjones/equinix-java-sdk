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
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.model.FilteredSortedPaginatedPost;
import com.eqixiac.equinix.fabric.client.implementation.FabricConfigImpl;
import com.eqixiac.equinix.fabric.client.internal.RouteFilterAttachmentClient;
import com.eqixiac.equinix.fabric.enums.Direction;
import com.eqixiac.equinix.fabric.model.RouteFilterAttachment;
import com.eqixiac.equinix.fabric.model.implementation.RouteFilterAttachmentRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.RouteFilterAttachmentJson;

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
        Page<RouteFilterAttachmentJson> page = ResponseHandler.handlePaginatedListResponse(invoke(request), request);
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

    public Page<RouteFilterAttachmentJson> searchCloudRouterAttachments(String routerId, FilterPropertyList filter, SortPropertyList sort) {
        EquinixRequest<RouteFilterAttachment> request = buildRequestWithPathParams("SearchCloudRouterRouteFilterAttachments", RequestType.PAGINATED_POST,
                Map.of("routerId", routerId), RouteFilterAttachmentJson.class);
        SerializationHelper.serializeJson(request, new FilteredSortedPaginatedPost<>(filter, sort));
        return ResponseHandler.handlePaginatedListResponse(invoke(request), request);
    }
}
