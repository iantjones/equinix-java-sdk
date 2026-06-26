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
import api.equinix.javasdk.fabric.client.internal.CloudRouterClient;
import api.equinix.javasdk.fabric.model.CloudRouter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.CloudRouterJson;
import api.equinix.javasdk.fabric.model.json.creators.CloudRouterCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.CloudRouterWrapper;

import java.util.List;

public class CloudRouterClientImpl extends ResourceClientBase<CloudRouter, CloudRouterJson> implements CloudRouterClient<CloudRouter> {

    public CloudRouterClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "CloudRouters", CloudRouterJson.class);
    }

    @Override
    protected CloudRouter wrap(CloudRouterJson json) {
        return new CloudRouterWrapper(json, this);
    }

    public Page<CloudRouter, CloudRouterJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage("SearchCloudRouters", new FilteredSortedPaginatedPost<>(filter, sort));
    }

    public CloudRouterJson getByUuid(String uuid) {
        return getOne("GetCloudRouter", uuid);
    }

    public CloudRouterJson create(CloudRouterCreatorJson cloudRouterCreatorJson) {
        return postOne("PostCloudRouter", cloudRouterCreatorJson);
    }

    public CloudRouterJson update(String uuid, List<PatchOperation> operations) {
        return patchOne("UpdateCloudRouter", uuid, operations);
    }

    public CloudRouterJson delete(String uuid) {
        return deleteOne("DeleteCloudRouter", uuid);
    }

    public CloudRouterJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
