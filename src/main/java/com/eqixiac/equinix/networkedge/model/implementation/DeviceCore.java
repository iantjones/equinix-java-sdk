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

import com.eqixiac.equinix.networkedge.enums.StorageUnit;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.ArrayList;

/**
 *
 * @author ianjones
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class DeviceCore {

    @JsonProperty("core")
    private Integer core;

    @JsonProperty("memory")
    private Integer memory;

    @JsonProperty("unit")
    private StorageUnit unit;

    @JsonProperty("flavor")
    private String flavor;

    @JsonProperty("packageCodes")
    private ArrayList<PackageCodeDetail> packageCodes;

    @JsonProperty("supported")
    private Boolean supported;

    // The spec types tier as one integer; the live /ne/v1/deviceTypes returns an array of tiers for
    // some device types (checked 2026-11-11). The shared mapper accepts a single value as a list.
    @JsonProperty("tier")
    @Getter(lombok.AccessLevel.NONE)
    private java.util.List<Integer> tier;

    /** @return the first throughput tier, or {@code null}; see {@link #getTiers()} for all of them */
    public Integer getTier() {
        return tier == null || tier.isEmpty() ? null : tier.get(0);
    }

    /** @return every throughput tier the device type offers for this core count, never {@code null} */
    public java.util.List<Integer> getTiers() {
        return tier == null ? java.util.List.of() : java.util.List.copyOf(tier);
    }
}
