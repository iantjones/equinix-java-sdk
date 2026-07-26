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
import com.eqixiac.equinix.fabric.model.EiaService;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.creators.EiaServiceOperator;

/**
 * Client interface for managing Equinix Internet Access (EIA) services. EIA services provide
 * dedicated internet connectivity (single or dual) over Equinix Fabric.
 */
public interface EiaServices {

    /**
     * Searches for EIA services using default filter and sort criteria.
     *
     * @return a paginated, filtered list of matching EIA services
     */
    PaginatedFilteredList<EiaService> search();

    /**
     * Searches for EIA services matching the specified filter criteria.
     *
     * @param filter the filter criteria to apply
     * @return a paginated, filtered list of matching EIA services
     */
    PaginatedFilteredList<EiaService> search(FilterPropertyList filter);

    /**
     * Searches for EIA services with the specified sort order.
     *
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching EIA services
     */
    PaginatedFilteredList<EiaService> search(SortPropertyList sort);

    /**
     * Searches for EIA services matching the specified filter and sort criteria.
     *
     * @param filter the filter criteria to apply
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching EIA services
     */
    PaginatedFilteredList<EiaService> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Retrieves a single EIA service by its unique identifier.
     *
     * @param uuid the unique identifier of the EIA service
     * @return the EIA service matching the given UUID
     */
    EiaService getByUuid(String uuid);

    /**
     * Begins the fluent builder for creating a new EIA service.
     * Call methods on the returned builder to configure the service, then call {@code create()}.
     *
     * @return a builder for configuring the new EIA service
     */
    EiaServiceOperator.EiaServiceBuilder define();
}
