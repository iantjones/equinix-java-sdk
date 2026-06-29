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

package api.equinix.javasdk.fabric.model.json.creators;

import api.equinix.javasdk.fabric.enums.ServiceProfileType;
import api.equinix.javasdk.fabric.enums.ServiceProfileVisibility;
import api.equinix.javasdk.fabric.model.implementation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Setter;

import java.util.List;

@Setter(AccessLevel.PRIVATE)
public class ServiceProfileCreatorJson {

    @JsonProperty("type")
    private ServiceProfileType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("notifications")
    private List<Notification> notifications;

    @JsonProperty("visibility")
    private ServiceProfileVisibility visibility;

    @JsonProperty("allowedEmails")
    private List<String> allowedEmails;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("ports")
    private List<AccessPointTypeConfigPort> ports;

    @JsonProperty("accessPointTypeConfigs")
    private List<AccessPointTypeConfig> accessPointTypeConfigs;

    @JsonProperty("customFields")
    private List<CustomField> customFields;

    @JsonProperty("marketingInfo")
    private MarketingInfo marketingInfo;

    public ServiceProfileCreatorJson(ServiceProfileOperator.ServiceProfileBuilder serviceProfileBuilder) {
        this.type = serviceProfileBuilder.getType();
        this.name = serviceProfileBuilder.getName();
        this.description = serviceProfileBuilder.getDescription();
        this.notifications = serviceProfileBuilder.getNotifications();
        this.visibility = serviceProfileBuilder.getVisibility();
        this.allowedEmails = serviceProfileBuilder.getAllowedEmails();
        this.tags = serviceProfileBuilder.getTags();
        this.ports = serviceProfileBuilder.getPorts();
        this.accessPointTypeConfigs = serviceProfileBuilder.getAccessPointTypeConfigs();
        this.customFields = serviceProfileBuilder.getCustomFields();
        this.marketingInfo = serviceProfileBuilder.getMarketingInfo();
    }
}
