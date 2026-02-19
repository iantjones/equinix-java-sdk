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
import api.equinix.javasdk.fabric.enums.NetworkScope;
import api.equinix.javasdk.fabric.enums.NetworkState;
import api.equinix.javasdk.fabric.enums.NetworkType;
import api.equinix.javasdk.fabric.model.Network;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.implementation.Change;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.MinimalLocation;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class NetworkJson implements Serializable {

    @Getter static TypeReference<Page<Network, NetworkJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<NetworkJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<NetworkJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("href")
    private String href;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private NetworkType type;

    @JsonProperty("state")
    private NetworkState state;

    @JsonProperty("scope")
    private NetworkScope scope;

    @JsonProperty("location")
    private MinimalLocation location;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;

    @JsonProperty("change")
    private Change change;

    @JsonProperty("connectionsCount")
    private Integer connectionsCount;
}
