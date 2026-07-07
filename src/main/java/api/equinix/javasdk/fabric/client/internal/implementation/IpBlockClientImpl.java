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
import api.equinix.javasdk.fabric.client.internal.IpBlockClient;
import api.equinix.javasdk.fabric.model.IpBlock;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.IpBlockJson;
import api.equinix.javasdk.fabric.model.json.creators.IpBlockCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.IpBlockWrapper;

import java.util.List;

public class IpBlockClientImpl extends ResourceClientBase<IpBlock, IpBlockJson> implements IpBlockClient<IpBlock> {

    public IpBlockClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "IpBlocks", IpBlockJson.class);
    }

    @Override
    protected IpBlock wrap(IpBlockJson json) {
        return new IpBlockWrapper(json, this);
    }

    public Page<IpBlockJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage("SearchIpBlocks", new FilteredSortedPaginatedPost<>(filter, sort));
    }

    public IpBlockJson getByUuid(String uuid) {
        return getOne("GetIpBlock", uuid);
    }

    public IpBlockJson create(IpBlockCreatorJson ipBlockCreatorJson) {
        return postOne("SubmitIpBlock", ipBlockCreatorJson);
    }

    public IpBlockJson update(String uuid, List<PatchOperation> operations) {
        return patchOne("PatchIpBlock", uuid, operations);
    }

    public IpBlockJson delete(String uuid) {
        return deleteOne("DeleteIpBlock", uuid);
    }

    public IpBlockJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
