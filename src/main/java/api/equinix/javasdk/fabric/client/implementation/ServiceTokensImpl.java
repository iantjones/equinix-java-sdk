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
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.client.ServiceTokens;
import api.equinix.javasdk.fabric.client.internal.ServiceTokenClient;
import api.equinix.javasdk.fabric.enums.ServiceTokenAction;
import api.equinix.javasdk.fabric.enums.Side;
import api.equinix.javasdk.fabric.model.ServiceToken;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.ServiceTokenJson;
import api.equinix.javasdk.fabric.model.json.creators.ServiceTokenOperator;
import api.equinix.javasdk.fabric.model.wrappers.ServiceTokenWrapper;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author ianjones
 */
@RequiredArgsConstructor
public class ServiceTokensImpl implements ServiceTokens {

    private final ServiceTokenClient<ServiceToken> serviceClient;

    public PaginatedList<ServiceToken> list() {
        Page<ServiceTokenJson> responsePage = this.serviceClient.list();
        PaginatedList<ServiceToken> serviceTokenList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, ServiceTokenWrapper::new);
        return new PaginatedList<>(serviceTokenList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public ServiceToken getByUuid(String uuid) {
        ServiceTokenJson serviceTokenJson = this.serviceClient.getByUuid(uuid);
        return new ServiceTokenWrapper(serviceTokenJson, this.serviceClient);
    }

    public ServiceTokenOperator.ServiceTokenBuilder define(Side issuerSide) {
        return new ServiceTokenOperator(this.serviceClient).create(issuerSide);
    }

    public PaginatedFilteredList<ServiceToken> search() {
        return search(Filter.filter().empty());
    }

    public PaginatedFilteredList<ServiceToken> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    public PaginatedFilteredList<ServiceToken> search(SortPropertyList sort) {
        return search(null, sort);
    }

    public PaginatedFilteredList<ServiceToken> search(FilterPropertyList filter, SortPropertyList sort) {
        Page<ServiceTokenJson> responsePage = this.serviceClient.search(filter, sort);
        PaginatedFilteredList<ServiceToken> serviceTokenList = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, ServiceTokenWrapper::new);
        return new PaginatedFilteredList<>(serviceTokenList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public ServiceTokenOperator.ServiceTokenUpdater update(String uuid) {
        return new ServiceTokenOperator(this.serviceClient).update(uuid);
    }

    public ServiceToken createAction(String uuid, ServiceTokenAction type) {
        ServiceTokenJson serviceTokenJson = this.serviceClient.createAction(uuid, type);
        return new ServiceTokenWrapper(serviceTokenJson, this.serviceClient);
    }
}
