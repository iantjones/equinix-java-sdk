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

package api.equinix.javasdk.networkedge.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;

/**
 *
 * @author ianjones
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Charge {

    /**
     * <p>The description of the charge. The spec models this as an open string (for example
     * {@code VIRTUAL_DEVICE}, {@code DEVICE_LICENSE} or {@code ADDITIONAL_BANDWIDTH}), so it is not
     * narrowed to an enum here.</p>
     */
    @JsonProperty("description")
    private String description;

    @JsonProperty("monthlyRecurringCharges")
    private BigDecimal monthlyRecurringCharge;
}
