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

import java.util.List;

/**
 * <p>DevicePricingDetail class. Pricing and other details of a Siebel order, returned on
 * the virtual device detail response.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class DevicePricingDetail {

    @JsonProperty("termLength")
    private String termLength;

    @JsonProperty("orderNumber")
    private String orderNumber;

    @JsonProperty("core")
    private Integer core;

    @JsonProperty("throughput")
    private String throughput;

    @JsonProperty("throughputUnit")
    private String throughputUnit;

    @JsonProperty("packageCode")
    private String packageCode;

    @JsonProperty("additionalBandwidth")
    private String additionalBandwidth;

    @JsonProperty("primary")
    private DeviceCharges primary;

    @JsonProperty("secondary")
    private DeviceCharges secondary;

    @Getter
    public static class DeviceCharges {

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("charges")
        private List<Charge> charges;
    }
}
