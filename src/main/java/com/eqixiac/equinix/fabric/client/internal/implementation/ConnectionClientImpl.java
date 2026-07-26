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
import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.EquinixResponse;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.model.FilteredSortedPaginatedPost;
import com.eqixiac.equinix.fabric.enums.Direction;
import com.eqixiac.equinix.fabric.enums.Side;
import com.eqixiac.equinix.fabric.client.implementation.FabricConfigImpl;
import com.eqixiac.equinix.fabric.client.internal.ConnectionClient;
import com.eqixiac.equinix.fabric.client.internal.RouteAggregationAttachmentClient;
import com.eqixiac.equinix.fabric.client.internal.RouteFilterAttachmentClient;
import com.eqixiac.equinix.fabric.enums.ConnectionOperationType;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.Metric;
import com.eqixiac.equinix.fabric.model.RouteAggregationAttachment;
import com.eqixiac.equinix.fabric.model.RouteFilterAttachment;
import com.eqixiac.equinix.fabric.model.ValidateConnectionResult;
import com.eqixiac.equinix.fabric.model.implementation.ConnectionValidationRequest;
import com.eqixiac.equinix.fabric.model.implementation.ManageConnection;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.ConnectionActionJson;
import com.eqixiac.equinix.fabric.model.json.ConnectionJson;
import com.eqixiac.equinix.fabric.model.json.ConnectionStatisticJson;
import com.eqixiac.equinix.fabric.model.json.ConnectionValidationResponseJson;
import com.eqixiac.equinix.fabric.model.json.MetricJson;
import com.eqixiac.equinix.fabric.model.json.creators.ConnectionCreatorJson;
import com.eqixiac.equinix.fabric.model.wrappers.ConnectionWrapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client for Fabric Connections. Standard CRUD + paging come from {@link ResourceClientBase};
 * the Connection-specific operations (dry-run, actions, statistics) remain bespoke below.
 *
 * @author ianjones
 */
public class ConnectionClientImpl extends ResourceClientBase<Connection, ConnectionJson> implements ConnectionClient<Connection> {

    private final RouteFilterAttachmentClient<RouteFilterAttachment> routeFilterAttachmentsClient;

    private final RouteAggregationAttachmentClient<RouteAggregationAttachment> routeAggregationAttachmentsClient;

    public ConnectionClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Connections", ConnectionJson.class);
        this.routeFilterAttachmentsClient = new RouteFilterAttachmentClientImpl(configClient, "Connections");
        this.routeAggregationAttachmentsClient = new RouteAggregationAttachmentClientImpl(configClient, "Connections");
    }

    @Override
    protected Connection wrap(ConnectionJson json) {
        return new ConnectionWrapper(json, this);
    }

    public Page<ConnectionJson> search(FilterPropertyList filter, SortPropertyList sort) {
        return searchPage("SearchConnections", new FilteredSortedPaginatedPost<>(filter, sort));
    }

    public List<ValidateConnectionResult> validate(FilterPropertyList filter) {
        ConnectionValidationResponseJson response = postForType("ValidateConnections",
                new ConnectionValidationRequest(filter), ConnectionValidationResponseJson.getSingleTypeRef());
        return (response != null && response.getData() != null)
                ? List.copyOf(response.getData())
                : Collections.<ValidateConnectionResult>emptyList();
    }

    public ConnectionJson getByUuid(String uuid) {
        return getOne("GetConnection", uuid);
    }

    public ConnectionJson create(ConnectionCreatorJson connectionCreatorJson) {
        return postOne("PostConnection", connectionCreatorJson);
    }

    public ConnectionJson delete(String uuid) {
        return deleteOne("DeleteConnection", uuid);
    }

    public ConnectionJson refresh(String uuid) {
        return getByUuid(uuid);
    }

    public ConnectionJson dryRunCreate(ConnectionCreatorJson connectionCreatorJson) {
        return dryRunCreate("PostConnection", connectionCreatorJson);
    }

    public ConnectionActionJson performOperation(String uuid, ConnectionOperationType connectionOperationType, String description, Object bodyObject) {
        return postForType("ManageConnection", Map.of("uuid", uuid),
                new ManageConnection(connectionOperationType, description, bodyObject),
                ConnectionActionJson.getSingleTypeRef());
    }

    public ConnectionJson update(String uuid, List<PatchOperation> operations) {
        return patchOne("UpdateConnection", uuid, operations);
    }

    public ConnectionJson dryRunUpdate(String uuid, List<PatchOperation> operations) {
        // Same wire shape as update() (json-patch+json PATCH) plus dryRun=true; nothing persists.
        return dryRunPatch("UpdateConnection", uuid, operations);
    }

    public ConnectionStatisticJson getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime, Side viewPoint) {
        Map<String, List<String>> qParams = Map.of(
                "startDateTime", ParameterMapper.singleParamList(ParameterMapper.dateTimeForQuery(startDateTime)),
                "endDateTime", ParameterMapper.singleParamList(ParameterMapper.dateTimeForQuery(endDateTime)),
                "viewPoint", ParameterMapper.singleParamList(viewPoint.toViewPoint())
        );
        return getAs("GetStatistics", Map.of("uuid", uuid), qParams, ConnectionStatisticJson.class);
    }

    public ConnectionStatisticJson refreshStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime, Side viewPoint) {
        return this.getStatistics(uuid, startDateTime, endDateTime, viewPoint);
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

    public List<RouteAggregationAttachment> getRouteAggregations(String connectionId) {
        return this.routeAggregationAttachmentsClient.getConnectionRouteAggregations(connectionId);
    }

    public RouteAggregationAttachment getRouteAggregation(String connectionId, String routeAggregationId) {
        return this.routeAggregationAttachmentsClient.getConnectionRouteAggregation(connectionId, routeAggregationId);
    }

    public RouteAggregationAttachment attachRouteAggregation(String connectionId, String routeAggregationId) {
        return this.routeAggregationAttachmentsClient.attachConnectionRouteAggregation(connectionId, routeAggregationId);
    }

    public Boolean detachRouteAggregation(String connectionId, String routeAggregationId) {
        return this.routeAggregationAttachmentsClient.detachConnectionRouteAggregation(connectionId, routeAggregationId);
    }

    public List<RouteFilterAttachment> getRouteFilters(String connectionId) {
        return this.routeFilterAttachmentsClient.getConnectionRouteFilters(connectionId);
    }

    public RouteFilterAttachment getRouteFilter(String connectionId, String routeFilterId) {
        return this.routeFilterAttachmentsClient.getConnectionRouteFilter(connectionId, routeFilterId);
    }

    public RouteFilterAttachment attachRouteFilter(String connectionId, String routeFilterId, Direction direction) {
        return this.routeFilterAttachmentsClient.attachConnectionRouteFilter(connectionId, routeFilterId, direction);
    }

    public Boolean detachRouteFilter(String connectionId, String routeFilterId) {
        return this.routeFilterAttachmentsClient.detachConnectionRouteFilter(connectionId, routeFilterId);
    }
}
