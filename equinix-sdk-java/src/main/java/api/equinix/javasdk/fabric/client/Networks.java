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
import api.equinix.javasdk.fabric.enums.NetworkType;
import api.equinix.javasdk.fabric.model.Network;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.creators.NetworkOperator;

/**
 * Client interface for managing Equinix Fabric networks. Networks provide logical groupings
 * of connections that share routing and policy configurations.
 */
public interface Networks {

    /**
     * Searches for networks using default filter and sort criteria.
     *
     * @return a paginated, filtered list of matching networks
     */
    PaginatedFilteredList<Network> search();

    /**
     * Searches for networks matching the specified filter criteria.
     *
     * @param filter the filter criteria to apply
     * @return a paginated, filtered list of matching networks
     */
    PaginatedFilteredList<Network> search(FilterPropertyList filter);

    /**
     * Searches for networks with the specified sort order.
     *
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching networks
     */
    PaginatedFilteredList<Network> search(SortPropertyList sort);

    /**
     * Searches for networks matching the specified filter and sort criteria.
     *
     * @param filter the filter criteria to apply
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching networks
     */
    PaginatedFilteredList<Network> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Retrieves a single network by its unique identifier.
     *
     * @param uuid the unique identifier of the network
     * @return the network matching the given UUID
     */
    Network getByUuid(String uuid);

    /**
     * Begins the fluent builder for creating a new network.
     * Call methods on the returned builder to configure the network, then call {@code create()}.
     *
     * @param networkType the type of network to create
     * @return a builder for configuring the new network
     */
    NetworkOperator.NetworkBuilder define(NetworkType networkType);
}
