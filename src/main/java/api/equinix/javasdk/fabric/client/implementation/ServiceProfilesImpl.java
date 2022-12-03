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
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.fabric.client.ServiceProfiles;
import api.equinix.javasdk.fabric.client.internal.ServiceProfileClient;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.ServiceProfileType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.Pricing;
import api.equinix.javasdk.fabric.model.ServiceProfile;
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

/**
 * <p>ServiceProfilesImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ServiceProfilesImpl implements ServiceProfiles {

    private final Fabric serviceManager;

    private final ServiceProfileClient<ServiceProfile> serviceClient;

    /**
     * <p>Constructor for ServiceProfilesImpl.</p>
     *
     * @param serviceClient a {@link ServiceProfileClient} object.
     * @param serviceManager a {@link api.equinix.javasdk.Fabric} object.
     */
    public ServiceProfilesImpl(ServiceProfileClient<ServiceProfile> serviceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<ServiceProfile> list() {
        Page<ServiceProfile, ServiceProfileJson> responsePage = this.serviceClient.list();
        PaginatedList<ServiceProfile> serviceProfileList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, ServiceProfileWrapper::new);
        return new PaginatedList<ServiceProfile>(serviceProfileList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
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
        Page<ServiceProfile, ServiceProfileJson> responsePage = serviceClient.search(filter, sort);
        PaginatedFilteredList<ServiceProfile> ServiceProfileList = Utils.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, ServiceProfileWrapper::new);
        return new PaginatedFilteredList<>(ServiceProfileList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public ServiceProfile getByUuid(String uuid) {
        ServiceProfileJson serviceProfileJson = this.serviceClient.getByUuid(uuid);
        return new ServiceProfileWrapper(serviceProfileJson, this.serviceClient);
    }

    public ServiceProfileOperator.ServiceProfileBuilder define(ServiceProfileType serviceProfileType) {
        return new ServiceProfileOperator(this.serviceClient).create(serviceProfileType);
    }
}
