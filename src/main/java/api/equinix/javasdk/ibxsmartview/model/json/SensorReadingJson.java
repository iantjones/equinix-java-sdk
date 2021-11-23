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

package api.equinix.javasdk.ibxsmartview.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.ibxsmartview.model.SensorReading;
import api.equinix.javasdk.ibxsmartview.model.implementation.Reading;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

@Getter
public class SensorReadingJson {

    @Getter static TypeReference<Page<SensorReading, SensorReadingJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<SensorReadingJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("sensorId")
    private String sensorId;

    @JsonProperty("zoneId")
    private String zoneId;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("humidity")
    private Reading humidity;

    @JsonProperty("temperature")
    private Reading temperature;
}