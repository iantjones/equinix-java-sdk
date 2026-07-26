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

import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PageablePost;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.model.CloudRouter;
import com.eqixiac.equinix.fabric.model.CloudRouterAction;
import com.eqixiac.equinix.fabric.model.RouteAggregationAttachment;
import com.eqixiac.equinix.fabric.model.RouteFilterAttachment;
import com.eqixiac.equinix.fabric.model.RoutingProtocolValidation;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.CloudRouterJson;
import com.eqixiac.equinix.fabric.model.json.creators.CloudRouterCreatorJson;

import java.util.List;

public interface CloudRouterClient<T> extends PageablePost<T> {

    Page<CloudRouterJson> search(FilterPropertyList filter, SortPropertyList sort);

    CloudRouterJson getByUuid(String uuid);

    CloudRouterJson create(CloudRouterCreatorJson cloudRouterCreatorJson);

    /**
     * Dry-run variant of {@link #create(CloudRouterCreatorJson)}: POSTs the same body to
     * {@code /fabric/v4/routers} with {@code dryRun=true} — per the Fabric v4 spec, an
     * "option to verify that API calls will succeed". Nothing is provisioned; the API responds
     * {@code 200} with the validated request echoed back (no {@code uuid}/{@code href}/{@code state}).
     */
    CloudRouterJson dryRunCreate(CloudRouterCreatorJson cloudRouterCreatorJson);

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
