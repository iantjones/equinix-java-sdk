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

package com.eqixiac.equinix.networkedge.model.json;

import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.model.Lifecycle;
import com.eqixiac.equinix.core.model.deserializers.LocalDateTimeDeserializer;
import com.eqixiac.equinix.networkedge.enums.DeviceLinkStatus;
import com.eqixiac.equinix.networkedge.enums.RedundancyType;
import com.eqixiac.equinix.networkedge.model.DeviceLink;
import com.eqixiac.equinix.networkedge.model.implementation.Link;
import com.eqixiac.equinix.networkedge.model.implementation.LinkDevice;
import com.eqixiac.equinix.networkedge.model.implementation.UUIDResult;
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
