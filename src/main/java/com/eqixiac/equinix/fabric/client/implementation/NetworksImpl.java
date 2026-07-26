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

package com.eqixiac.equinix.fabric.client.implementation;

import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.client.Networks;
import com.eqixiac.equinix.fabric.client.internal.ConnectionClient;
import com.eqixiac.equinix.fabric.client.internal.NetworkClient;
import com.eqixiac.equinix.fabric.enums.NetworkType;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.Network;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.ConnectionJson;
import com.eqixiac.equinix.fabric.model.json.NetworkJson;
import com.eqixiac.equinix.fabric.model.json.creators.NetworkOperator;
import com.eqixiac.equinix.fabric.model.wrappers.ConnectionWrapper;
import com.eqixiac.equinix.fabric.model.wrappers.NetworkWrapper;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NetworksImpl implements Networks {

    private final NetworkClient<Network> serviceClient;

    private final ConnectionClient<Connection> connectionClient;

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
        Page<NetworkJson> responsePage = serviceClient.search(filter, sort);
        PaginatedFilteredList<Network> networkList = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, NetworkWrapper::new);
        return new PaginatedFilteredList<>(networkList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public Network getByUuid(String uuid) {
        NetworkJson networkJson = this.serviceClient.getByUuid(uuid);
        return new NetworkWrapper(networkJson, this.serviceClient);
    }

    public NetworkOperator.NetworkBuilder define(NetworkType networkType) {
        return new NetworkOperator(this.serviceClient).create(networkType);
    }

    public PaginatedList<Connection> getConnections(String networkId) {
        Page<ConnectionJson> responsePage = this.serviceClient.getConnections(networkId);
        PaginatedList<Connection> connectionList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.connectionClient, ConnectionWrapper::new);
        return new PaginatedList<>(connectionList, this.connectionClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public List<Change> getChanges(String uuid) {
        return this.serviceClient.getChanges(uuid);
    }

    public Change getChange(String uuid, String changeId) {
        return this.serviceClient.getChange(uuid, changeId);
    }
}
