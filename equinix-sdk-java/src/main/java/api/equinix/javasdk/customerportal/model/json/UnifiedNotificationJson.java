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

package api.equinix.javasdk.customerportal.model.json;

import api.equinix.javasdk.customerportal.enums.NotificationCategory;
import api.equinix.javasdk.customerportal.enums.NotificationProductType;
import api.equinix.javasdk.customerportal.enums.NotificationStatus;
import api.equinix.javasdk.customerportal.enums.NotificationType;
import api.equinix.javasdk.customerportal.model.UnifiedNotification;
import api.equinix.javasdk.customerportal.model.implementation.AdditionalInfo;
import api.equinix.javasdk.customerportal.model.implementation.NotificationContact;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnifiedNotificationJson implements UnifiedNotification {

    @JsonProperty("id")
    private String id;

    @JsonProperty("notificationNumber")
    private String notificationNumber;

    @JsonProperty("category")
    private NotificationCategory category;

    @JsonProperty("type")
    private NotificationType type;

    @JsonProperty("productTypes")
    private List<NotificationProductType> productTypes;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("status")
    private NotificationStatus status;

    @JsonProperty("createdDateTime")
    private String createdDateTime;

    @JsonProperty("ibxs")
    private List<String> ibxs;

    @JsonProperty("contacts")
    private List<NotificationContact> contacts;

    @JsonProperty("additionalInfo")
    private List<AdditionalInfo> additionalInfo;
}
