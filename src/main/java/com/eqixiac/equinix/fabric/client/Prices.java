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
import com.eqixiac.equinix.fabric.model.Pricing;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;

/**
 * Client interface for querying Equinix Fabric pricing information. Provides access to
 * pricing details for connections, ports, and other Fabric resources.
 */
public interface Prices {

    /**
     * Lists pricing information matching the specified filter criteria.
     *
     * @param filter the filter criteria to narrow pricing results (e.g., by connection type or metro)
     * @return a paginated, filtered list of pricing entries
     */
    PaginatedFilteredList<Pricing> list(FilterPropertyList filter);
}
