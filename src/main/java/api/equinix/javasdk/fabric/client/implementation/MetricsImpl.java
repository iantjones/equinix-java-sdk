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

package api.equinix.javasdk.fabric.client.implementation;

import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.client.Metrics;
import api.equinix.javasdk.fabric.client.internal.MetricClient;
import api.equinix.javasdk.fabric.model.Metric;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.MetricJson;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author ianjones
 */
public class MetricsImpl implements Metrics {

    private final MetricClient<Metric> serviceClient;

    public MetricsImpl(MetricClient<Metric> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public PaginatedFilteredList<Metric> search() {
        return search(Filter.filter().empty());
    }

    public PaginatedFilteredList<Metric> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    public PaginatedFilteredList<Metric> search(FilterPropertyList filter, SortPropertyList sort) {
        Page<MetricJson> responsePage = serviceClient.search(filter, sort);
        return ResponseHandler.toPaginatedFilteredList(responsePage, this.serviceClient, (json, client) -> json);
    }

    public List<Metric> getMetricsByName(String name, String value) {
        return serviceClient.getMetricsByName(name, value);
    }

    public List<Metric> getMetricsByAssetId(String asset, String assetId, String name, LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        return serviceClient.getMetricsByAssetId(asset, assetId, name, fromDateTime, toDateTime);
    }
}
