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

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.Sortable;
import api.equinix.javasdk.core.enums.RequestType;
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
 * <p>PortStatisticsClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class PortStatisticClientImpl extends PageableBase implements PortStatisticClient<PortStatistic> {

    /**
     * <p>Constructor for PortStatisticsClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl} object.
     */
    public PortStatisticClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Ports");
    }

    /** {@inheritDoc} */
    public PortStatisticJson getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Map<String, List<String>> qParams = Map.of(
                "startDateTime", Utils.singleParamList(Utils.dateTimeForQuery(startDateTime)),
                "endDateTime", Utils.singleParamList(Utils.dateTimeForQuery(endDateTime))
        );
        Map<String, String> pParams = Map.of("uuid", uuid);

        EquinixRequest<PortStatistic> equinixRequest = this.buildRequest("GetStatistics", RequestType.SINGLE, pParams, qParams, PortStatisticJson.singleTypeRef());
        EquinixResponse<PortStatistic> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /**
     * {@inheritDoc}
     *
     * <p>getTopStatistics.</p>
     */
    public Page<PortStatistic, PortStatisticJson> getTopStatistics(StatisticDuration duration, Sortable sortable, RequestBuilder.TopPortStatistics requestBuilder) {
        Map<String, List<String>> qParams = Utils.newMap(requestBuilder);
        Utils.addAdditionalValue(qParams, "sort", sortable);
        Utils.addAdditionalValue(qParams, "duration", duration);

        EquinixRequest<PortStatistic> equinixRequest = this.buildRequest("GetStatistics", RequestType.PAGINATED, null, qParams, PortStatisticJson.pagedTypeRef());
        EquinixResponse<PortStatistic> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public PortStatisticJson refreshStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return this.getStatistics(uuid, startDateTime, endDateTime);
    }

    /** {@inheritDoc} */
    public PaginatedList<PortStatistic> nextPage(PaginatedRequest<PortStatistic> equinixRequest) {
        EquinixResponse<PortStatistic> equinixResponse = this.invoke(equinixRequest);
        Page<PortStatistic, PortStatisticJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<PortStatistic> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, PortStatisticWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
