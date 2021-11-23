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
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.Sortable;
import api.equinix.javasdk.fabric.client.RequestBuilder;
import api.equinix.javasdk.fabric.enums.StatisticDuration;
import api.equinix.javasdk.fabric.model.PortStatistic;
import api.equinix.javasdk.fabric.model.json.PortStatisticJson;

import java.time.LocalDateTime;

/**
 * <p>PortStatisticsClient interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface PortStatisticClient<T> extends Pageable<T> {

    /**
     * <p>getStatistics.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param startDateTime a {@link java.time.LocalDateTime} object.
     * @param endDateTime a {@link java.time.LocalDateTime} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.PortStatisticJson} object.
     */
    PortStatisticJson getStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime);

    /**
     * <p>getTopStatistics.</p>
     *
     * @param duration a {@link api.equinix.javasdk.fabric.enums.StatisticDuration} object.
     * @param sortable a {@link api.equinix.javasdk.core.model.Sortable} object.
     * @param requestBuilder a {@link api.equinix.javasdk.fabric.client.RequestBuilder.TopPortStatistics} object.
     * @return a {@link api.equinix.javasdk.core.http.response.Page} object.
     */
    Page<PortStatistic, PortStatisticJson> getTopStatistics(StatisticDuration duration, Sortable sortable, RequestBuilder.TopPortStatistics requestBuilder);

    /**
     * <p>refreshStatistics.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param startDateTime a {@link java.time.LocalDateTime} object.
     * @param endDateTime a {@link java.time.LocalDateTime} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.PortStatisticJson} object.
     */
    PortStatisticJson refreshStatistics(String uuid, LocalDateTime startDateTime, LocalDateTime endDateTime);
}
