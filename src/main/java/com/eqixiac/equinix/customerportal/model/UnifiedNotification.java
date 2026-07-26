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

import com.eqixiac.equinix.customerportal.enums.NotificationCategory;
import com.eqixiac.equinix.customerportal.enums.NotificationProductType;
import com.eqixiac.equinix.customerportal.enums.NotificationStatus;
import com.eqixiac.equinix.customerportal.enums.NotificationType;
import com.eqixiac.equinix.customerportal.model.implementation.AdditionalInfo;
import com.eqixiac.equinix.customerportal.model.implementation.NotificationContact;

import java.util.List;

/**
 * A unified order / colocation notification event returned by the Customer Portal unified
 * notifications v2 search API.
 */
public interface UnifiedNotification {

    String getId();

    String getNotificationNumber();

    NotificationCategory getCategory();

    NotificationType getType();

    List<NotificationProductType> getProductTypes();

    String getSummary();

    NotificationStatus getStatus();

    String getCreatedDateTime();

    List<String> getIbxs();

    List<NotificationContact> getContacts();

    List<AdditionalInfo> getAdditionalInfo();
}
