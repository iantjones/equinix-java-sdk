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
import api.equinix.javasdk.fabric.client.internal.CloudRouterActionClient;
import api.equinix.javasdk.fabric.model.CloudRouterAction;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.CloudRouterActionJson;

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
        Page<CloudRouterAction, CloudRouterActionJson> page = Utils.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public CloudRouterActionJson getByUuid(String routerId, String uuid) {
        return getOne("GetCloudRouterAction", Map.of("routerId", routerId, "uuid", uuid));
    }

    public Page<CloudRouterAction, CloudRouterActionJson> search(String routerId, FilterPropertyList filter, SortPropertyList sort) {
        EquinixRequest<CloudRouterAction> request = buildRequestWithPathParams("SearchRouterActions", RequestType.PAGINATED_POST,
                Map.of("routerId", routerId), CloudRouterActionJson.class);
        Utils.serializeJson(request, new FilteredSortedPaginatedPost<>(filter, sort));
        return Utils.handlePaginatedListResponse(invoke(request), request);
    }
}
