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
import com.eqixiac.equinix.fabric.client.internal.CloudRouterActionClient;
import com.eqixiac.equinix.fabric.model.CloudRouterAction;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.CloudRouterActionJson;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Internal client for Fabric Cloud Router actions (route-table / BGP-session updates). The JSON
 * model implements the public interface directly, so {@link #wrap(CloudRouterActionJson)} is the
 * identity.
 *
 * @author ianjones
 */
public class CloudRouterActionClientImpl extends ResourceClientBase<CloudRouterAction, CloudRouterActionJson> implements CloudRouterActionClient<CloudRouterAction> {

    public CloudRouterActionClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "CloudRouters", CloudRouterActionJson.class);
    }

    @Override
    protected CloudRouterAction wrap(CloudRouterActionJson json) {
        return json;
    }

    public List<CloudRouterAction> list(String routerId) {
        EquinixRequest<CloudRouterAction> request = buildRequestWithPathParams("GetCloudRouterActions", RequestType.PAGINATED,
                Map.of("routerId", routerId), CloudRouterActionJson.class);
        Page<CloudRouterActionJson> page = ResponseHandler.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public CloudRouterActionJson getByUuid(String routerId, String uuid) {
        return getOne("GetCloudRouterAction", Map.of("routerId", routerId, "uuid", uuid));
    }

    public Page<CloudRouterActionJson> search(String routerId, FilterPropertyList filter, SortPropertyList sort) {
        EquinixRequest<CloudRouterAction> request = buildRequestWithPathParams("SearchRouterActions", RequestType.PAGINATED_POST,
                Map.of("routerId", routerId), CloudRouterActionJson.class);
        SerializationHelper.serializeJson(request, new FilteredSortedPaginatedPost<>(filter, sort));
        return ResponseHandler.handlePaginatedListResponse(invoke(request), request);
    }
}
