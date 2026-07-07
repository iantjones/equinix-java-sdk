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

package api.equinix.javasdk.fabric.client.internal;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.fabric.model.Metric;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.MetricJson;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>MetricClient interface. Internal client for the Fabric Metrics search API
 * ({@code POST /fabric/v4/metrics/search}).</p>
 *
 * @author ianjones
 */
public interface MetricClient<T> extends PageablePost<T> {

    /**
     * <p>Searches metrics matching the supplied filter and sort criteria.</p>
     *
     */
    Page<MetricJson> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Retrieves metrics by wildcard metric name ({@code GET /fabric/v4/metrics}). Only the
     * wildcard metro metric types are supported by this endpoint (for example
     * {@code equinix.fabric.metro.*.latency}). This endpoint does not support time-range filtering.
     *
     * @param name the wildcard metric name (required)
     * @param value which value to retrieve (for example {@code last}; required)
     * @return the matching metrics
     */
    List<Metric> getMetricsByName(String name, String value);

    /**
     * Retrieves metrics for a specific asset ({@code GET /fabric/v4/{asset}/{assetId}/metrics}).
     *
     * @param asset the asset type (for example {@code ports}, {@code connections}, or {@code metros})
     * @param assetId the asset uuid
     * @param name the metric name (required)
     * @param fromDateTime optional start of the time range, or {@code null}
     * @param toDateTime optional end of the time range, or {@code null}
     * @return the matching metrics for the asset
     */
    List<Metric> getMetricsByAssetId(String asset, String assetId, String name, LocalDateTime fromDateTime, LocalDateTime toDateTime);
}
