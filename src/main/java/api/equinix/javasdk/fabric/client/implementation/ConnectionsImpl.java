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
import api.equinix.javasdk.core.enums.Side;
import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.client.Connections;
import api.equinix.javasdk.fabric.client.RequestBuilder;
import api.equinix.javasdk.fabric.client.internal.ConnectionClient;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.Pricing;
import api.equinix.javasdk.fabric.model.ConnectionStatistic;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.ConnectionJson;
import api.equinix.javasdk.fabric.model.json.PricingJson;
import api.equinix.javasdk.fabric.model.json.ConnectionStatisticJson;
import api.equinix.javasdk.fabric.model.json.creators.ConnectionOperator;
import api.equinix.javasdk.fabric.model.wrappers.PricingWrapper;
import api.equinix.javasdk.fabric.model.wrappers.ConnectionStatisticWrapper;
import api.equinix.javasdk.fabric.model.wrappers.ConnectionWrapper;

import java.time.LocalDateTime;

/**
 * <p>ConnectionsImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ConnectionsImpl implements Connections {

    private final Fabric serviceManager;

    private final ConnectionClient<Connection> serviceClient;

    /**
     * <p>Constructor for ConnectionsImpl.</p>
     *
     * @param serviceClient a {@link ConnectionClient} object.
     * @param serviceManager a {@link api.equinix.javasdk.Fabric} object.
     */
    public ConnectionsImpl(ConnectionClient<Connection> serviceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
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

    /** {@inheritDoc} */
    @Override
    public ConnectionStatistic getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime, Side viewPoint) {
        ConnectionStatisticJson connectionStatisticJson = this.serviceClient.getStatistics(uuid, startDateTime, endDateTime, viewPoint);
        return new ConnectionStatisticWrapper(connectionStatisticJson, this.serviceClient);
    }

    /** {@inheritDoc} */
    @Override
    public ConnectionStatistic getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return getStatistics(uuid, startDateTime, endDateTime, Side.A_Side);
    }
}
