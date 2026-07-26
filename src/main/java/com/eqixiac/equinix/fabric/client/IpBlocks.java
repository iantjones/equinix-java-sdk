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
import com.eqixiac.equinix.fabric.model.IpBlock;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.creators.IpBlockOperator;

/**
 * Client interface for managing Equinix Fabric IP blocks (BYOIP / Equinix-owned IPv4 and IPv6
 * prefixes).
 */
public interface IpBlocks {

    /**
     * Searches for IP blocks using default filter and sort criteria.
     *
     * @return a paginated, filtered list of matching IP blocks
     */
    PaginatedFilteredList<IpBlock> search();

    /**
     * Searches for IP blocks matching the specified filter criteria.
     *
     * @param filter the filter criteria to apply
     * @return a paginated, filtered list of matching IP blocks
     */
    PaginatedFilteredList<IpBlock> search(FilterPropertyList filter);

    /**
     * Searches for IP blocks with the specified sort order.
     *
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching IP blocks
     */
    PaginatedFilteredList<IpBlock> search(SortPropertyList sort);

    /**
     * Searches for IP blocks matching the specified filter and sort criteria.
     *
     * @param filter the filter criteria to apply
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching IP blocks
     */
    PaginatedFilteredList<IpBlock> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Retrieves a single IP block by its unique identifier.
     *
     * @param uuid the unique identifier of the IP block
     * @return the IP block matching the given UUID
     */
    IpBlock getByUuid(String uuid);

    /**
     * Begins the fluent builder for submitting a new IP block.
     *
     * @return a builder for configuring the new IP block
     */
    IpBlockOperator.IpBlockBuilder define();
}
