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
import api.equinix.javasdk.core.http.ParameterMapper;
import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.PortStatisticClient;
import api.equinix.javasdk.fabric.model.Metric;
import api.equinix.javasdk.fabric.model.PortStatistic;
import api.equinix.javasdk.fabric.model.json.MetricJson;
import api.equinix.javasdk.fabric.model.json.PortStatisticJson;
import api.equinix.javasdk.fabric.model.wrappers.PortStatisticWrapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client for Fabric Port statistics. The statistics endpoints reuse the generic
 * single/paginated helpers from {@link ResourceClientBase}/{@code ClientBase}.
 *
 * @author ianjones
 */
public class PortStatisticClientImpl extends ResourceClientBase<PortStatistic, PortStatisticJson> implements PortStatisticClient<PortStatistic> {

    public PortStatisticClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Ports", PortStatisticJson.class);
    }

    @Override
    protected PortStatistic wrap(PortStatisticJson json) {
        return new PortStatisticWrapper(json, this);
    }

    public PortStatisticJson getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Map<String, List<String>> qParams = Map.of(
                "startDateTime", ParameterMapper.singleParamList(ParameterMapper.dateTimeForQuery(startDateTime)),
                "endDateTime", ParameterMapper.singleParamList(ParameterMapper.dateTimeForQuery(endDateTime))
        );
        return getAs("GetStatistics", Map.of("uuid", uuid), qParams, PortStatisticJson.class);
    }

    public PortStatisticJson refreshStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return getStatistics(uuid, startDateTime, endDateTime);
    }

    public List<Metric> getMetrics(String uuid, String name, LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        Map<String, List<String>> qParams = new HashMap<>();
        if (name != null) {
            ParameterMapper.addAdditionalValue(qParams, "name", name);
        }
        if (fromDateTime != null) {
            ParameterMapper.addAdditionalValue(qParams, "fromDateTime", ParameterMapper.dateTimeForQuery(fromDateTime));
        }
        if (toDateTime != null) {
            ParameterMapper.addAdditionalValue(qParams, "toDateTime", ParameterMapper.dateTimeForQuery(toDateTime));
        }

        EquinixRequest<Metric> equinixRequest = buildRequest("GetMetrics", RequestType.PAGINATED,
                Map.of("uuid", uuid), qParams, MetricJson.class);
        EquinixResponse<Metric> equinixResponse = invoke(equinixRequest);
        Page<MetricJson> page = ResponseHandler.handlePaginatedListResponse(equinixResponse, equinixRequest);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }
}
