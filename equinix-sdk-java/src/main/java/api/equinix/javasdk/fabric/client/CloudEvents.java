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
import api.equinix.javasdk.fabric.model.CloudEvent;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;

/**
 * Client interface for accessing Equinix Fabric cloud events. Cloud events provide
 * notifications about changes and activities across Fabric resources.
 *
 * <p>Cloud events are queried via a POST-based search ({@code POST /fabric/v4/cloudevents/search});
 * there is no bare collection listing in the Fabric API.</p>
 */
public interface CloudEvents {

    /**
     * Searches for cloud events using default (empty) filter criteria.
     *
     * @return a paginated, filtered list of cloud events
     */
    PaginatedFilteredList<CloudEvent> search();

    /**
     * Searches for cloud events matching the specified filter criteria.
     *
     * @param filter the filter criteria to apply
     * @return a paginated, filtered list of matching cloud events
     */
    PaginatedFilteredList<CloudEvent> search(FilterPropertyList filter);

    /**
     * Searches for cloud events with the specified sort order.
     *
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of cloud events
     */
    PaginatedFilteredList<CloudEvent> search(SortPropertyList sort);

    /**
     * Searches for cloud events matching the specified filter and sort criteria.
     *
     * @param filter the filter criteria to apply
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching cloud events
     */
    PaginatedFilteredList<CloudEvent> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Retrieves a single cloud event by its unique identifier.
     *
     * @param uuid the unique identifier of the cloud event
     * @return the cloud event matching the given UUID
     */
    CloudEvent getByUuid(String uuid);
}
