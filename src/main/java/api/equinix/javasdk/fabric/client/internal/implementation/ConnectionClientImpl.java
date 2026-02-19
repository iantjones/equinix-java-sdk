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
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.enums.Side;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedPostRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.FilteredSortedPaginatedPost;
import api.equinix.javasdk.core.util.StringUtils;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.ConnectionClient;
import api.equinix.javasdk.fabric.enums.ConnectionOperationType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.ConnectionStatistic;
import api.equinix.javasdk.fabric.model.ServiceToken;
import api.equinix.javasdk.fabric.model.implementation.ManageConnection;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.ConnectionJson;
import api.equinix.javasdk.fabric.model.json.ConnectionStatisticJson;
import api.equinix.javasdk.fabric.model.json.creators.ConnectionCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.ConnectionWrapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ConnectionClientImpl extends PageableBase implements ConnectionClient<Connection> {

    public ConnectionClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Connections");
    }

    public Page<Connection, ConnectionJson> search(FilterPropertyList filter, SortPropertyList sort) {
        EquinixRequest<Connection> equinixRequest = this.buildRequest("SearchConnections", RequestType.PAGINATED_POST, ConnectionJson.getPagedTypeRef());
        Utils.serializeJson(equinixRequest, new FilteredSortedPaginatedPost(filter, sort));
        EquinixResponse<Connection> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public ConnectionJson getByUuid(String uuid) {
        EquinixRequest<Connection> equinixRequest = this.buildRequestWithPathParams("GetConnection", RequestType.SINGLE, Map.of("uuid", uuid), ConnectionJson.getSingleTypeRef());
        EquinixResponse<Connection> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public ConnectionJson create(ConnectionCreatorJson connectionCreatorJson) {
        EquinixRequest<Connection> equinixRequest = this.buildRequest("PostConnection", RequestType.SINGLE, ConnectionJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, connectionCreatorJson);
        EquinixResponse<Connection> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public ConnectionJson dryRunCreate(ConnectionCreatorJson connectionCreatorJson) {
        EquinixRequest<Connection> equinixRequest = this.buildRequest("PostConnection", RequestType.SINGLE, ConnectionJson.getSingleTypeRef());
        equinixRequest.addSingleQueryParameter("dryRun", "true");
        Utils.serializeJson(equinixRequest, connectionCreatorJson);
        EquinixResponse<Connection> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public ConnectionJson performOperation(String uuid, ConnectionOperationType connectionOperationType, String description, Object bodyObject) {
        ManageConnection connOp = new ManageConnection(connectionOperationType, description, bodyObject);
        EquinixRequest<Connection> equinixRequest = this.buildRequestWithPathParams("ManageConnection", RequestType.SINGLE, Map.of("uuid", uuid), ConnectionJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, connOp);
        EquinixResponse<Connection> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public List<Connection> batch(List<ConnectionCreatorJson> connectionCreatorJsonList) {
        EquinixRequest<Connection> equinixRequest = this.buildRequest("PostBulkConnections", RequestType.SINGLE, ConnectionJson.getListTypeRef());
        Utils.serializeJson(equinixRequest, connectionCreatorJsonList);
        EquinixResponse<Connection> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public ConnectionStatisticJson getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime, Side viewPoint) {
        Map<String, List<String>> qParams = Map.of(
                "startDateTime", Utils.singleParamList(Utils.dateTimeForQuery(startDateTime)),
                "endDateTime", Utils.singleParamList(Utils.dateTimeForQuery(endDateTime)),
                "viewPoint", Utils.singleParamList(StringUtils.sideToViewPoint(viewPoint))
        );

        EquinixRequest<ConnectionStatistic> equinixRequest = this.buildRequest("GetStatistics", RequestType.SINGLE, Map.of("uuid", uuid), qParams, ConnectionStatisticJson.getSingleTypeRef());
        EquinixResponse<ConnectionStatistic> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public ConnectionStatisticJson refreshStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime, Side viewPoint) {
        return this.getStatistics(uuid, startDateTime, endDateTime, viewPoint);
    }

    public ConnectionJson delete(String uuid) {
        EquinixRequest<ServiceToken> equinixRequest = this.buildRequestWithPathParams("DeleteConnection", RequestType.SINGLE, Map.of("uuid", uuid), ConnectionJson.getSingleTypeRef());
        EquinixResponse<ServiceToken> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public ConnectionJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    @Override
    public PaginatedList<Connection> nextPage(PaginatedRequest<Connection> equinixRequest) {
        EquinixResponse<Connection> equinixResponse = this.invoke(equinixRequest);
        Page<Connection, ConnectionJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Connection> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, ConnectionWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }

    public PaginatedFilteredList<Connection> nextPage(PaginatedPostRequest<Connection> equinixRequest) {
        Utils.serializeJson(equinixRequest, equinixRequest.getObjectToSerialize());
        EquinixResponse<Connection> equinixResponse = this.invoke(equinixRequest);
        Page<Connection, ConnectionJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedFilteredList<Connection> newPaginatedFilteredList = Utils.mapPaginatedFilteredList(nextPage.getItems(), this, ConnectionWrapper::new);
        return new PaginatedFilteredList<>(newPaginatedFilteredList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
