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

import api.equinix.javasdk.fabric.enums.RouteTableEntryProtocolType;
import api.equinix.javasdk.fabric.enums.RouteTableEntryType;
import api.equinix.javasdk.fabric.model.RouteTableEntry;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.RouteTableEntryConnection;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class RouteTableEntryJson implements RouteTableEntry {

    @Getter static TypeReference<List<RouteTableEntryJson>> listTypeRef = new TypeReference<>() {};


    @JsonProperty("type")
    private RouteTableEntryType type;

    @JsonProperty("protocolType")
    private RouteTableEntryProtocolType protocolType;

    @JsonProperty("state")
    private String state;

    @JsonProperty("prefix")
    private String prefix;

    @JsonProperty("nextHop")
    private String nextHop;

    @JsonProperty("MED")
    private Integer MED;

    @JsonProperty("localPreference")
    private Integer localPreference;

    @JsonProperty("asPath")
    private List<String> asPath;

    @JsonProperty("connection")
    private RouteTableEntryConnection connection;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;
}
