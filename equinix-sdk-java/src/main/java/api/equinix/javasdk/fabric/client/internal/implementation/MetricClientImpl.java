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

package api.equinix.javasdk.fabric.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.FilteredSortedPaginatedPost;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.MetricClient;
import api.equinix.javasdk.fabric.model.Metric;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.MetricJson;

import java.time.LocalDateTime;
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
 * @version $Id: $Id
 */
public class MetricClientImpl extends ResourceClientBase<Metric, MetricJson> implements MetricClient<Metric> {

    public MetricClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Metrics", MetricJson.class);
    }

    @Override
    protected Metric wrap(MetricJson json) {
        return json;
    }

    public Page<Metric, MetricJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage("SearchMetrics", new FilteredSortedPaginatedPost<>(filter, sort));
    }

    /** {@inheritDoc} */
    public List<Metric> getMetricsByName(String name, String value) {
        Map<String, List<String>> qParams = new HashMap<>();
        if (name != null) {
            Utils.addAdditionalValue(qParams, "name", name);
        }
        if (value != null) {
            Utils.addAdditionalValue(qParams, "value", value);
        }

        EquinixRequest<Metric> request = buildRequestWithQueryParams("GetMetricByName", RequestType.PAGINATED,
                qParams, MetricJson.getPagedTypeRef());
        return toMetricList(request);
    }

    /** {@inheritDoc} */
    public List<Metric> getMetricsByAssetId(String asset, String assetId, String name, LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        Map<String, List<String>> qParams = new HashMap<>();
        if (name != null) {
            Utils.addAdditionalValue(qParams, "name", name);
        }
        addDateRange(qParams, fromDateTime, toDateTime);

        EquinixRequest<Metric> request = buildRequest("GetMetricByAssetId", RequestType.PAGINATED,
                Map.of("asset", asset, "assetId", assetId), qParams, MetricJson.getPagedTypeRef());
        return toMetricList(request);
    }

    private static void addDateRange(Map<String, List<String>> qParams, LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        if (fromDateTime != null) {
            Utils.addAdditionalValue(qParams, "fromDateTime", Utils.dateTimeForQuery(fromDateTime));
        }
        if (toDateTime != null) {
            Utils.addAdditionalValue(qParams, "toDateTime", Utils.dateTimeForQuery(toDateTime));
        }
    }

    private List<Metric> toMetricList(EquinixRequest<Metric> request) {
        EquinixResponse<Metric> response = invoke(request);
        Page<Metric, MetricJson> page = Utils.handlePaginatedListResponse(response, request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }
}
