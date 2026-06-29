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

import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.Sortable;
import api.equinix.javasdk.fabric.client.Ports;
import api.equinix.javasdk.fabric.client.RequestBuilder;
import api.equinix.javasdk.fabric.client.internal.PortStatisticClient;
import api.equinix.javasdk.fabric.client.internal.PortClient;
import api.equinix.javasdk.fabric.enums.StatisticDuration;
import api.equinix.javasdk.fabric.model.Metric;
import api.equinix.javasdk.fabric.model.Port;
import api.equinix.javasdk.fabric.model.PortStatistic;
import api.equinix.javasdk.fabric.model.PortVlan;
import api.equinix.javasdk.fabric.model.implementation.PhysicalPort;
import api.equinix.javasdk.fabric.model.implementation.filter.Filter;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.PhysicalPortsResponseJson;
import api.equinix.javasdk.fabric.model.json.PortJson;
import api.equinix.javasdk.fabric.model.json.PortStatisticJson;
import api.equinix.javasdk.fabric.model.json.creators.PortOperator;
import api.equinix.javasdk.fabric.model.wrappers.PortStatisticWrapper;
import api.equinix.javasdk.fabric.model.wrappers.PortWrapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author ianjones
 */
public class PortsImpl implements Ports {

    private final PortClient<Port> serviceClient;

    private final PortStatisticClient<PortStatistic> statisticServiceClient;

    public PortsImpl(PortClient<Port> serviceClient, PortStatisticClient<PortStatistic> statisticServiceClient) {
        this.serviceClient = serviceClient;
        this.statisticServiceClient = statisticServiceClient;
    }

    public PaginatedList<Port> list() {
        Page<Port, PortJson> responsePage = serviceClient.list();
        PaginatedList<Port> portList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, PortWrapper::new);
        return new PaginatedList<>(portList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PaginatedFilteredList<Port> search() {
        return search(Filter.filter().empty());
    }

    public PaginatedFilteredList<Port> search(FilterPropertyList filter) {
        return search(filter, null);
    }

    public PaginatedFilteredList<Port> search(SortPropertyList sort) {
        return search(null, sort);
    }

    public PaginatedFilteredList<Port> search(FilterPropertyList filter, SortPropertyList sort) {
        Page<Port, PortJson> responsePage = serviceClient.search(filter, sort);
        PaginatedFilteredList<Port> portList = Utils.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, PortWrapper::new);
        return new PaginatedFilteredList<>(portList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public Port getByUuid(String uuid) {
        PortJson portJson = serviceClient.getByUuid(uuid);
        return new PortWrapper(portJson, this.serviceClient);
    }

    public List<PortVlan> getVlans(String portUuid) {
        return serviceClient.getVlans(portUuid);
    }

    @Deprecated
    public PortStatistic getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        PortStatisticJson portStatisticJson = statisticServiceClient.getStatistics(uuid, startDateTime, endDateTime);
        return new PortStatisticWrapper(portStatisticJson, this.statisticServiceClient);
    }

    public List<Metric> getMetrics(String uuid, String name, LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        return statisticServiceClient.getMetrics(uuid, name, fromDateTime, toDateTime);
    }

    public PaginatedList<PortStatistic> getTopStatistics(StatisticDuration duration, Sortable sortable) {
        return getTopStatistics(duration, sortable, null);
    }

    /**
     * {@inheritDoc}
     *
     */
    public PaginatedList<PortStatistic> getTopStatistics(StatisticDuration duration, Sortable sortable, RequestBuilder.TopPortStatistics requestBuilder) {
        Page<PortStatistic, PortStatisticJson> responsePage = statisticServiceClient.getTopStatistics(duration, sortable, requestBuilder);
        PaginatedList<PortStatistic> portStatisticsList = Utils.mapPaginatedList(responsePage.getItems(), this.statisticServiceClient, PortStatisticWrapper::new);
        return new PaginatedList<>(portStatisticsList, this.statisticServiceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PortOperator.PortBuilder define() {
        return new PortOperator(this.serviceClient).create();
    }

    public Port delete(String uuid) {
        PortJson portJson = this.serviceClient.delete(uuid);
        return new PortWrapper(portJson, this.serviceClient);
    }

    public PortOperator.PortUpdater update(String uuid) {
        return new PortOperator(this.serviceClient).update(uuid);
    }

    public PhysicalPortsResponseJson addToLag(String portId, List<PhysicalPort> physicalPorts) {
        return this.serviceClient.addToLag(portId, physicalPorts);
    }
}
