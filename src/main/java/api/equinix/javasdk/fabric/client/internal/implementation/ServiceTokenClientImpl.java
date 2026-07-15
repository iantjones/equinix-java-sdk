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
import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.FilteredSortedPaginatedPost;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.ServiceTokenClient;
import api.equinix.javasdk.fabric.enums.ServiceTokenAction;
import api.equinix.javasdk.fabric.model.ServiceToken;
import api.equinix.javasdk.fabric.model.implementation.ServiceTokenActionRequest;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.ServiceTokenJson;
import api.equinix.javasdk.fabric.model.json.creators.ServiceTokenCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.ServiceTokenWrapper;

import java.util.List;
import java.util.Map;

/**
 * Internal client for Fabric Service Tokens. Standard plumbing/paging come from
 * {@link ResourceClientBase}.
 *
 * @author ianjones
 */
public class ServiceTokenClientImpl extends ResourceClientBase<ServiceToken, ServiceTokenJson> implements ServiceTokenClient<ServiceToken> {

    public ServiceTokenClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "ServiceTokens", ServiceTokenJson.class);
    }

    @Override
    protected ServiceToken wrap(ServiceTokenJson json) {
        return new ServiceTokenWrapper(json, this);
    }

    public Page<ServiceTokenJson> list() {
        return listPage("GetServiceTokens");
    }

    public Page<ServiceTokenJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage("SearchServiceTokens", new FilteredSortedPaginatedPost<>(filter, sort));
    }

    public ServiceTokenJson getByUuid(String uuid) {
        return getOne("GetServiceToken", uuid);
    }

    public ServiceTokenJson update(String uuid, List<PatchOperation> operations) {
        // PATCH /serviceTokens/{uuid} with an op/path/value array sent as application/json.
        return updateOne("UpdateServiceToken", uuid, operations);
    }

    public ServiceTokenJson dryRunUpdate(String uuid, List<PatchOperation> operations) {
        // Same wire shape as update() (op/path/value array as application/json) plus dryRun=true,
        // so the dry run validates exactly the request the real update would send.
        return dryRunUpdate("UpdateServiceToken", uuid, operations);
    }

    public ServiceTokenJson createAction(String uuid, ServiceTokenAction type) {
        return postOne("PostServiceTokenAction", Map.of("uuid", uuid), new ServiceTokenActionRequest(type));
    }

    public ServiceTokenJson create(ServiceTokenCreatorJson serviceTokenCreatorJson) {
        // The former createServiceTokenFilter was inert: no class carried
        // @JsonFilter("createServiceTokenFilter"), so the body always serialized unfiltered.
        // The filter machinery has been removed; behaviour on the wire is unchanged.
        return postOne("PostServiceToken", serviceTokenCreatorJson);
    }

    public ServiceTokenJson dryRunCreate(ServiceTokenCreatorJson serviceTokenCreatorJson) {
        return dryRunCreate("PostServiceToken", serviceTokenCreatorJson);
    }

    public ServiceTokenJson delete(String uuid) {
        return deleteOne("DeleteServiceToken", uuid);
    }

    public ServiceTokenJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
