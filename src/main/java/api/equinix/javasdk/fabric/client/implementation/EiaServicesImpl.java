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

import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.client.EiaServices;
import api.equinix.javasdk.fabric.client.internal.EiaServiceClient;
import api.equinix.javasdk.fabric.model.EiaService;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.EiaServiceJson;
import api.equinix.javasdk.fabric.model.json.creators.EiaServiceOperator;
import api.equinix.javasdk.fabric.model.wrappers.EiaServiceWrapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EiaServicesImpl implements EiaServices {

    private final EiaServiceClient<EiaService> serviceClient;

    public PaginatedFilteredList<EiaService> search() {
        return search(Filter.filter().empty());
    }

    public PaginatedFilteredList<EiaService> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    public PaginatedFilteredList<EiaService> search(SortPropertyList sort) {
        return search(null, sort);
    }

    public PaginatedFilteredList<EiaService> search(FilterPropertyList filter, SortPropertyList sort) {
        Page<EiaServiceJson> responsePage = serviceClient.search(filter, sort);
        PaginatedFilteredList<EiaService> eiaServiceList = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, EiaServiceWrapper::new);
        return new PaginatedFilteredList<>(eiaServiceList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public EiaService getByUuid(String uuid) {
        EiaServiceJson eiaServiceJson = this.serviceClient.getByUuid(uuid);
        return new EiaServiceWrapper(eiaServiceJson, this.serviceClient);
    }

    public EiaServiceOperator.EiaServiceBuilder define() {
        return new EiaServiceOperator(this.serviceClient).create();
    }
}
