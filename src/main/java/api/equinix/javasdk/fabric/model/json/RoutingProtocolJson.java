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

import api.equinix.javasdk.fabric.enums.RoutingProtocolState;
import api.equinix.javasdk.fabric.enums.RoutingProtocolType;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.RoutingProtocol;
import api.equinix.javasdk.fabric.model.implementation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoutingProtocolJson {

    @Getter static TypeReference<List<RoutingProtocolJson>> listTypeRef = new TypeReference<>() {};


    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("href")
    private String href;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private RoutingProtocolType type;

    @JsonProperty("state")
    private RoutingProtocolState state;

    @JsonProperty("bgpIpv4")
    private BGPConnectionIpv4 bgpIpv4;

    @JsonProperty("bgpIpv6")
    private BGPConnectionIpv6 bgpIpv6;

    @JsonProperty("directIpv4")
    private DirectConnectionIpv4 directIpv4;

    @JsonProperty("directIpv6")
    private DirectConnectionIpv6 directIpv6;

    @JsonProperty("bfd")
    private BFDConfig bfd;

    @JsonProperty("customerAsn")
    private Long customerAsn;

    @JsonProperty("equinixAsn")
    private Long equinixAsn;

    @JsonProperty("bgpAuthKey")
    private String bgpAuthKey;

    @JsonProperty("asOverrideEnabled")
    private Boolean asOverrideEnabled;

    @JsonProperty("operation")
    private RoutingProtocolOperation operation;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("connection")
    private RoutingProtocolConnection connection;

    @JsonProperty("changelog")
    private ChangeLog changeLog;

    @JsonProperty("change")
    private Change change;
}
