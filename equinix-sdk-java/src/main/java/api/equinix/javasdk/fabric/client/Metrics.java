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
import api.equinix.javasdk.fabric.model.Metric;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;

/**
 * Client interface for the Equinix Fabric Metrics API. Provides a filtered search over
 * time-series metrics for Fabric assets (connections, ports, and more), superseding the
 * deprecated per-asset {@code /stats} statistics endpoints.
 *
 * @author ianjones
 * @version $Id: $Id
 * @see api.equinix.javasdk.fabric.model.Metric
 */
public interface Metrics {

    /**
     * Searches for metrics using an empty filter.
     *
     * @return a paginated, filtered list of matching metrics
     */
    PaginatedFilteredList<Metric> search();

    /**
     * Searches for metrics matching the specified filter criteria.
     *
     * @param filter the filter criteria to apply (for example metric {@code /name} and {@code /subject})
     * @return a paginated, filtered list of matching metrics
     */
    PaginatedFilteredList<Metric> search(FilterPropertyList filter);

    /**
     * Searches for metrics matching the specified filter and sort criteria.
     *
     * @param filter the filter criteria to apply
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching metrics
     */
    PaginatedFilteredList<Metric> search(FilterPropertyList filter, SortPropertyList sort);
}
