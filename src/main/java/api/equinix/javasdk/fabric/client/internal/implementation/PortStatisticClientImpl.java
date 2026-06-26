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
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.Sortable;
import api.equinix.javasdk.fabric.client.RequestBuilder;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.PortStatisticClient;
import api.equinix.javasdk.fabric.enums.StatisticDuration;
import api.equinix.javasdk.fabric.model.PortStatistic;
import api.equinix.javasdk.fabric.model.json.PortStatisticJson;
import api.equinix.javasdk.fabric.model.wrappers.PortStatisticWrapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Internal client for Fabric Port statistics. The statistics endpoints reuse the generic
 * single/paginated helpers from {@link ResourceClientBase}/{@code ClientBase}.
 *
 * @author ianjones
 * @version $Id: $Id
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
                "startDateTime", Utils.singleParamList(Utils.dateTimeForQuery(startDateTime)),
                "endDateTime", Utils.singleParamList(Utils.dateTimeForQuery(endDateTime))
        );
        return getAs("GetStatistics", Map.of("uuid", uuid), qParams, PortStatisticJson.class);
    }

    public Page<PortStatistic, PortStatisticJson> getTopStatistics(StatisticDuration duration, Sortable sortable, RequestBuilder.TopPortStatistics requestBuilder) {
        Map<String, List<String>> qParams = Utils.newMap(requestBuilder);
        Utils.addAdditionalValue(qParams, "sort", sortable);
        Utils.addAdditionalValue(qParams, "duration", duration);
        return listPage("GetStatistics", qParams);
    }

    public PortStatisticJson refreshStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return getStatistics(uuid, startDateTime, endDateTime);
    }
}
