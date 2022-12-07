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
import api.equinix.javasdk.core.model.Serializable;
import api.equinix.javasdk.fabric.enums.GatewayPackageCode;
import api.equinix.javasdk.fabric.enums.GatewayPackageType;
import api.equinix.javasdk.fabric.enums.NatType;
import api.equinix.javasdk.fabric.model.GatewayPackage;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class GatewayPackageJson implements Serializable {

    @Getter static TypeReference<Page<GatewayPackage, GatewayPackageJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<GatewayPackageJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<GatewayPackageJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("code")
    private GatewayPackageCode code;

    @JsonProperty("href")
    private String href;

    @JsonProperty("type")
    private GatewayPackageType type;

    @JsonProperty("description")
    private String description;

    @JsonProperty("totalIPv4RoutesMax")
    private Integer totalIPv4RoutesMax;

    @JsonProperty("totalIPv6RoutesMax")
    private Integer totalIPv6RoutesMax;

    @JsonProperty("staticIPv4RoutesMax")
    private Integer staticIPv4RoutesMax;

    @JsonProperty("staticIPv6RoutesMax")
    private Integer staticIPv6RoutesMax;

    @JsonProperty("naclsMax")
    private Integer naclsMax;

    @JsonProperty("naclRulesMax")
    private Integer naclRulesMax;

    @JsonProperty("haSupported")
    private Boolean haSupported;

    @JsonProperty("routeFilterSupported")
    private Boolean routeFilterSupported;

    @JsonProperty("natType")
    private NatType natType;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;
}
