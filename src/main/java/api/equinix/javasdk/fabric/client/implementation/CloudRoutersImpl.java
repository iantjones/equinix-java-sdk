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
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.client.CloudRouters;
import api.equinix.javasdk.fabric.client.internal.CloudRouterClient;
import api.equinix.javasdk.fabric.client.internal.CloudRouterPackageClient;
import api.equinix.javasdk.fabric.enums.CloudRouterPackageCode;
import api.equinix.javasdk.fabric.model.CloudRouter;
import api.equinix.javasdk.fabric.model.CloudRouterPackage;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.CloudRouterJson;
import api.equinix.javasdk.fabric.model.json.CloudRouterPackageJson;
import api.equinix.javasdk.fabric.model.json.creators.CloudRouterOperator;
import api.equinix.javasdk.fabric.model.wrappers.CloudRouterWrapper;

public class CloudRoutersImpl implements CloudRouters {

    private final Fabric serviceManager;

    private final CloudRouterClient<CloudRouter> serviceClient;

    private final CloudRouterPackageClient<CloudRouterPackage> cloudRouterPackageServiceClient;

    public CloudRoutersImpl(CloudRouterClient<CloudRouter> serviceClient, CloudRouterPackageClient<CloudRouterPackage> cloudRouterPackageServiceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.cloudRouterPackageServiceClient = cloudRouterPackageServiceClient;
        this.serviceClient = serviceClient;
    }

    public PaginatedFilteredList<CloudRouter> search() {
        return search(Filter.filter().empty());
    }

    public PaginatedFilteredList<CloudRouter> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    public PaginatedFilteredList<CloudRouter> search(SortPropertyList sort) {
        return search(null, sort);
    }

    public PaginatedFilteredList<CloudRouter> search(FilterPropertyList filter, SortPropertyList sort) {
        Page<CloudRouter, CloudRouterJson> responsePage = serviceClient.search(filter, sort);
        PaginatedFilteredList<CloudRouter> cloudRouterList = Utils.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, CloudRouterWrapper::new);
        return new PaginatedFilteredList<>(cloudRouterList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public CloudRouter getByUuid(String uuid) {
        CloudRouterJson cloudRouterJson = this.serviceClient.getByUuid(uuid);
        return new CloudRouterWrapper(cloudRouterJson, this.serviceClient);
    }

    public CloudRouterOperator.CloudRouterBuilder define() {
        return new CloudRouterOperator(this.serviceClient).create();
    }

    public PaginatedList<CloudRouterPackage> routerPackages() {
        Page<CloudRouterPackage, CloudRouterPackageJson> responsePage = this.cloudRouterPackageServiceClient.list();
        PaginatedList<CloudRouterPackage> cloudRouterPackageList = Utils.mapPaginatedList(responsePage.getItems(), this.cloudRouterPackageServiceClient, (json, client) -> json);
        return new PaginatedList<>(cloudRouterPackageList, this.cloudRouterPackageServiceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public CloudRouterPackage routerPackageByCode(CloudRouterPackageCode packageCode) {
        return this.cloudRouterPackageServiceClient.getByPackageCode(packageCode);
    }
}
