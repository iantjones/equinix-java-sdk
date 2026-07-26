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

package com.eqixiac.equinix.fabric.model.json;

import com.eqixiac.equinix.fabric.model.HealthStatus;
import com.eqixiac.equinix.fabric.model.implementation.ApiServices;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthStatusJson implements HealthStatus {


    @JsonProperty("href")
    private String href;

    @JsonProperty("version")
    private String version;

    @JsonProperty("release")
    private String release;

    @JsonProperty("state")
    private String state;

    @JsonProperty("apiServices")
    private ApiServices apiServices;
}
