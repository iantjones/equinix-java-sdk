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

package com.eqixiac.equinix.customerportal.client;

import com.eqixiac.equinix.customerportal.model.UnifiedNotification;
import com.eqixiac.equinix.customerportal.model.json.creators.UnifiedNotificationSearchRequest;

import java.util.List;

/**
 * Client interface for searching unified order and colocation notifications in the Equinix
 * Customer Portal.
 *
 * <p>Backed by the unified notifications v2 search API at {@code /notifications/v2/events/findAll}.
 * Notifications are retrieved by supplying a {@link UnifiedNotificationSearchRequest} (filter, sort
 * and pagination criteria) to {@link #getNotifications(UnifiedNotificationSearchRequest)}.</p>
 */
public interface UnifiedNotifications {

    /**
     * Searches notification events by the supplied search criteria.
     *
     * @param request the search criteria body
     * @return the matching notification events
     */
    List<? extends UnifiedNotification> getNotifications(UnifiedNotificationSearchRequest request);
}
