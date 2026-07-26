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

package com.eqixiac.equinix.fabric.client.implementation;

import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.client.Ports;
import com.eqixiac.equinix.fabric.client.internal.PortStatisticClient;
import com.eqixiac.equinix.fabric.client.internal.PortClient;
import com.eqixiac.equinix.fabric.model.Metric;
import com.eqixiac.equinix.fabric.model.Port;
import com.eqixiac.equinix.fabric.model.PortStatistic;
import com.eqixiac.equinix.fabric.model.PortVlan;
import com.eqixiac.equinix.fabric.model.implementation.PhysicalPort;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.PhysicalPortsResponseJson;
import com.eqixiac.equinix.fabric.model.json.PortJson;
import com.eqixiac.equinix.fabric.model.json.PortStatisticJson;
import com.eqixiac.equinix.fabric.model.json.creators.PortOperator;
import com.eqixiac.equinix.fabric.model.wrappers.PortStatisticWrapper;
import com.eqixiac.equinix.fabric.model.wrappers.PortWrapper;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author ianjones
 */
@RequiredArgsConstructor
public class PortsImpl implements Ports {

    private final PortClient<Port> serviceClient;

    private final PortStatisticClient<PortStatistic> statisticServiceClient;

    public PaginatedList<Port> list() {
        Page<PortJson> responsePage = serviceClient.list();
        PaginatedList<Port> portList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, PortWrapper::new);
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
        Page<PortJson> responsePage = serviceClient.search(filter, sort);
        PaginatedFilteredList<Port> portList = ResponseHandler.mapPaginatedFilteredList(responsePage.getItems(), this.serviceClient, PortWrapper::new);
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

    public PortOperator.PortBuilder define() {
        return new PortOperator(this.serviceClient).create();
    }

    public Port delete(String uuid) {
        PortJson portJson = this.serviceClient.delete(uuid);
        return new PortWrapper(portJson, this.serviceClient);
    }

    public Port delete(String uuid, boolean dryRun) {
        PortJson portJson = dryRun
                ? this.serviceClient.dryRunDelete(uuid)
                : this.serviceClient.delete(uuid);
        return new PortWrapper(portJson, this.serviceClient);
    }

    public PortOperator.PortUpdater update(String uuid) {
        return new PortOperator(this.serviceClient).update(uuid);
    }

    public PhysicalPortsResponseJson addToLag(String portId, List<PhysicalPort> physicalPorts) {
        return this.serviceClient.addToLag(portId, physicalPorts);
    }
}
