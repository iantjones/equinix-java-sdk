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

package api.equinix.javasdk.fabric.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.Serializable;
import api.equinix.javasdk.fabric.enums.ServiceProfileState;
import api.equinix.javasdk.fabric.enums.ServiceProfileType;
import api.equinix.javasdk.fabric.enums.ServiceProfileVisibility;
import api.equinix.javasdk.fabric.model.implementation.*;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public final class ServiceProfileJson implements Serializable {

    @Getter static TypeReference<Page<ServiceProfile, ServiceProfileJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<ServiceProfileJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<ServiceProfileJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("state")
    private ServiceProfileState state;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("name")
    private String name;

    @JsonProperty("href")
    private String href;

    @JsonProperty("description")
    private String description;

    @JsonProperty("type")
    private ServiceProfileType type;

    @JsonProperty("notifications")
    private List<Notification> notifications;

    @JsonProperty("account")
    private Account account;

    @JsonProperty("visibility")
    private ServiceProfileVisibility visibility;

    @JsonProperty("metros")
    private List<ServiceProfileMetro> metros;

    @JsonProperty("marketingInfo")
    private MarketingInfo marketingInfo;

    @JsonProperty("selfProfile")
    private Boolean selfProfile;

    @JsonProperty("accessPointTypeConfigs")
    private List<AccessPointTypeConfig> accessPointTypeConfigs;

    @JsonProperty("ports")
    private List<AccessPointTypeConfigPort> ports;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;

    @JsonProperty("customFields")
    private List<CustomField> customFields;

    @JsonProperty("allowedEmails")
    private List<String> allowedEmails;
}
