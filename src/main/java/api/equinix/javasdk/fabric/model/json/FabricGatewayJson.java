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
import api.equinix.javasdk.fabric.enums.*;
import api.equinix.javasdk.fabric.model.FabricGateway;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.implementation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class FabricGatewayJson implements Serializable {

    @Getter static TypeReference<Page<FabricGateway, FabricGatewayJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<FabricGatewayJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<FabricGatewayJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private FabricGatewayType type;

    @JsonProperty("href")
    private String href;

    @JsonProperty("state")
    private FabricGatewayState state;

    @JsonProperty("location")
    private MinimalLocation location;

    @JsonProperty("package")
    private FabricGatewayPackage gatewayPackage;

    @JsonProperty("order")
    private Order order;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("account")
    private BasicAccount account;

    @JsonProperty("notifications")
    private List<Notification> notifications;

    @JsonProperty("bgpIpv4RoutesCount")
    private Integer bgpIpv4RoutesCount;

    @JsonProperty("bgpIpv6RoutesCount")
    private Integer bgpIpv6RoutesCount;

    @JsonProperty("connectionCount")
    private Integer connectionCount;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;
}
