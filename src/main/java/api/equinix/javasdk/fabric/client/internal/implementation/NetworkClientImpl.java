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
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.FilteredSortedPaginatedPost;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.NetworkClient;
import api.equinix.javasdk.fabric.model.Network;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.NetworkJson;
import api.equinix.javasdk.fabric.model.json.creators.NetworkCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.NetworkWrapper;

public class NetworkClientImpl extends ResourceClientBase<Network, NetworkJson> implements NetworkClient<Network> {

    public NetworkClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Networks", NetworkJson.class);
    }

    @Override
    protected Network wrap(NetworkJson json) {
        return new NetworkWrapper(json, this);
    }

    public Page<Network, NetworkJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage("SearchNetworks", new FilteredSortedPaginatedPost<>(filter, sort));
    }

    public NetworkJson getByUuid(String uuid) {
        return getOne("GetNetwork", uuid);
    }

    public NetworkJson create(NetworkCreatorJson networkCreatorJson) {
        return postOne("PostNetwork", networkCreatorJson);
    }

    public NetworkJson update(String uuid, NetworkCreatorJson networkCreatorJson) {
        return updateOne("UpdateNetwork", uuid, networkCreatorJson);
    }

    public NetworkJson delete(String uuid) {
        return deleteOne("DeleteNetwork", uuid);
    }

    public NetworkJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
