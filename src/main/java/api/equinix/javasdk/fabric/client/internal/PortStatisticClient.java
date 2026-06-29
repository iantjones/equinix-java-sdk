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
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.Sortable;
import api.equinix.javasdk.fabric.client.RequestBuilder;
import api.equinix.javasdk.fabric.enums.StatisticDuration;
import api.equinix.javasdk.fabric.model.Metric;
import api.equinix.javasdk.fabric.model.PortStatistic;
import api.equinix.javasdk.fabric.model.json.PortStatisticJson;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author ianjones
 */
public interface PortStatisticClient<T> extends Pageable<T> {

    PortStatisticJson getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime);

    Page<PortStatistic, PortStatisticJson> getTopStatistics(StatisticDuration duration, Sortable sortable, RequestBuilder.TopPortStatistics requestBuilder);

    PortStatisticJson refreshStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime);

    /**
     * <p>Retrieves metrics for a single port over the supplied time range.</p>
     *
     * @param name the metric name to retrieve, or {@code null} for all metrics.
     * @return a {@link java.util.List} of {@link api.equinix.javasdk.fabric.model.Metric} objects.
     */
    List<Metric> getMetrics(String uuid, String name, LocalDateTime fromDateTime, LocalDateTime toDateTime);
}
