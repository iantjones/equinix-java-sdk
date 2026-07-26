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
import com.eqixiac.equinix.fabric.client.ServiceProfiles;
import com.eqixiac.equinix.fabric.client.internal.ServiceProfileClient;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.enums.ServiceProfileType;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.Pricing;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.ServiceProfileAction;
import com.eqixiac.equinix.fabric.model.implementation.ServiceMetro;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.ConnectionJson;
import com.eqixiac.equinix.fabric.model.json.PricingJson;
import com.eqixiac.equinix.fabric.model.json.ServiceProfileJson;
import com.eqixiac.equinix.fabric.model.json.creators.ConnectionOperator;
import com.eqixiac.equinix.fabric.model.json.creators.ServiceProfileOperator;
import com.eqixiac.equinix.fabric.model.wrappers.ConnectionWrapper;
import com.eqixiac.equinix.fabric.model.wrappers.PricingWrapper;
import com.eqixiac.equinix.fabric.model.wrappers.ServiceProfileWrapper;

import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author ianjones
 */
@RequiredArgsConstructor
public class ServiceProfilesImpl implements ServiceProfiles {

    private final ServiceProfileClient<ServiceProfile> serviceClient;

    public PaginatedList<ServiceProfile> list() {
        return ResponseHandler.toPaginatedList(this.serviceClient.list(), this.serviceClient, ServiceProfileWrapper::new);
    }

    public PaginatedFilteredList<ServiceProfile> search() {
        return search(Filter.filter().empty());
    }

    public PaginatedFilteredList<ServiceProfile> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    public PaginatedFilteredList<ServiceProfile> search(SortPropertyList sort) {
        return search(null, sort);
    }

    public PaginatedFilteredList<ServiceProfile> search(FilterPropertyList filter, SortPropertyList sort) {
        return ResponseHandler.toPaginatedFilteredList(serviceClient.search(filter, sort), this.serviceClient, ServiceProfileWrapper::new);
    }

    public ServiceProfile getByUuid(String uuid) {
        ServiceProfileJson serviceProfileJson = this.serviceClient.getByUuid(uuid);
        return new ServiceProfileWrapper(serviceProfileJson, this.serviceClient);
    }

    public ServiceProfileOperator.ServiceProfileBuilder define(ServiceProfileType serviceProfileType) {
        return new ServiceProfileOperator(this.serviceClient).create(serviceProfileType);
    }

    public ServiceProfileOperator.ServiceProfileUpdater update(String uuid) {
        return new ServiceProfileOperator(this.serviceClient).update(uuid);
    }

    public ServiceProfileAction createAction(String uuid, String type, String description) {
        return this.serviceClient.createAction(uuid, type, description);
    }

    public List<ServiceMetro> getMetros(String uuid) {
        return this.serviceClient.getMetros(uuid);
    }
}
