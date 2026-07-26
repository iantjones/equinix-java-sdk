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
import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.model.FilteredSortedPaginatedPost;
import com.eqixiac.equinix.fabric.client.implementation.FabricConfigImpl;
import com.eqixiac.equinix.fabric.client.internal.IpBlockClient;
import com.eqixiac.equinix.fabric.model.IpBlock;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.IpBlockJson;
import com.eqixiac.equinix.fabric.model.json.creators.IpBlockCreatorJson;
import com.eqixiac.equinix.fabric.model.wrappers.IpBlockWrapper;

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
