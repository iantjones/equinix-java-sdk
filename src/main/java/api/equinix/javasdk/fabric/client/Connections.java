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

package api.equinix.javasdk.fabric.client;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Side;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.Pricing;
import api.equinix.javasdk.fabric.model.ConnectionStatistic;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.creators.ConnectionOperator;

import java.time.LocalDateTime;

/**
 * <p>Connections interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface Connections {

    PaginatedFilteredList<Connection> search();

    PaginatedFilteredList<Connection> search(FilterPropertyList filter);

    PaginatedFilteredList<Connection> search(SortPropertyList sort);

    PaginatedFilteredList<Connection> search(FilterPropertyList filter, SortPropertyList sort);

    Connection getByUuid(String uuid);

    ConnectionOperator.ConnectionBuilder define(ConnectionType connectionType);

    ConnectionOperator.BatchConnectionBuilder startBatch();

    /**
     * <p>getStatistics.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param startDateTime a {@link java.time.LocalDateTime} object.
     * @param endDateTime a {@link java.time.LocalDateTime} object.
     * @param viewPoint a {@link api.equinix.javasdk.core.enums.Side} object.
     * @return a {@link api.equinix.javasdk.fabric.model.ConnectionStatistic} object.
     */
    ConnectionStatistic getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime, Side viewPoint);

    /**
     * <p>getStatistics.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param startDateTime a {@link java.time.LocalDateTime} object.
     * @param endDateTime a {@link java.time.LocalDateTime} object.
     * @return a {@link api.equinix.javasdk.fabric.model.ConnectionStatistic} object.
     */
    ConnectionStatistic getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime);
}
