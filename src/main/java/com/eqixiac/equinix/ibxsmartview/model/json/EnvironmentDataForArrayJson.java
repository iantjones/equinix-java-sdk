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

package com.eqixiac.equinix.ibxsmartview.model.json;

import com.eqixiac.equinix.ibxsmartview.model.EnvironmentDataForArray;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentDataForArrayJson implements EnvironmentDataForArray {

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("accountNo")
    private String accountNo;

    @JsonProperty("zone")
    private String zone;

    @JsonProperty("cage")
    private String cage;

    @JsonProperty("cabinet")
    private String cabinet;

    @JsonProperty("sensor")
    private String sensor;

    @JsonProperty("temperature")
    private String temperature;

    @JsonProperty("humidity")
    private String humidity;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("temperatureUom")
    private String temperatureUom;

    @JsonProperty("humidityUom")
    private String humidityUom;
}
