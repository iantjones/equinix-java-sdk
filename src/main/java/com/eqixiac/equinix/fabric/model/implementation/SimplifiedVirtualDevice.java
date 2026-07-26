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

package com.eqixiac.equinix.fabric.model.implementation;

import com.eqixiac.equinix.fabric.enums.VirtualDeviceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The Network Edge virtual device used at an access-point selector (spec schema
 * {@code SimplifiedVirtualDevice}). Only {@code uuid} is sent on requests; the remaining
 * members are populated on service-token reads.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimplifiedVirtualDevice extends VirtualDeviceRef {

    public SimplifiedVirtualDevice(String uuid) {
        super(uuid);
    }

    @JsonProperty("href")
    private String href;

    /**
     * Customer-assigned virtual device name.
     */
    @JsonProperty("name")
    private String name;

    /**
     * Type of virtual device ({@code EDGE}).
     */
    @JsonProperty("type")
    private VirtualDeviceType type;

    @JsonProperty("account")
    private AccountSummary account;

    /**
     * Virtual Device Cluster information.
     */
    @JsonProperty("cluster")
    private String cluster;
}
