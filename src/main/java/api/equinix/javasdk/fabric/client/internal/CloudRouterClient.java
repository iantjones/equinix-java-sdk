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

import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.model.CloudRouter;
import api.equinix.javasdk.fabric.model.CloudRouterAction;
import api.equinix.javasdk.fabric.model.RouteAggregationAttachment;
import api.equinix.javasdk.fabric.model.RouteFilterAttachment;
import api.equinix.javasdk.fabric.model.RoutingProtocolValidation;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.CloudRouterJson;
import api.equinix.javasdk.fabric.model.json.creators.CloudRouterCreatorJson;

import java.util.List;

public interface CloudRouterClient<T> extends PageablePost<T> {

    Page<CloudRouter, CloudRouterJson> search(FilterPropertyList filter, SortPropertyList sort);

    CloudRouterJson getByUuid(String uuid);

    CloudRouterJson create(CloudRouterCreatorJson cloudRouterCreatorJson);

    CloudRouterJson update(String uuid, List<PatchOperation> operations);

    CloudRouterJson delete(String uuid);

    CloudRouterJson refresh(String uuid);

    RoutingProtocolValidation validateRoutingProtocol(String routerId, FilterPropertyList filter);

    List<CloudRouterAction> getActions(String routerId);

    CloudRouterAction getAction(String routerId, String uuid);

    PaginatedFilteredList<CloudRouterAction> searchActions(String routerId, FilterPropertyList filter, SortPropertyList sort);

    PaginatedFilteredList<RouteFilterAttachment> searchRouteFilterAttachments(String routerId, FilterPropertyList filter, SortPropertyList sort);

    PaginatedFilteredList<RouteAggregationAttachment> searchRouteAggregationAttachments(String routerId, FilterPropertyList filter, SortPropertyList sort);
}
