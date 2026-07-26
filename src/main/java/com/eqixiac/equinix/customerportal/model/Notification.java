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

package com.eqixiac.equinix.customerportal.model;

import com.eqixiac.equinix.customerportal.enums.NotificationStatus;
import com.eqixiac.equinix.customerportal.enums.NotificationType;
import java.util.List;

/**
 * An IBX or network notification in the Equinix Customer Portal (Notifications v1
 * {@code ibx-notification} / {@code network-notification}).
 *
 * <p>This single model carries the union of the IBX and network notification fields:
 * {@code productTypes} is only populated for network notifications, and {@code emails} is only
 * populated when an individual notification is fetched by id (the search responses carry summaries
 * without email details).</p>
 */
public interface Notification {

    /**
     * Returns the notification identifier.
     *
     * @return the notification id
     */
    String getId();

    /**
     * Returns the notification type (e.g. {@code IBX_MAINTENANCE}, {@code NETWORK_INCIDENT}).
     *
     * @return the notification type
     */
    NotificationType getType();

    /**
     * Returns the event/notification start timestamp.
     *
     * @return the start timestamp
     */
    String getStartTimestamp();

    /**
     * Returns the event/notification end timestamp.
     *
     * @return the end timestamp, or {@code null} if not provided
     */
    String getEndTimestamp();

    /**
     * Returns the list of affected IBXs.
     *
     * @return the IBX codes
     */
    List<String> getIbxs();

    /**
     * Returns the notification status (e.g. {@code NEW}, {@code RESOLVED}).
     *
     * @return the status
     */
    NotificationStatus getStatus();

    /**
     * Returns summary information about the event/notification.
     *
     * @return the summary, or {@code null} if not provided
     */
    String getSummary();

    /**
     * Returns the affected network product types (network notifications only).
     *
     * @return the product types, or {@code null} for IBX notifications
     */
    List<String> getProductTypes();

    /**
     * Returns the notification email content (populated only when fetched by id).
     *
     * @return the emails, or {@code null} if not provided
     */
    List<? extends NotificationEmail> getEmails();
}
