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
import com.eqixiac.equinix.networkedge.enums.DeviceACLStatus;
import com.eqixiac.equinix.networkedge.model.ACLTemplate;
import com.eqixiac.equinix.networkedge.model.implementation.InboundRule;
import com.eqixiac.equinix.networkedge.model.implementation.VirtualDeviceACLDetail;
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
public class ACLTemplateJson extends Lifecycle {


    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("inboundRules")
    List<InboundRule> inboundRules;

    @JsonProperty("virtualDeviceDetails")
    List<VirtualDeviceACLDetail> virtualDeviceDetails;

    @JsonProperty("status")
    private DeviceACLStatus status;

    // Network Edge responses use *DateTime audit fields rather than the shared Lifecycle *Date names.
    @JsonProperty("createdDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdDateTime;

    @Override
    public LocalDateTime getCreatedDate() {
        return createdDateTime != null ? createdDateTime : super.getCreatedDate();
    }
}
