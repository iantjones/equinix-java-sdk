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
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.model.Metric;
import com.eqixiac.equinix.fabric.model.Port;
import com.eqixiac.equinix.fabric.model.PortStatistic;
import com.eqixiac.equinix.fabric.model.PortVlan;
import com.eqixiac.equinix.fabric.model.implementation.PhysicalPort;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.PhysicalPortsResponseJson;
import com.eqixiac.equinix.fabric.model.json.creators.PortOperator;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Client interface for managing Equinix Fabric ports. Provides operations for listing,
 * retrieving, and monitoring port bandwidth statistics.
 *
 * @author ianjones
 */
public interface Ports {

    /**
     * Lists all ports accessible to the current account.
     *
     * @return a paginated list of ports
     */
    PaginatedList<Port> list();

    /**
     * Searches for ports using default filter and sort criteria.
     *
     * @return a paginated, filtered list of matching ports
     */
    PaginatedFilteredList<Port> search();

    /**
     * Searches for ports matching the specified filter criteria.
     *
     * @param filter the filter criteria to apply
     * @return a paginated, filtered list of matching ports
     */
    PaginatedFilteredList<Port> search(FilterPropertyList filter);

    /**
     * Searches for ports with the specified sort order.
     *
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching ports
     */
    PaginatedFilteredList<Port> search(SortPropertyList sort);

    /**
     * Searches for ports matching the specified filter and sort criteria.
     *
     * @param filter the filter criteria to apply
     * @param sort the sort criteria to apply
     * @return a paginated, filtered list of matching ports
     */
    PaginatedFilteredList<Port> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Retrieves a single port by its unique identifier.
     *
     * @param uuid the unique identifier of the port
     * @return the port matching the given UUID
     */
    Port getByUuid(String uuid);

    /**
     * Lists the VLANs (link protocols) configured on a port.
     *
     * @param portUuid the unique identifier of the port
     * @return the list of VLANs configured on the port
     */
    List<PortVlan> getVlans(String portUuid);

    /**
     * Retrieves bandwidth statistics for a port over the specified time range.
     *
     * <p>UTC contract: {@code LocalDateTime} inputs are UTC wall clock (matching every timestamp
     * the SDK returns); use {@code LocalDateTime.now(ZoneOffset.UTC)} for the current time.</p>
     *
     * @param uuid the unique identifier of the port
     * @param startDateTime the start of the statistics time range, as UTC wall clock
     * @param endDateTime the end of the statistics time range, as UTC wall clock
     * @return the port statistics for the specified time range
     * @deprecated the {@code /stats} endpoint is deprecated by Equinix; use
     *             {@link #getMetrics(String, String, LocalDateTime, LocalDateTime)} or
     *             {@link Metrics#search(FilterPropertyList)} instead.
     */
    @Deprecated
    PortStatistic getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime);

    /**
     * Retrieves metrics for a single port over the specified time range. This is the
     * non-deprecated replacement for {@link #getStatistics(String, LocalDateTime, LocalDateTime)}.
     *
     * <p>UTC contract: {@code LocalDateTime} inputs are UTC wall clock (matching every timestamp
     * the SDK returns); use {@code LocalDateTime.now(ZoneOffset.UTC)} for the current time.</p>
     *
     * @param uuid the unique identifier of the port
     * @param name the metric name to retrieve (for example {@code equinix.fabric.port.bandwidth_rx.usage}), or {@code null} for all metrics
     * @param fromDateTime the start of the metrics time range, as UTC wall clock
     * @param toDateTime the end of the metrics time range, as UTC wall clock
     * @return the list of metrics for the port over the specified time range
     */
    List<Metric> getMetrics(String uuid, String name, LocalDateTime fromDateTime, LocalDateTime toDateTime);

    /**
     * Begins the fluent builder for creating a new port ({@code POST /fabric/v4/ports}).
     * Configure the returned builder, then call {@code create()}.
     *
     * @return a builder for configuring the new port
     */
    PortOperator.PortBuilder define();

    /**
     * Deletes a port by its unique identifier ({@code DELETE /fabric/v4/ports/{uuid}}).
     *
     * @param uuid the unique identifier of the port to delete
     * @return the deleted port as returned by the server
     */
    Port delete(String uuid);

    /**
     * Deletes a port by its unique identifier, optionally as a validate-only dry run
     * ({@code DELETE /fabric/v4/ports/{uuid}?dryRun=true}; spec: "option to verify that API calls
     * will succeed"; boolean, default {@code false}).
     *
     * <p>With {@code dryRun = true} nothing is deleted: the API responds {@code 200} (rather than
     * the real delete's {@code 202 Accepted}) with the existing port entity — uuid, name and all —
     * that WOULD be deleted. With {@code dryRun = false} this behaves exactly like
     * {@link #delete(String)}.</p>
     *
     * @param uuid the unique identifier of the port to delete
     * @param dryRun {@code true} to only verify that the delete would succeed, without deleting
     * @return the deleted port — or, on a dry run, the port that would be deleted
     */
    Port delete(String uuid, boolean dryRun);

    /**
     * Begins a fluent update of an existing port ({@code PATCH /fabric/v4/ports/{uuid}}).
     * Configure the returned updater, then call {@code save()}.
     *
     * @param uuid the unique identifier of the port to update
     * @return an updater for applying changes to the port
     */
    PortOperator.PortUpdater update(String uuid);

    /**
     * Adds physical ports to a virtual port's Link Aggregation Group
     * ({@code POST /fabric/v4/ports/{portId}/physicalPorts/bulk}).
     *
     * @param portId the virtual port uuid
     * @param physicalPorts the physical ports to add to the LAG
     * @return the full set of physical ports backing the virtual port after the addition
     */
    PhysicalPortsResponseJson addToLag(String portId, List<PhysicalPort> physicalPorts);
}
