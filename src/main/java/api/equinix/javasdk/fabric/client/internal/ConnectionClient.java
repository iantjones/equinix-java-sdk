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

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Side;
import api.equinix.javasdk.fabric.client.RequestBuilder;
import api.equinix.javasdk.fabric.model.json.ConnectionPricingJson;
import api.equinix.javasdk.fabric.model.json.ConnectionStatisticJson;

import java.time.LocalDateTime;

/**
 * <p>ConnectionsClient interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface ConnectionClient<T> extends Pageable<T> {

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
     * <p>getPricing.</p>
     *
     * @param portUuid a {@link java.lang.String} object.
     * @param destinationMetro a {@link api.equinix.javasdk.core.enums.MetroCode} object.
     * @param requestBuilder a {@link api.equinix.javasdk.fabric.client.RequestBuilder.ConnectionPricing} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.ConnectionPricingJson} object.
     */
    ConnectionPricingJson getPricing(String portUuid, MetroCode destinationMetro, RequestBuilder.ConnectionPricing requestBuilder);

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
