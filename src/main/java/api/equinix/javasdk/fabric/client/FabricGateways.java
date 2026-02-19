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

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.enums.GatewayPackageCode;
import api.equinix.javasdk.fabric.model.FabricGateway;
import api.equinix.javasdk.fabric.model.GatewayPackage;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;

/**
 * Client interface for managing Equinix Fabric Gateways. Gateways provide virtual routing
 * and network functions for connecting to cloud providers and other networks.
 */
public interface FabricGateways {

    /**
     * Searches for fabric gateways using default filter and sort criteria.
     *
     * @return a paginated, filtered list of matching gateways
     */
    PaginatedFilteredList<FabricGateway> search();

    /**
     * Searches for fabric gateways matching the specified filter criteria.
     *
     * @param filter the filter criteria to apply
     * @return a paginated, filtered list of matching gateways
     */
    PaginatedFilteredList<FabricGateway> search(FilterPropertyList filter);

    /**
     * Searches for fabric gateways with the specified sort order.
     *
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching gateways
     */
    PaginatedFilteredList<FabricGateway> search(SortPropertyList sort);

    /**
     * Searches for fabric gateways matching the specified filter and sort criteria.
     *
     * @param filter the filter criteria to apply
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching gateways
     */
    PaginatedFilteredList<FabricGateway> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Retrieves a single fabric gateway by its unique identifier.
     *
     * @param uuid the unique identifier of the gateway
     * @return the fabric gateway matching the given UUID
     */
    FabricGateway getByUuid(String uuid);

    /**
     * Lists all available gateway packages.
     *
     * @return a paginated list of gateway packages
     */
    PaginatedList<GatewayPackage> gatewayPackages();

    /**
     * Retrieves a specific gateway package by its package code.
     *
     * @param gatewayPackageCode the code identifying the gateway package
     * @return the gateway package matching the given code
     */
    GatewayPackage gatewayPackageByCode(GatewayPackageCode gatewayPackageCode);

//    ServiceTokenOperator.ServiceTokenBuilder define(Side issuerSide);
}
