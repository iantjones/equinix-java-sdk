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

package api.equinix.javasdk.customerportal.client;

import api.equinix.javasdk.customerportal.model.Notification;
import api.equinix.javasdk.customerportal.model.json.creators.NotificationSearchRequest;

import java.util.List;

/**
 * Client interface for accessing IBX and network notifications in the Equinix Customer Portal.
 *
 * <p>Backed by the Notifications v1 API at {@code /v1/notifications}. IBX and network notifications
 * are searched separately and retrieved individually by id.</p>
 */
public interface Notifications {

    /**
     * Searches IBX notifications (maintenance, incidents, advisories).
     *
     * <p>Maps to {@code POST /v1/notifications/ibx/search} ({@code search-ibx-notifications}).</p>
     *
     * @param request the search request body
     * @return the matching IBX notifications
     */
    List<? extends Notification> searchIbx(NotificationSearchRequest request);

    /**
     * Searches IBX notifications (maintenance, incidents, advisories), paging the results.
     *
     * <p>Maps to {@code POST /v1/notifications/ibx/search} ({@code search-ibx-notifications}).</p>
     *
     * @param request the search request body
     * @param offset  the index of the first item returned (zero-based), or {@code null} for the default
     * @param limit   the maximum number of items returned per page, or {@code null} for the default
     * @return the matching IBX notifications
     */
    List<? extends Notification> searchIbx(NotificationSearchRequest request, Integer offset, Integer limit);

    /**
     * Searches network notifications (network maintenance and incidents).
     *
     * <p>Maps to {@code POST /v1/notifications/network/search} ({@code search-network-notifications}).</p>
     *
     * @param request the search request body
     * @return the matching network notifications
     */
    List<? extends Notification> searchNetwork(NotificationSearchRequest request);

    /**
     * Searches network notifications (network maintenance and incidents), paging the results.
     *
     * <p>Maps to {@code POST /v1/notifications/network/search} ({@code search-network-notifications}).</p>
     *
     * @param request the search request body
     * @param offset  the index of the first item returned (zero-based), or {@code null} for the default
     * @param limit   the maximum number of items returned per page, or {@code null} for the default
     * @return the matching network notifications
     */
    List<? extends Notification> searchNetwork(NotificationSearchRequest request, Integer offset, Integer limit);

    /**
     * Retrieves an IBX notification by id.
     *
     * <p>Maps to {@code GET /v1/notifications/ibx/{id}} ({@code get-ibx-Notification}).</p>
     *
     * @param id the notification id
     * @return the matching IBX notification
     */
    Notification getIbxById(String id);

    /**
     * Retrieves a network notification by id.
     *
     * <p>Maps to {@code GET /v1/notifications/network/{id}} ({@code get-network-Notification}).</p>
     *
     * @param id the notification id
     * @return the matching network notification
     */
    Notification getNetworkById(String id);
}
