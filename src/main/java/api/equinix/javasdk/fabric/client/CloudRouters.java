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

package api.equinix.javasdk.fabric.client;

import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.enums.CloudRouterPackageCode;
import api.equinix.javasdk.fabric.model.CloudRouter;
import api.equinix.javasdk.fabric.model.CloudRouterPackage;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.creators.CloudRouterOperator;

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
}
