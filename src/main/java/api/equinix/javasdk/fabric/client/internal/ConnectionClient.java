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

package api.equinix.javasdk.fabric.client.internal;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Side;
import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.fabric.client.RequestBuilder;
import api.equinix.javasdk.fabric.enums.ConnectionOperationType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.ConnectionJson;
import api.equinix.javasdk.fabric.model.json.PricingJson;
import api.equinix.javasdk.fabric.model.json.ConnectionStatisticJson;
import api.equinix.javasdk.fabric.model.json.creators.ConnectionCreatorJson;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>ConnectionsClient interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface ConnectionClient<T> extends PageablePost<T> {

    Page<Connection, ConnectionJson> search(FilterPropertyList filter, SortPropertyList sort);

    ConnectionJson getByUuid(String uuid);

    ConnectionJson create(ConnectionCreatorJson connectionCreatorJson);

    ConnectionJson dryRunCreate(ConnectionCreatorJson connectionCreatorJson);

    ConnectionJson performOperation(String uuid, ConnectionOperationType connectionOperationType, String description, Object bodyObject);

    List<Connection> batch(List<ConnectionCreatorJson> connectionCreatorJsonList);

    /**
     * <p>getStatistics.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param startDateTime a {@link java.time.LocalDateTime} object.
     * @param endDateTime a {@link java.time.LocalDateTime} object.
     * @param viewPoint a {@link api.equinix.javasdk.core.enums.Side} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.ConnectionStatisticJson} object.
     */
    ConnectionStatisticJson getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime, Side viewPoint);

    /**
     * <p>refreshStatistics.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param startDateTime a {@link java.time.LocalDateTime} object.
     * @param endDateTime a {@link java.time.LocalDateTime} object.
     * @param viewPoint a {@link api.equinix.javasdk.core.enums.Side} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.ConnectionStatisticJson} object.
     */
    ConnectionStatisticJson refreshStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime, Side viewPoint);
}
