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

package api.equinix.javasdk.fabric.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Delivery window range for a last-mile catalog (the Fabric v4
 * {@code ServiceProfileLastMileDeliveryDateRange} schema).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceProfileLastMileDeliveryDateRange {

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("min")
    private Integer min;

    @JsonProperty("max")
    private Integer max;
}
