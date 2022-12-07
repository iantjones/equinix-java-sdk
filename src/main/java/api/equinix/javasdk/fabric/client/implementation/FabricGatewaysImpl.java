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
import api.equinix.javasdk.fabric.client.FabricGateways;
import api.equinix.javasdk.fabric.client.internal.FabricGatewayClient;
import api.equinix.javasdk.fabric.client.internal.GatewayPackageClient;
import api.equinix.javasdk.fabric.enums.GatewayPackageCode;
import api.equinix.javasdk.fabric.model.FabricGateway;
import api.equinix.javasdk.fabric.model.GatewayPackage;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.FabricGatewayJson;
import api.equinix.javasdk.fabric.model.json.GatewayPackageJson;
import api.equinix.javasdk.fabric.model.wrappers.FabricGatewayWrapper;
import api.equinix.javasdk.fabric.model.wrappers.GatewayPackageWrapper;

public class FabricGatewaysImpl implements FabricGateways {

    private final Fabric serviceManager;

    private final FabricGatewayClient<FabricGateway> serviceClient;

    private final GatewayPackageClient<GatewayPackage> gatewayPackageServiceClient;

    public FabricGatewaysImpl(FabricGatewayClient<FabricGateway> serviceClient, GatewayPackageClient<GatewayPackage> gatewayPackageServiceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.gatewayPackageServiceClient = gatewayPackageServiceClient;
        this.serviceClient = serviceClient;
    }

    public PaginatedFilteredList<FabricGateway> search() {
        return search(Filter.filter().empty());
    }

    public PaginatedFilteredList<FabricGateway> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    public PaginatedFilteredList<FabricGateway> search(SortPropertyList sort) {
        return search(null, sort);
    }

    public PaginatedFilteredList<FabricGateway> search(FilterPropertyList filter, SortPropertyList sort) {
        Page<FabricGateway, FabricGatewayJson> responsePage = serviceClient.search(filter, sort);
        PaginatedFilteredList<FabricGateway> FabricGatewayList = Utils.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, FabricGatewayWrapper::new);
        return new PaginatedFilteredList<>(FabricGatewayList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public FabricGateway getByUuid(String uuid) {
        FabricGatewayJson FabricGatewayJson = this.serviceClient.getByUuid(uuid);
        return new FabricGatewayWrapper(FabricGatewayJson, this.serviceClient);
    }
    
    public PaginatedList<GatewayPackage> gatewayPackages() {
        Page<GatewayPackage, GatewayPackageJson> responsePage = this.gatewayPackageServiceClient.list();
        PaginatedList<GatewayPackage> GatewayPackageList = Utils.mapPaginatedList(responsePage.getItems(), this.gatewayPackageServiceClient, GatewayPackageWrapper::new);
        return new PaginatedList<>(GatewayPackageList, this.gatewayPackageServiceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public GatewayPackage gatewayPackageByCode(GatewayPackageCode gatewayPackageCode) {
        GatewayPackageJson GatewayPackageJson = this.gatewayPackageServiceClient.getByPackageCode(gatewayPackageCode);
        return new GatewayPackageWrapper(GatewayPackageJson, this.gatewayPackageServiceClient);
    }

//    public FabricGatewayOperator.FabricGatewayBuilder define(Side issuerSide) {
//        return new FabricGatewayOperator(this.serviceClient).create(issuerSide);
//    }
}
