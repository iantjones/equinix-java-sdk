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

package api.equinix.javasdk.fabric.model;

import api.equinix.javasdk.fabric.model.implementation.MetricDatapoint;
import api.equinix.javasdk.fabric.model.implementation.MetricResource;

import java.util.List;

/**
 * A time-series metric returned by the Equinix Fabric Metrics API. Each metric describes a
 * named measurement (for example bandwidth usage) for a particular asset over a series of
 * time-bucketed {@link MetricDatapoint datapoints}.
 *
 * <p>Metrics supersede the deprecated per-asset {@code /stats} statistics endpoints and are
 * retrieved via {@code fabric.metrics().search(...)} or the per-asset convenience methods
 * {@code fabric.connections().getMetrics(...)} and {@code fabric.ports().getMetrics(...)}.</p>
 *
 * @author ianjones
 * @see api.equinix.javasdk.fabric.client.Metrics
 */
public interface Metric {

    /**
     * <p>The Equinix supported metric type.</p>
     *
     */
    String getType();

    /**
     * <p>The metric name (for example {@code equinix.fabric.connection.bandwidth_tx.usage}).</p>
     *
     */
    String getName();

    /**
     * <p>The unit the metric values are expressed in.</p>
     *
     */
    String getUnit();

    /**
     * <p>The metric interval, set automatically based on the search range.</p>
     *
     */
    String getInterval();

    /**
     * <p>The asset (resource) the metric was collected for.</p>
     *
     */
    MetricResource getResource();

    /**
     * <p>A human-readable summary of the metric.</p>
     *
     */
    String getSummary();

    /**
     * <p>The time-bucketed datapoints making up this metric.</p>
     *
     * @return a {@link java.util.List} of {@link api.equinix.javasdk.fabric.model.implementation.MetricDatapoint} objects.
     */
    List<MetricDatapoint> getDatapoints();
}
