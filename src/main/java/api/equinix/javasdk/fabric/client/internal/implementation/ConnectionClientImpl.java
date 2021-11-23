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
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.enums.Side;
import api.equinix.javasdk.core.util.StringUtils;
import api.equinix.javasdk.fabric.client.RequestBuilder;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.ConnectionClient;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.ConnectionStatistic;
import api.equinix.javasdk.fabric.model.json.ConnectionJson;
import api.equinix.javasdk.fabric.model.json.ConnectionPricingJson;
import api.equinix.javasdk.fabric.model.json.ConnectionStatisticJson;
import api.equinix.javasdk.fabric.model.wrappers.ConnectionWrapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>ConnectionsClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ConnectionClientImpl extends PageableBase implements ConnectionClient<Connection> {

    /**
     * <p>Constructor for ConnectionsClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl} object.
     */
    public ConnectionClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Connections");
    }

    /** {@inheritDoc} */
    public ConnectionStatisticJson getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime, Side viewPoint) {
        Map<String, List<String>> qParams = Map.of(
                "startDateTime", Utils.singleParamList(Utils.dateTimeForQuery(startDateTime)),
                "endDateTime", Utils.singleParamList(Utils.dateTimeForQuery(endDateTime)),
                "viewPoint", Utils.singleParamList(StringUtils.sideToViewPoint(viewPoint))
        );
        Map<String, String> pParams = Map.of("uuid", uuid);

        EquinixRequest<ConnectionStatistic> equinixRequest = this.buildRequest("GetStatistics", RequestType.SINGLE, pParams, qParams, ConnectionStatisticJson.singleTypeRef());
        EquinixResponse<ConnectionStatistic> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public ConnectionPricingJson getPricing(String portUuid, MetroCode destinationMetro, RequestBuilder.ConnectionPricing requestBuilder) {
        try {
            Map<String, List<String>> qParams = Utils.newMap(requestBuilder);
            qParams.put("destinationMetro", Utils.singleParamList(destinationMetro));
            qParams.put("portUUID", Utils.singleParamList(portUuid));

            if(requestBuilder != null) {
                qParams.putAll(Utils.processRequestBuilder(requestBuilder));
            }

            EquinixRequest<ConnectionStatistic> equinixRequest = Utils.buildRequest("ECX", "Connections",
                    "GetPricing", RequestType.SINGLE, this.getConfigClient().getEquinixClient(), null, qParams,
                    ConnectionPricingJson.singleTypeRef());
            EquinixResponse<ConnectionStatistic> equinixResponse = this.invoke(equinixRequest);
            return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /** {@inheritDoc} */
    public ConnectionStatisticJson refreshStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime, Side viewPoint) {
        return this.getStatistics(uuid, startDateTime, endDateTime, viewPoint);
    }

    /** {@inheritDoc} */
    public PaginatedList<Connection> nextPage(PaginatedRequest<Connection> equinixRequest) {
        EquinixResponse<Connection> equinixResponse = this.invoke(equinixRequest);
        Page<Connection, ConnectionJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Connection> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, ConnectionWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
