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

package com.eqixiac.equinix.fabric.client.implementation;

import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.client.Metrics;
import com.eqixiac.equinix.fabric.client.internal.MetricClient;
import com.eqixiac.equinix.fabric.model.Metric;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.filter.MetricQuery;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.MetricJson;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * Default {@link Metrics} implementation over {@code POST /fabric/v4/metrics/search}.
 *
 * @author ianjones
 */
@RequiredArgsConstructor
public class MetricsImpl implements Metrics {

    private final MetricClient<Metric> serviceClient;

    @Override
    public PaginatedFilteredList<Metric> search(MetricQuery query) {
        Objects.requireNonNull(query, "query");
        return search(query.toFilter(), null);
    }

    @Override
    public PaginatedFilteredList<Metric> search(FilterPropertyList filter, SortPropertyList sort) {
        MetricQuery.validate(filter);
        Page<MetricJson> responsePage = serviceClient.search(filter, sort);
        return ResponseHandler.toPaginatedFilteredList(responsePage, this.serviceClient, (json, client) -> json);
    }

    @Override
    public List<Metric> forConnection(String connectionUuid, LocalDateTime fromDateTime, LocalDateTime toDateTime, String... names) {
        return all(MetricQuery.forConnection(connectionUuid), fromDateTime, toDateTime, names);
    }

    @Override
    public List<Metric> forPort(String portUuid, LocalDateTime fromDateTime, LocalDateTime toDateTime, String... names) {
        return all(MetricQuery.forPort(portUuid), fromDateTime, toDateTime, names);
    }

    @Override
    public List<Metric> forMetro(String metroCode, LocalDateTime fromDateTime, LocalDateTime toDateTime, String... names) {
        return all(MetricQuery.forMetro(metroCode), fromDateTime, toDateTime, names);
    }

    @Override
    public List<Metric> getMetricsByName(String name, String value) {
        return serviceClient.getMetricsByName(name, value);
    }

    private List<Metric> all(MetricQuery query, LocalDateTime fromDateTime, LocalDateTime toDateTime, String... names) {
        query.names(names).between(fromDateTime, toDateTime);
        return search(query).loadAll().toList();
    }
}
