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

package com.eqixiac.equinix.networkedge.model.implementation;

import com.eqixiac.equinix.networkedge.enums.DeviceACLStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * <p>Details of an ACL template as applied to a virtual device, including its current
 * provisioning {@link com.eqixiac.equinix.networkedge.enums.DeviceACLStatus} on the device.</p>
 *
 * @author ianjones
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class DeviceACLDetail {

    @JsonProperty("name")
    private String name;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("description")
    private String description;

    @JsonProperty("inboundRules")
    private List<InboundRule> inboundRules;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdDateTime")
    private String createdDateTime;

    @JsonProperty("status")
    private DeviceACLStatus status;
}
