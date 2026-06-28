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

package api.equinix.javasdk.fabric.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.fabric.enums.PrecisionTimePackageCode;
import api.equinix.javasdk.fabric.model.TimeServicePackage;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public final class TimeServicePackageJson implements TimeServicePackage {

    @Getter static TypeReference<List<TimeServicePackageJson>> listTypeRef = new TypeReference<>() {};

    @Getter static TypeReference<Page<TimeServicePackage, TimeServicePackageJson>> pagedTypeRef = new TypeReference<>() {};

    @JsonProperty("href")
    private String href;

    @JsonProperty("type")
    private String type;

    @JsonProperty("code")
    private PrecisionTimePackageCode code;

    @JsonProperty("bandwidth")
    private Integer bandwidth;

    @JsonProperty("clientsPerSecondMax")
    private Integer clientsPerSecondMax;

    @JsonProperty("redundancySupported")
    private Boolean redundancySupported;

    @JsonProperty("multiSubnetSupported")
    private Boolean multiSubnetSupported;

    @JsonProperty("accuracySlaUnit")
    private String accuracySlaUnit;

    @JsonProperty("accuracySla")
    private Integer accuracySla;

    @JsonProperty("accuracySlaMin")
    private Integer accuracySlaMin;

    @JsonProperty("accuracySlaMax")
    private Integer accuracySlaMax;

    @JsonProperty("changelog")
    private ChangeLog changelog;
}
