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
import com.eqixiac.equinix.fabric.client.CloudRouters;
import com.eqixiac.equinix.fabric.client.internal.CloudRouterClient;
import com.eqixiac.equinix.fabric.client.internal.CloudRouterCommandClient;
import com.eqixiac.equinix.fabric.client.internal.CloudRouterPackageClient;
import com.eqixiac.equinix.fabric.client.internal.RouteTableEntryClient;
import com.eqixiac.equinix.fabric.enums.CloudRouterPackageCode;
import com.eqixiac.equinix.fabric.model.CloudRouter;
import com.eqixiac.equinix.fabric.model.CloudRouterAction;
import com.eqixiac.equinix.fabric.model.CloudRouterCommand;
import com.eqixiac.equinix.fabric.model.CloudRouterPackage;
import com.eqixiac.equinix.fabric.model.RouteAggregationAttachment;
import com.eqixiac.equinix.fabric.model.RouteFilterAttachment;
import com.eqixiac.equinix.fabric.model.RouteTableEntry;
import com.eqixiac.equinix.fabric.model.RoutingProtocolValidation;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.CloudRouterCommandJson;
import com.eqixiac.equinix.fabric.model.json.CloudRouterJson;
import com.eqixiac.equinix.fabric.model.json.CloudRouterPackageJson;
import com.eqixiac.equinix.fabric.model.json.RouteTableEntryJson;
import com.eqixiac.equinix.fabric.model.json.creators.CloudRouterCommandOperator;
import com.eqixiac.equinix.fabric.model.json.creators.CloudRouterOperator;
import com.eqixiac.equinix.fabric.model.wrappers.CloudRouterWrapper;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CloudRoutersImpl implements CloudRouters {

    private final CloudRouterClient<CloudRouter> serviceClient;

    private final CloudRouterPackageClient<CloudRouterPackage> cloudRouterPackageServiceClient;

    private final RouteTableEntryClient<RouteTableEntry> routesClient;

    private final CloudRouterCommandClient<CloudRouterCommand> commandsClient;

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
        Page<CloudRouterJson> responsePage = serviceClient.search(filter, sort);
        PaginatedFilteredList<CloudRouter> cloudRouterList = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, CloudRouterWrapper::new);
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
        Page<CloudRouterPackageJson> responsePage = this.cloudRouterPackageServiceClient.list();
        PaginatedList<CloudRouterPackage> cloudRouterPackageList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.cloudRouterPackageServiceClient, (json, client) -> json);
        return new PaginatedList<>(cloudRouterPackageList, this.cloudRouterPackageServiceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public CloudRouterPackage routerPackageByCode(CloudRouterPackageCode packageCode) {
        return this.cloudRouterPackageServiceClient.getByPackageCode(packageCode);
    }

    public PaginatedFilteredList<RouteTableEntry> searchRoutes(String routerId) {
        return searchRoutes(routerId, null, null);
    }

    public PaginatedFilteredList<RouteTableEntry> searchRoutes(String routerId, FilterPropertyList filter, SortPropertyList sort) {
        Page<RouteTableEntryJson> responsePage = this.routesClient.searchCloudRouterRoutes(routerId, filter, sort);
        PaginatedFilteredList<RouteTableEntry> routes = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.routesClient, (json, client) -> json);
        return new PaginatedFilteredList<>(routes, this.routesClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public RoutingProtocolValidation validateRoutingProtocol(String routerId, FilterPropertyList filter) {
        return this.serviceClient.validateRoutingProtocol(routerId, filter);
    }

    public PaginatedList<CloudRouterCommand> commands(String routerId) {
        Page<CloudRouterCommandJson> responsePage = this.commandsClient.list(routerId);
        PaginatedList<CloudRouterCommand> commandList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.commandsClient, (json, client) -> json);
        return new PaginatedList<>(commandList, this.commandsClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public CloudRouterCommand getCommand(String routerId, String commandId) {
        return this.commandsClient.getByUuid(routerId, commandId);
    }

    public CloudRouterCommandOperator.CloudRouterCommandBuilder defineCommand(String routerId) {
        return new CloudRouterCommandOperator(this.commandsClient, routerId).create();
    }

    public Boolean deleteCommand(String routerId, String commandId) {
        this.commandsClient.delete(routerId, commandId);
        return true;
    }

    public List<CloudRouterAction> getActions(String routerId) {
        return this.serviceClient.getActions(routerId);
    }

    public CloudRouterAction getAction(String routerId, String uuid) {
        return this.serviceClient.getAction(routerId, uuid);
    }

    public PaginatedFilteredList<CloudRouterAction> searchActions(String routerId, FilterPropertyList filter, SortPropertyList sort) {
        return this.serviceClient.searchActions(routerId, filter, sort);
    }

    public PaginatedFilteredList<CloudRouterCommand> searchCommands(String routerId, FilterPropertyList filter, SortPropertyList sort) {
        Page<CloudRouterCommandJson> responsePage = this.commandsClient.search(routerId, filter, sort);
        PaginatedFilteredList<CloudRouterCommand> commands = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.commandsClient, (json, client) -> json);
        return new PaginatedFilteredList<>(commands, this.commandsClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PaginatedFilteredList<RouteFilterAttachment> searchRouteFilterAttachments(String routerId, FilterPropertyList filter, SortPropertyList sort) {
        return this.serviceClient.searchRouteFilterAttachments(routerId, filter, sort);
    }

    public PaginatedFilteredList<RouteAggregationAttachment> searchRouteAggregationAttachments(String routerId, FilterPropertyList filter, SortPropertyList sort) {
        return this.serviceClient.searchRouteAggregationAttachments(routerId, filter, sort);
    }
}
