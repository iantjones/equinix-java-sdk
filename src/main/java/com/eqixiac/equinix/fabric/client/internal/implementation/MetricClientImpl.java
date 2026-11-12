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

package com.eqixiac.equinix.fabric.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.http.ParameterMapper;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.response.EquinixResponse;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.model.FilteredSortedPaginatedPost;
import com.eqixiac.equinix.fabric.client.implementation.FabricConfigImpl;
import com.eqixiac.equinix.fabric.client.internal.MetricClient;
import com.eqixiac.equinix.fabric.model.Metric;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.MetricJson;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client for the Fabric Metrics search API ({@code POST /fabric/v4/metrics/search}).
 * Standard POST-search paging comes from {@link ResourceClientBase}; since {@link MetricJson}
 * implements {@link Metric} directly, {@link #wrap(MetricJson)} is the identity.
 *
 * @author ianjones
 */
public class MetricClientImpl extends ResourceClientBase<Metric, MetricJson> implements MetricClient<Metric> {

    /**
     * The largest page the live metrics search accepts. Observed 2026-11-12: a {@code pagination.limit}
     * of 10 answers 200, while 20, 21, 25, 50 and 100 answer HTTP 400 EQ-3045002 (field: limit). The
     * spec states no maximum.
     */
    static final int MAX_PAGE_SIZE = 10;

    public MetricClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Metrics", MetricJson.class);
    }

    @Override
    protected Metric wrap(MetricJson json) {
        return json;
    }

    public Page<MetricJson> search(FilterPropertyList filter, SortPropertyList sort) {
        FilteredSortedPaginatedPost<FilterPropertyList, SortPropertyList> body =
                new FilteredSortedPaginatedPost<>(filter, sort);
        body.seekPage(0, MAX_PAGE_SIZE);
        return searchPage("SearchMetrics", body);
    }

    public List<Metric> getMetricsByName(String name, String value) {
        Map<String, List<String>> qParams = new HashMap<>();
        if (name != null) {
            ParameterMapper.addAdditionalValue(qParams, "name", name);
        }
        if (value != null) {
            ParameterMapper.addAdditionalValue(qParams, "value", value);
        }

        EquinixRequest<Metric> request = buildRequestWithQueryParams("GetMetricByName", RequestType.PAGINATED,
                qParams, MetricJson.class);
        return toMetricList(request);
    }

    private List<Metric> toMetricList(EquinixRequest<Metric> request) {
        EquinixResponse<Metric> response = invoke(request);
        Page<MetricJson> page = ResponseHandler.handlePaginatedListResponse(response, request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }
}
