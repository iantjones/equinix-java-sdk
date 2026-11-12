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
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

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

    // The fabricv4 spec types apiServices as one object, but the live service returns an array of
    // per-route statuses (checked 2026-11-11). Both shapes are accepted.
    @JsonProperty("apiServices")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @Getter(lombok.AccessLevel.NONE)
    private List<ApiServices> apiServices;

    @Override
    public ApiServices getApiServices() {
        return apiServices == null || apiServices.isEmpty() ? null : apiServices.get(0);
    }

    @Override
    public List<ApiServices> getAllApiServices() {
        return apiServices == null ? List.of() : List.copyOf(apiServices);
    }
}
