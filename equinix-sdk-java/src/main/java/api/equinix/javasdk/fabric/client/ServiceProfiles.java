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
import api.equinix.javasdk.fabric.enums.ServiceProfileType;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.creators.ServiceProfileOperator;

/**
 * Client interface for managing Equinix Fabric service profiles. Service profiles define the
 * attributes and access configurations that providers publish for customers to connect to.
 */
public interface ServiceProfiles {

    /**
     * Lists all service profiles owned by the current account.
     *
     * @return a paginated list of service profiles
     */
    PaginatedList<ServiceProfile> list();

    /**
     * Searches for service profiles using default filter and sort criteria.
     *
     * @return a paginated, filtered list of matching service profiles
     */
    PaginatedFilteredList<ServiceProfile> search();

    /**
     * Searches for service profiles matching the specified filter criteria.
     *
     * @param filter the filter criteria to apply
     * @return a paginated, filtered list of matching service profiles
     */
    PaginatedFilteredList<ServiceProfile> search(FilterPropertyList filter);

    /**
     * Searches for service profiles with the specified sort order.
     *
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching service profiles
     */
    PaginatedFilteredList<ServiceProfile> search(SortPropertyList sort);

    /**
     * Searches for service profiles matching the specified filter and sort criteria.
     *
     * @param filter the filter criteria to apply
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching service profiles
     */
    PaginatedFilteredList<ServiceProfile> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Retrieves a single service profile by its unique identifier.
     *
     * @param uuid the unique identifier of the service profile
     * @return the service profile matching the given UUID
     */
    ServiceProfile getByUuid(String uuid);

    /**
     * Begins the fluent builder for creating a new service profile.
     * Call methods on the returned builder to configure the profile, then call {@code create()}.
     *
     * @param serviceProfileType the type of service profile to create
     * @return a builder for configuring the new service profile
     */
    ServiceProfileOperator.ServiceProfileBuilder define(ServiceProfileType serviceProfileType);
}
