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

import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.client.IpBlocks;
import api.equinix.javasdk.fabric.client.internal.IpBlockClient;
import api.equinix.javasdk.fabric.model.IpBlock;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.IpBlockJson;
import api.equinix.javasdk.fabric.model.json.creators.IpBlockOperator;
import api.equinix.javasdk.fabric.model.wrappers.IpBlockWrapper;

public class IpBlocksImpl implements IpBlocks {

    private final IpBlockClient<IpBlock> serviceClient;

    public IpBlocksImpl(IpBlockClient<IpBlock> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public PaginatedFilteredList<IpBlock> search() {
        return search(Filter.filter().empty());
    }

    public PaginatedFilteredList<IpBlock> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    public PaginatedFilteredList<IpBlock> search(SortPropertyList sort) {
        return search(null, sort);
    }

    public PaginatedFilteredList<IpBlock> search(FilterPropertyList filter, SortPropertyList sort) {
        Page<IpBlock, IpBlockJson> responsePage = serviceClient.search(filter, sort);
        PaginatedFilteredList<IpBlock> ipBlockList = Utils.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, IpBlockWrapper::new);
        return new PaginatedFilteredList<>(ipBlockList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public IpBlock getByUuid(String uuid) {
        IpBlockJson ipBlockJson = this.serviceClient.getByUuid(uuid);
        return new IpBlockWrapper(ipBlockJson, this.serviceClient);
    }

    public IpBlockOperator.IpBlockBuilder define() {
        return new IpBlockOperator(this.serviceClient).create();
    }
}
