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

import api.equinix.javasdk.core.model.Serializable;
import api.equinix.javasdk.ibxsmartview.model.EnvironmentData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentDataJson implements EnvironmentData, Serializable {

    @Getter static TypeReference<EnvironmentDataJson> singleTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<EnvironmentDataJson>> listTypeRef = new TypeReference<>() {};

    @JsonProperty("temperature")
    private Double temperature;

    @JsonProperty("humidity")
    private Double humidity;

    @JsonProperty("dewPoint")
    private Double dewPoint;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("levelType")
    private String levelType;

    @JsonProperty("levelValue")
    private String levelValue;

    @JsonProperty("sensorId")
    private String sensorId;

    @JsonProperty("timestamp")
    private String timestamp;
}
