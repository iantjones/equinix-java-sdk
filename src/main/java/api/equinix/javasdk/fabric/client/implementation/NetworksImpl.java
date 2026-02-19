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

package api.equinix.javasdk.fabric.client.implementation;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.client.Networks;
import api.equinix.javasdk.fabric.client.internal.NetworkClient;
import api.equinix.javasdk.fabric.enums.NetworkType;
import api.equinix.javasdk.fabric.model.Network;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.NetworkJson;
import api.equinix.javasdk.fabric.model.json.creators.NetworkOperator;
import api.equinix.javasdk.fabric.model.wrappers.NetworkWrapper;

public class NetworksImpl implements Networks {

    private final Fabric serviceManager;

    private final NetworkClient<Network> serviceClient;

    public NetworksImpl(NetworkClient<Network> serviceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedFilteredList<Network> search() {
        return search(Filter.filter().empty());
    }

    public PaginatedFilteredList<Network> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    public PaginatedFilteredList<Network> search(SortPropertyList sort) {
        return search(null, sort);
    }

    public PaginatedFilteredList<Network> search(FilterPropertyList filter, SortPropertyList sort) {
        Page<Network, NetworkJson> responsePage = serviceClient.search(filter, sort);
        PaginatedFilteredList<Network> networkList = Utils.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, NetworkWrapper::new);
        return new PaginatedFilteredList<>(networkList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public Network getByUuid(String uuid) {
        NetworkJson networkJson = this.serviceClient.getByUuid(uuid);
        return new NetworkWrapper(networkJson, this.serviceClient);
    }

    public NetworkOperator.NetworkBuilder define(NetworkType networkType) {
        return new NetworkOperator(this.serviceClient).create(networkType);
    }
}
