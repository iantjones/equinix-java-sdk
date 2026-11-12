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
import com.eqixiac.equinix.fabric.model.CloudEvent;
import com.eqixiac.equinix.fabric.model.implementation.filter.CloudEventQuery;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;

/**
 * Client interface for Equinix Fabric cloud events ({@code POST /fabric/v4/cloudevents/search},
 * {@code GET /fabric/v4/cloudevents/{id}}) and live events ({@code POST /fabric/v4/liveEvents/search},
 * the last operational event per subject, used for project-level active alerts).
 *
 * <p>The per-asset {@code GET /{asset}/{id}/cloudevents} endpoint is deprecated in spec 4.34.0 and
 * is no longer called; use {@link CloudEventQuery#forAsset(String, String)}.</p>
 */
public interface CloudEvents {

    /**
     * Searches cloud events with a filter built by a {@link CloudEventQuery}.
     *
     * @param query the query (scoped by subject, project or organization)
     * @return a paginated, filtered list of matching cloud events
     */
    PaginatedFilteredList<CloudEvent> search(CloudEventQuery query);

    /**
     * Searches cloud events with a {@link CloudEventQuery} and a sort order.
     *
     * @param query the query
     * @param sort the sort criteria, or {@code null}
     * @return a paginated, filtered list of matching cloud events
     */
    PaginatedFilteredList<CloudEvent> search(CloudEventQuery query, SortPropertyList sort);

    /**
     * Searches cloud events with a hand-built filter.
     *
     * @param filter the filter; it must contain {@code /subject}, {@code /equinixproject} or
     *        {@code /equinixorganization}
     * @param sort the sort criteria, or {@code null}
     * @return a paginated, filtered list of matching cloud events
     * @throws IllegalArgumentException when the filter has no scope expression
     */
    PaginatedFilteredList<CloudEvent> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Retrieves a single cloud event by its unique identifier.
     *
     * @param uuid the unique identifier of the cloud event
     * @return the cloud event matching the given UUID
     */
    CloudEvent getByUuid(String uuid);

    /**
     * Returns the last operational event of each given subject
     * ({@code POST /fabric/v4/liveEvents/search}), for example the current port-status alert.
     *
     * <p>Live behaviour (observed 2026-11-11): exact subjects such as
     * {@code /fabric/v4/ports/{uuid}} work with {@code IN}; an empty filter answers HTTP 500 and a
     * wildcard subject is rejected, so at least one exact subject is required.</p>
     *
     * @param subjects the exact subject paths (at least one)
     * @return a paginated, filtered list of the live events
     */
    PaginatedFilteredList<CloudEvent> searchLiveEvents(String... subjects);
}
