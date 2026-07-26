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
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.model.FilteredSortedPaginatedPost;
import com.eqixiac.equinix.fabric.client.implementation.FabricConfigImpl;
import com.eqixiac.equinix.fabric.client.internal.NetworkClient;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.Network;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.ConnectionJson;
import com.eqixiac.equinix.fabric.model.json.NetworkJson;
import com.eqixiac.equinix.fabric.model.json.creators.NetworkCreatorJson;
import com.eqixiac.equinix.fabric.model.wrappers.NetworkWrapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NetworkClientImpl extends ResourceClientBase<Network, NetworkJson> implements NetworkClient<Network> {

    public NetworkClientImpl(FabricConfigImpl configClient) {
        // "Network" drives the derived endpoint names (SearchNetworks, GetNetwork, DeleteNetwork).
        super(configClient, "Fabric", "Networks", NetworkJson.class, "Network");
    }

    @Override
    protected Network wrap(NetworkJson json) {
        return new NetworkWrapper(json, this);
    }

    public Page<NetworkJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage(new FilteredSortedPaginatedPost<>(filter, sort));
    }

    public Page<ConnectionJson> getConnections(String networkId) {
        EquinixRequest<Connection> request = buildRequestWithPathParams("GetNetworkConnections", RequestType.PAGINATED,
                Map.of("networkId", networkId), ConnectionJson.class);
        return ResponseHandler.handlePaginatedListResponse(invoke(request), request);
    }

    public List<Change> getChanges(String uuid) {
        EquinixRequest<Change> request = buildRequestWithPathParams("GetNetworkChanges", RequestType.PAGINATED,
                Map.of("uuid", uuid), Change.class);
        Page<Change> page = ResponseHandler.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public Change getChange(String uuid, String changeId) {
        return getAs("GetNetworkChange", Map.of("uuid", uuid, "changeId", changeId), null, Change.class);
    }

    public NetworkJson getByUuid(String uuid) {
        return getOneByUuid(uuid);
    }

    public NetworkJson create(NetworkCreatorJson networkCreatorJson) {
        // apiParams names this operation PostNetwork (not CreateNetwork) — explicit.
        return postOne("PostNetwork", networkCreatorJson);
    }

    /**
     * Dry-run variant of {@link #create(NetworkCreatorJson)}: POSTs the same body to
     * {@code /fabric/v4/networks} with the spec's {@code dryRun=true} query parameter
     * ("option to verify that API calls will succeed"). Nothing is provisioned; the spec's
     * dry-run example ({@code CreateNetworkDryRunResponse}) is the validated request echoed
     * back with no {@code uuid}/{@code href}/{@code state}.
     *
     * @param networkCreatorJson the create request body to validate
     * @return the validated request echoed back by the API
     */
    public NetworkJson dryRunCreate(NetworkCreatorJson networkCreatorJson) {
        return dryRunCreate("PostNetwork", networkCreatorJson);
    }

    public NetworkJson update(String uuid, List<PatchOperation> operations) {
        return patchOne("UpdateNetwork", uuid, operations);
    }

    public NetworkJson delete(String uuid) {
        return deleteOneByUuid(uuid);
    }

    public NetworkJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
