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

package api.equinix.javasdk.networkedge.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.Lifecycle;
import api.equinix.javasdk.core.model.deserializers.LocalDateTimeDeserializer;
import api.equinix.javasdk.networkedge.enums.DeviceLinkStatus;
import api.equinix.javasdk.networkedge.enums.RedundancyType;
import api.equinix.javasdk.networkedge.enums.Source;
import api.equinix.javasdk.networkedge.model.DeviceLink;
import api.equinix.javasdk.networkedge.model.implementation.DeviceLinkSupportDetail;
import api.equinix.javasdk.networkedge.model.implementation.Link;
import api.equinix.javasdk.networkedge.model.implementation.LinkDevice;
import api.equinix.javasdk.networkedge.model.implementation.UUIDResult;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author ianjones
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class DeviceLinkJson extends Lifecycle {

    @Getter static TypeReference<UUIDResult> createTypeRef = new TypeReference<>() {};

    @JsonAlias("id")
    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("source")
    private Source source;

    @JsonProperty("groupName")
    private String groupName;

    @JsonProperty("subnet")
    private String subnet;

    @JsonProperty("redundancyType")
    private RedundancyType redundancyType;

    @JsonProperty("status")
    private DeviceLinkStatus status;

    @JsonAlias("links")
    @JsonProperty("metroLinks")
    private List<Link> metroLinks;

    @JsonProperty("linkDevices")
    private List<LinkDevice> linkDevices;

    @JsonProperty("supportDetails")
    private List<DeviceLinkSupportDetail> supportDetails;

    // Network Edge responses use *DateTime audit fields rather than the shared Lifecycle *Date names.
    @JsonProperty("createdDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdDateTime;

    @JsonProperty("lastUpdatedDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime lastUpdatedDateTime;

    @JsonProperty("deletedDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime deletedDateTime;

    @Override
    public LocalDateTime getCreatedDate() {
        return createdDateTime != null ? createdDateTime : super.getCreatedDate();
    }

    @Override
    public LocalDateTime getLastUpdatedDate() {
        return lastUpdatedDateTime != null ? lastUpdatedDateTime : super.getLastUpdatedDate();
    }

    @Override
    public LocalDateTime getDeletedDate() {
        return deletedDateTime != null ? deletedDateTime : super.getDeletedDate();
    }
}
