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
import api.equinix.javasdk.ibxsmartview.model.PowerReading;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PowerReadingJson implements PowerReading {

    @Getter static TypeReference<Page<PowerReading, PowerReadingJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<PowerReadingJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("cageId")
    private String cageId;

    @JsonProperty("cabinetId")
    private String cabinetId;

    @JsonProperty("phase")
    private String phase;

    @JsonProperty("currentAmps")
    private Double currentAmps;

    @JsonProperty("apparentPower")
    private Double apparentPower;

    @JsonProperty("activePower")
    private Double activePower;

    @JsonProperty("voltage")
    private Double voltage;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("status")
    private String status;
}
