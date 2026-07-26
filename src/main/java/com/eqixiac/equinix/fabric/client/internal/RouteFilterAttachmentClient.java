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

package com.eqixiac.equinix.fabric.client.internal;

import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PageablePost;
import com.eqixiac.equinix.fabric.enums.Direction;
import com.eqixiac.equinix.fabric.model.RouteFilterAttachment;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.RouteFilterAttachmentJson;

import java.util.List;

/**
 * Internal client for Fabric Route Filter attachments. The same attachment shape is returned by
 * the connection route-filter endpoints and by the cloud-router route-filter attachment search.
 */
public interface RouteFilterAttachmentClient<T> extends PageablePost<T> {

    List<RouteFilterAttachment> getConnectionRouteFilters(String connectionId);

    RouteFilterAttachmentJson getConnectionRouteFilter(String connectionId, String routeFilterId);

    RouteFilterAttachmentJson attachConnectionRouteFilter(String connectionId, String routeFilterId, Direction direction);

    boolean detachConnectionRouteFilter(String connectionId, String routeFilterId);

    Page<RouteFilterAttachmentJson> searchCloudRouterAttachments(String routerId, FilterPropertyList filter, SortPropertyList sort);
}
