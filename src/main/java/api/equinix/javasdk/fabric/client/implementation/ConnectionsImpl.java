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

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.Direction;
import api.equinix.javasdk.fabric.enums.Side;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.client.Connections;
import api.equinix.javasdk.fabric.client.RequestBuilder;
import api.equinix.javasdk.fabric.client.internal.ConnectionClient;
import api.equinix.javasdk.fabric.client.internal.RouteTableEntryClient;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.Metric;
import api.equinix.javasdk.fabric.model.Pricing;
import api.equinix.javasdk.fabric.model.ConnectionStatistic;
import api.equinix.javasdk.fabric.model.RouteAggregationAttachment;
import api.equinix.javasdk.fabric.model.RouteFilterAttachment;
import api.equinix.javasdk.fabric.model.RouteTableEntry;
import api.equinix.javasdk.fabric.model.ValidateConnectionResult;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.ConnectionJson;
import api.equinix.javasdk.fabric.model.json.PricingJson;
import api.equinix.javasdk.fabric.model.json.ConnectionStatisticJson;
import api.equinix.javasdk.fabric.model.json.RouteTableEntryJson;
import api.equinix.javasdk.fabric.model.json.creators.ConnectionOperator;
import api.equinix.javasdk.fabric.model.wrappers.PricingWrapper;
import api.equinix.javasdk.fabric.model.wrappers.ConnectionStatisticWrapper;
import api.equinix.javasdk.fabric.model.wrappers.ConnectionWrapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author ianjones
 */
public class ConnectionsImpl implements Connections {

    private final ConnectionClient<Connection> serviceClient;

    private final RouteTableEntryClient<RouteTableEntry> routesClient;

    public ConnectionsImpl(ConnectionClient<Connection> serviceClient, RouteTableEntryClient<RouteTableEntry> routesClient) {
        this.serviceClient = serviceClient;
        this.routesClient = routesClient;
    }

    public PaginatedFilteredList<Connection> search() {
        return search(Filter.filter().empty());
    }

    public PaginatedFilteredList<Connection> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    public PaginatedFilteredList<Connection> search(SortPropertyList sort) {
        return search(null, sort);
    }

    public PaginatedFilteredList<Connection> search(FilterPropertyList filter, SortPropertyList sort) {
        Page<Connection, ConnectionJson> responsePage = serviceClient.search(filter, sort);
        PaginatedFilteredList<Connection> connectionList = Utils.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, ConnectionWrapper::new);
        return new PaginatedFilteredList<>(connectionList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    @Override
    public List<ValidateConnectionResult> validate(FilterPropertyList filter) {
        return this.serviceClient.validate(filter);
    }

    public Connection getByUuid(String uuid) {
        ConnectionJson connectionJson = serviceClient.getByUuid(uuid);
        return new ConnectionWrapper(connectionJson, this.serviceClient);
    }

    public ConnectionOperator.ConnectionBuilder define(ConnectionType connectionType) {
        return new ConnectionOperator(this.serviceClient).create(connectionType);
    }

    public ConnectionOperator.BatchConnectionBuilder startBatch() {
        return new ConnectionOperator(this.serviceClient).batch();
    }

    @Deprecated
    @Override
    public ConnectionStatistic getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime, Side viewPoint) {
        ConnectionStatisticJson connectionStatisticJson = this.serviceClient.getStatistics(uuid, startDateTime, endDateTime, viewPoint);
        return new ConnectionStatisticWrapper(connectionStatisticJson, this.serviceClient);
    }

    @Deprecated
    @Override
    public ConnectionStatistic getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return getStatistics(uuid, startDateTime, endDateTime, Side.A_Side);
    }

    @Override
    public List<Metric> getMetrics(String uuid, String name, LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        return this.serviceClient.getMetrics(uuid, name, fromDateTime, toDateTime);
    }

    @Override
    public PaginatedFilteredList<RouteTableEntry> searchAdvertisedRoutes(String uuid) {
        return searchAdvertisedRoutes(uuid, null, null);
    }

    @Override
    public PaginatedFilteredList<RouteTableEntry> searchAdvertisedRoutes(String uuid, FilterPropertyList filter, SortPropertyList sort) {
        Page<RouteTableEntry, RouteTableEntryJson> responsePage = this.routesClient.searchAdvertisedRoutes(uuid, filter, sort);
        PaginatedFilteredList<RouteTableEntry> routes = Utils.mapPaginatedFilteredList(responsePage.getItems(), this.routesClient, (json, client) -> json);
        return new PaginatedFilteredList<>(routes, this.routesClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    @Override
    public PaginatedFilteredList<RouteTableEntry> searchReceivedRoutes(String uuid) {
        return searchReceivedRoutes(uuid, null, null);
    }

    @Override
    public PaginatedFilteredList<RouteTableEntry> searchReceivedRoutes(String uuid, FilterPropertyList filter, SortPropertyList sort) {
        Page<RouteTableEntry, RouteTableEntryJson> responsePage = this.routesClient.searchReceivedRoutes(uuid, filter, sort);
        PaginatedFilteredList<RouteTableEntry> routes = Utils.mapPaginatedFilteredList(responsePage.getItems(), this.routesClient, (json, client) -> json);
        return new PaginatedFilteredList<>(routes, this.routesClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    @Override
    public List<RouteAggregationAttachment> getRouteAggregations(String connectionId) {
        return this.serviceClient.getRouteAggregations(connectionId);
    }

    @Override
    public RouteAggregationAttachment getRouteAggregation(String connectionId, String routeAggregationId) {
        return this.serviceClient.getRouteAggregation(connectionId, routeAggregationId);
    }

    @Override
    public RouteAggregationAttachment attachRouteAggregation(String connectionId, String routeAggregationId) {
        return this.serviceClient.attachRouteAggregation(connectionId, routeAggregationId);
    }

    @Override
    public Boolean detachRouteAggregation(String connectionId, String routeAggregationId) {
        return this.serviceClient.detachRouteAggregation(connectionId, routeAggregationId);
    }

    @Override
    public List<RouteFilterAttachment> getRouteFilters(String connectionId) {
        return this.serviceClient.getRouteFilters(connectionId);
    }

    @Override
    public RouteFilterAttachment getRouteFilter(String connectionId, String routeFilterId) {
        return this.serviceClient.getRouteFilter(connectionId, routeFilterId);
    }

    @Override
    public RouteFilterAttachment attachRouteFilter(String connectionId, String routeFilterId, Direction direction) {
        return this.serviceClient.attachRouteFilter(connectionId, routeFilterId, direction);
    }

    @Override
    public Boolean detachRouteFilter(String connectionId, String routeFilterId) {
        return this.serviceClient.detachRouteFilter(connectionId, routeFilterId);
    }
}
