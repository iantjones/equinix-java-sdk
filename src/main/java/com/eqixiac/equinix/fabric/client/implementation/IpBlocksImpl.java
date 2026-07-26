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
import com.eqixiac.equinix.fabric.client.IpBlocks;
import com.eqixiac.equinix.fabric.client.internal.IpBlockClient;
import com.eqixiac.equinix.fabric.model.IpBlock;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.IpBlockJson;
import com.eqixiac.equinix.fabric.model.json.creators.IpBlockOperator;
import com.eqixiac.equinix.fabric.model.wrappers.IpBlockWrapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IpBlocksImpl implements IpBlocks {

    private final IpBlockClient<IpBlock> serviceClient;

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
        Page<IpBlockJson> responsePage = serviceClient.search(filter, sort);
        PaginatedFilteredList<IpBlock> ipBlockList = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, IpBlockWrapper::new);
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
