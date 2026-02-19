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

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.Sortable;
import api.equinix.javasdk.fabric.enums.StatisticDuration;
import api.equinix.javasdk.fabric.model.Port;
import api.equinix.javasdk.fabric.model.PortStatistic;

import java.time.LocalDateTime;

/**
 * Client interface for managing Equinix Fabric ports. Provides operations for listing,
 * retrieving, and monitoring port bandwidth statistics.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface Ports {

    /**
     * Lists all ports accessible to the current account.
     *
     * @return a paginated list of ports
     */
    PaginatedList<Port> list();

    /**
     * Retrieves a single port by its unique identifier.
     *
     * @param uuid the unique identifier of the port
     * @return the port matching the given UUID
     */
    Port getByUuid(String uuid);

    /**
     * Retrieves bandwidth statistics for a port over the specified time range.
     *
     * @param uuid the unique identifier of the port
     * @param startDateTime the start of the statistics time range
     * @param endDateTime the end of the statistics time range
     * @return the port statistics for the specified time range
     */
    PortStatistic getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime);

    /**
     * Retrieves top port statistics ranked by bandwidth usage for the specified duration.
     *
     * @param duration the time duration to aggregate statistics over
     * @param sortable the sort configuration for ranking results
     * @return a paginated list of top port statistics
     */
    PaginatedList<PortStatistic> getTopStatistics(StatisticDuration duration, Sortable sortable);

    /**
     * Retrieves top port statistics ranked by bandwidth usage with additional request options.
     *
     * @param duration the time duration to aggregate statistics over
     * @param sortable the sort configuration for ranking results
     * @param requestBuilder additional request parameters for filtering top statistics
     * @return a paginated list of top port statistics
     */
    PaginatedList<PortStatistic> getTopStatistics(StatisticDuration duration, Sortable sortable, RequestBuilder.TopPortStatistics requestBuilder);
}
