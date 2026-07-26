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
import com.eqixiac.equinix.fabric.enums.CloudRouterPackageType;

import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.fabric.enums.CloudRouterPackageCode;
import com.eqixiac.equinix.fabric.model.CloudRouterPackage;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CloudRouterPackageJson implements CloudRouterPackage {

    @Getter static TypeReference<List<CloudRouterPackageJson>> listTypeRef = new TypeReference<>() {};

    @JsonProperty("href")
    private String href;

    @JsonProperty("code")
    private CloudRouterPackageCode code;

    @JsonProperty("type")
    private CloudRouterPackageType type;

    @JsonProperty("description")
    private String description;

    @JsonProperty("totalIPv4RoutesMax")
    private Integer totalIPv4RoutesMax;

    @JsonProperty("totalIPv6RoutesMax")
    private Integer totalIPv6RoutesMax;

    @JsonProperty("routeFilterSupported")
    private Boolean routeFilterSupported;

    @JsonProperty("vcCountMax")
    private Integer vcCountMax;

    @JsonProperty("crCountMax")
    private Integer crCountMax;

    @JsonProperty("vcBandwidthMax")
    private Integer vcBandwidthMax;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;
}
