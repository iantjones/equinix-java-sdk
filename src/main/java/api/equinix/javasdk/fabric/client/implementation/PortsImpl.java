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
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.Sortable;
import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.fabric.client.Ports;
import api.equinix.javasdk.fabric.client.RequestBuilder;
import api.equinix.javasdk.fabric.client.internal.PortStatisticClient;
import api.equinix.javasdk.fabric.client.internal.PortClient;
import api.equinix.javasdk.fabric.enums.StatisticDuration;
import api.equinix.javasdk.fabric.model.Port;
import api.equinix.javasdk.fabric.model.PortStatistic;
import api.equinix.javasdk.fabric.model.json.PortJson;
import api.equinix.javasdk.fabric.model.json.PortStatisticJson;
import api.equinix.javasdk.fabric.model.wrappers.PortStatisticWrapper;
import api.equinix.javasdk.fabric.model.wrappers.PortWrapper;

import java.time.LocalDateTime;

/**
 * <p>PortsImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class PortsImpl implements Ports {

    private final Fabric serviceManager;

    private final PortClient<Port> serviceClient;

    private final PortStatisticClient<PortStatistic> statisticServiceClient;

    /**
     * <p>Constructor for PortsImpl.</p>
     *
     * @param serviceClient a {@link PortClient} object.
     * @param statisticServiceClient a {@link PortStatisticClient} object.
     * @param serviceManager a {@link api.equinix.javasdk.Fabric} object.
     */
    public PortsImpl(PortClient<Port> serviceClient, PortStatisticClient<PortStatistic> statisticServiceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
        this.statisticServiceClient = statisticServiceClient;
    }

    /**
     * <p>list.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.PaginatedList} object.
     */
    public PaginatedList<Port> list() {
        Page<Port, PortJson> responsePage = serviceClient.list();
        PaginatedList<Port> portList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, PortWrapper::new);
        return new PaginatedList<>(portList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    /** {@inheritDoc} */
    public Port getByUuid(String uuid) {
        PortJson portJson = serviceClient.getByUuid(uuid);
        return new PortWrapper(portJson, this.serviceClient);
    }

    /** {@inheritDoc} */
    public PortStatistic getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        PortStatisticJson portStatisticJson = statisticServiceClient.getStatistics(uuid, startDateTime, endDateTime);
        return new PortStatisticWrapper(portStatisticJson, this.statisticServiceClient);
    }

    /** {@inheritDoc} */
    public PaginatedList<PortStatistic> getTopStatistics(StatisticDuration duration, Sortable sortable) {
        return getTopStatistics(duration, sortable, null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>getTopStatistics.</p>
     */
    public PaginatedList<PortStatistic> getTopStatistics(StatisticDuration duration, Sortable sortable, RequestBuilder.TopPortStatistics requestBuilder) {
        Page<PortStatistic, PortStatisticJson> responsePage = statisticServiceClient.getTopStatistics(duration, sortable, requestBuilder);
        PaginatedList<PortStatistic> portStatisticsList = Utils.mapPaginatedList(responsePage.getItems(), this.statisticServiceClient, PortStatisticWrapper::new);
        return new PaginatedList<>(portStatisticsList, this.statisticServiceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }
}
