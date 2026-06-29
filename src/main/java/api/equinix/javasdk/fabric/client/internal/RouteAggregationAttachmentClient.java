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

package api.equinix.javasdk.fabric.client.internal;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.fabric.model.RouteAggregationAttachment;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.RouteAggregationAttachmentJson;

import java.util.List;

/**
 * Internal client for Fabric Route Aggregation attachments. The same attachment shape is returned
 * by the connection route-aggregation endpoints and by the cloud-router route-aggregation
 * attachment search.
 */
public interface RouteAggregationAttachmentClient<T> extends PageablePost<T> {

    List<RouteAggregationAttachment> getConnectionRouteAggregations(String connectionId);

    RouteAggregationAttachmentJson getConnectionRouteAggregation(String connectionId, String routeAggregationId);

    RouteAggregationAttachmentJson attachConnectionRouteAggregation(String connectionId, String routeAggregationId);

    boolean detachConnectionRouteAggregation(String connectionId, String routeAggregationId);

    Page<RouteAggregationAttachment, RouteAggregationAttachmentJson> searchCloudRouterAttachments(String routerId, FilterPropertyList filter, SortPropertyList sort);
}
