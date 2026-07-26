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

package com.eqixiac.equinix.fabric.client;

import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.enums.CloudRouterPackageCode;
import com.eqixiac.equinix.fabric.model.CloudRouter;
import com.eqixiac.equinix.fabric.model.CloudRouterAction;
import com.eqixiac.equinix.fabric.model.CloudRouterCommand;
import com.eqixiac.equinix.fabric.model.CloudRouterPackage;
import com.eqixiac.equinix.fabric.model.RouteAggregationAttachment;
import com.eqixiac.equinix.fabric.model.RouteFilterAttachment;
import com.eqixiac.equinix.fabric.model.RouteTableEntry;
import com.eqixiac.equinix.fabric.model.RoutingProtocolValidation;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.creators.CloudRouterCommandOperator;
import com.eqixiac.equinix.fabric.model.json.creators.CloudRouterOperator;

import java.util.List;

/**
 * Client interface for managing Equinix Fabric Cloud Routers. Provides operations for searching,
 * retrieving, creating cloud routers and querying available router packages.
 */
public interface CloudRouters {

    /**
     * Searches for cloud routers using default filter and sort criteria.
     *
     * @return a paginated, filtered list of matching cloud routers
     */
    PaginatedFilteredList<CloudRouter> search();

    /**
     * Searches for cloud routers matching the specified filter criteria.
     *
     * @param filter the filter criteria to apply
     * @return a paginated, filtered list of matching cloud routers
     */
    PaginatedFilteredList<CloudRouter> search(FilterPropertyList filter);

    /**
     * Searches for cloud routers with the specified sort order.
     *
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching cloud routers
     */
    PaginatedFilteredList<CloudRouter> search(SortPropertyList sort);

    /**
     * Searches for cloud routers matching the specified filter and sort criteria.
     *
     * @param filter the filter criteria to apply
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching cloud routers
     */
    PaginatedFilteredList<CloudRouter> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Retrieves a single cloud router by its unique identifier.
     *
     * @param uuid the unique identifier of the cloud router
     * @return the cloud router matching the given UUID
     */
    CloudRouter getByUuid(String uuid);

    /**
     * Begins the fluent builder for creating a new cloud router.
     * Call methods on the returned builder to configure the router, then call {@code create()}.
     *
     * @return a builder for configuring the new cloud router
     */
    CloudRouterOperator.CloudRouterBuilder define();

    /**
     * Lists all available cloud router packages.
     *
     * @return a paginated list of cloud router packages
     */
    PaginatedList<CloudRouterPackage> routerPackages();

    /**
     * Retrieves a specific cloud router package by its package code.
     *
     * @param packageCode the code identifying the router package
     * @return the cloud router package matching the given code
     */
    CloudRouterPackage routerPackageByCode(CloudRouterPackageCode packageCode);

    /**
     * Searches the route table of a Fabric Cloud Router using default filter and sort criteria.
     *
     * @param routerId the unique identifier of the cloud router
     * @return a paginated, filtered list of route table entries
     */
    PaginatedFilteredList<RouteTableEntry> searchRoutes(String routerId);

    /**
     * Searches the route table of a Fabric Cloud Router.
     *
     * @param routerId the unique identifier of the cloud router
     * @param filter the filter criteria to apply (may be {@code null})
     * @param sort the sort criteria to apply (may be {@code null})
     * @return a paginated, filtered list of route table entries
     */
    PaginatedFilteredList<RouteTableEntry> searchRoutes(String routerId, FilterPropertyList filter, SortPropertyList sort);

    /**
     * Validates all subnets associated with the connections on a Fabric Cloud Router.
     *
     * @param routerId the unique identifier of the cloud router
     * @param filter the validation filter (for example a direct interface IP and connection uuid)
     * @return the validation result
     */
    RoutingProtocolValidation validateRoutingProtocol(String routerId, FilterPropertyList filter);

    /**
     * Lists all diagnostic commands (ping / traceroute) issued against a Fabric Cloud Router.
     *
     * @param routerId the unique identifier of the cloud router
     * @return a paginated list of cloud router commands
     */
    PaginatedList<CloudRouterCommand> commands(String routerId);

    /**
     * Retrieves a single Fabric Cloud Router diagnostic command by its unique identifier.
     *
     * @param routerId the unique identifier of the cloud router
     * @param commandId the unique identifier of the command
     * @return the cloud router command matching the given UUID
     */
    CloudRouterCommand getCommand(String routerId, String commandId);

    /**
     * Begins the fluent builder for issuing a new Fabric Cloud Router diagnostic command.
     *
     * @param routerId the unique identifier of the cloud router
     * @return a builder for configuring the new command
     */
    CloudRouterCommandOperator.CloudRouterCommandBuilder defineCommand(String routerId);

    /**
     * Deletes a Fabric Cloud Router diagnostic command by its unique identifier.
     *
     * @param routerId the unique identifier of the cloud router
     * @param commandId the unique identifier of the command
     * @return {@code true} if the command was deleted
     */
    Boolean deleteCommand(String routerId, String commandId);

    /**
     * Lists all route-table / BGP-session actions issued against a Fabric Cloud Router.
     *
     * @param routerId the unique identifier of the cloud router
     * @return the list of cloud router actions
     */
    List<CloudRouterAction> getActions(String routerId);

    /**
     * Retrieves a single Fabric Cloud Router action by its unique identifier.
     *
     * @param routerId the unique identifier of the cloud router
     * @param uuid the unique identifier of the action
     * @return the cloud router action matching the given UUID
     */
    CloudRouterAction getAction(String routerId, String uuid);

    /**
     * Searches the route-table / BGP-session actions issued against a Fabric Cloud Router.
     *
     * @param routerId the unique identifier of the cloud router
     * @param filter the filter criteria to apply (may be {@code null})
     * @param sort the sort criteria to apply (may be {@code null})
     * @return a paginated, filtered list of cloud router actions
     */
    PaginatedFilteredList<CloudRouterAction> searchActions(String routerId, FilterPropertyList filter, SortPropertyList sort);

    /**
     * Searches the diagnostic commands (ping / traceroute) issued against a Fabric Cloud Router.
     *
     * @param routerId the unique identifier of the cloud router
     * @param filter the filter criteria to apply (may be {@code null})
     * @param sort the sort criteria to apply (may be {@code null})
     * @return a paginated, filtered list of cloud router commands
     */
    PaginatedFilteredList<CloudRouterCommand> searchCommands(String routerId, FilterPropertyList filter, SortPropertyList sort);

    /**
     * Searches the Route Filter attachments for a Fabric Cloud Router.
     *
     * @param routerId the unique identifier of the cloud router
     * @param filter the filter criteria to apply (may be {@code null})
     * @param sort the sort criteria to apply (may be {@code null})
     * @return a paginated, filtered list of route filter attachments
     */
    PaginatedFilteredList<RouteFilterAttachment> searchRouteFilterAttachments(String routerId, FilterPropertyList filter, SortPropertyList sort);

    /**
     * Searches the Route Aggregation attachments for a Fabric Cloud Router.
     *
     * @param routerId the unique identifier of the cloud router
     * @param filter the filter criteria to apply (may be {@code null})
     * @param sort the sort criteria to apply (may be {@code null})
     * @return a paginated, filtered list of route aggregation attachments
     */
    PaginatedFilteredList<RouteAggregationAttachment> searchRouteAggregationAttachments(String routerId, FilterPropertyList filter, SortPropertyList sort);
}
