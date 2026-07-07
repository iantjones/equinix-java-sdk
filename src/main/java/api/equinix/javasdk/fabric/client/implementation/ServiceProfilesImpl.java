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
import api.equinix.javasdk.fabric.client.ServiceProfiles;
import api.equinix.javasdk.fabric.client.internal.ServiceProfileClient;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.ServiceProfileType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.Pricing;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.ServiceProfileAction;
import api.equinix.javasdk.fabric.model.implementation.ServiceMetro;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.ConnectionJson;
import api.equinix.javasdk.fabric.model.json.PricingJson;
import api.equinix.javasdk.fabric.model.json.ServiceProfileJson;
import api.equinix.javasdk.fabric.model.json.creators.ConnectionOperator;
import api.equinix.javasdk.fabric.model.json.creators.ServiceProfileOperator;
import api.equinix.javasdk.fabric.model.wrappers.ConnectionWrapper;
import api.equinix.javasdk.fabric.model.wrappers.PricingWrapper;
import api.equinix.javasdk.fabric.model.wrappers.ServiceProfileWrapper;

import java.util.List;

/**
 *
 * @author ianjones
 */
public class ServiceProfilesImpl implements ServiceProfiles {

    private final ServiceProfileClient<ServiceProfile> serviceClient;

    public ServiceProfilesImpl(ServiceProfileClient<ServiceProfile> serviceClient) {
        this.serviceClient = serviceClient;
    }

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
