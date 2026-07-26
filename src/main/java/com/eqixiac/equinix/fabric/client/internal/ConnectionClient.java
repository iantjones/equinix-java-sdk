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

package com.eqixiac.equinix.fabric.client.internal;

import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.fabric.enums.Direction;
import com.eqixiac.equinix.fabric.enums.Side;
import com.eqixiac.equinix.core.http.response.PageablePost;
import com.eqixiac.equinix.fabric.client.RequestBuilder;
import com.eqixiac.equinix.fabric.enums.ConnectionOperationType;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.Metric;
import com.eqixiac.equinix.fabric.model.RouteAggregationAttachment;
import com.eqixiac.equinix.fabric.model.RouteFilterAttachment;
import com.eqixiac.equinix.fabric.model.ValidateConnectionResult;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.ConnectionActionJson;
import com.eqixiac.equinix.fabric.model.json.ConnectionJson;
import com.eqixiac.equinix.fabric.model.json.PricingJson;
import com.eqixiac.equinix.fabric.model.json.ConnectionStatisticJson;
import com.eqixiac.equinix.fabric.model.json.creators.ConnectionCreatorJson;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author ianjones
 */
public interface ConnectionClient<T> extends PageablePost<T> {

    Page<ConnectionJson> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * <p>Validates connections against the supplied filter (auth key or VLAN availability).</p>
     *
     * @return a {@link java.util.List} of {@link com.eqixiac.equinix.fabric.model.ValidateConnectionResult} objects.
     */
    List<ValidateConnectionResult> validate(FilterPropertyList filter);

    ConnectionJson getByUuid(String uuid);

    ConnectionJson create(ConnectionCreatorJson connectionCreatorJson);

    ConnectionJson dryRunCreate(ConnectionCreatorJson connectionCreatorJson);

    ConnectionActionJson performOperation(String uuid, ConnectionOperationType connectionOperationType, String description, Object bodyObject);

    ConnectionJson update(String uuid, List<PatchOperation> operations);

    /**
     * Dry-run variant of {@link #update(String, List)}: sends the same JSON Patch to
     * {@code PATCH /fabric/v4/connections/{uuid}} with {@code dryRun=true} — per the Fabric v4
     * spec, an "option to verify that API calls will succeed". Nothing is persisted; the API
     * responds {@code 200} (the real update responds {@code 202}) with a simulation of the
     * post-update connection.
     */
    ConnectionJson dryRunUpdate(String uuid, List<PatchOperation> operations);

    ConnectionStatisticJson getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime, Side viewPoint);

    /**
     * <p>Retrieves metrics for a single connection over the supplied time range.</p>
     *
     * @param name the metric name to retrieve, or {@code null} for all metrics.
     * @return a {@link java.util.List} of {@link com.eqixiac.equinix.fabric.model.Metric} objects.
     */
    List<Metric> getMetrics(String uuid, String name, LocalDateTime fromDateTime, LocalDateTime toDateTime);

    ConnectionStatisticJson refreshStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime, Side viewPoint);

    List<RouteAggregationAttachment> getRouteAggregations(String connectionId);

    RouteAggregationAttachment getRouteAggregation(String connectionId, String routeAggregationId);

    RouteAggregationAttachment attachRouteAggregation(String connectionId, String routeAggregationId);

    Boolean detachRouteAggregation(String connectionId, String routeAggregationId);

    List<RouteFilterAttachment> getRouteFilters(String connectionId);

    RouteFilterAttachment getRouteFilter(String connectionId, String routeFilterId);

    RouteFilterAttachment attachRouteFilter(String connectionId, String routeFilterId, Direction direction);

    Boolean detachRouteFilter(String connectionId, String routeFilterId);
}
