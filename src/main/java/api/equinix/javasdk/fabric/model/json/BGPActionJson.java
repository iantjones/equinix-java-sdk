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

import api.equinix.javasdk.fabric.enums.BGPActionState;
import api.equinix.javasdk.fabric.enums.BGPActionType;
import api.equinix.javasdk.fabric.model.BGPAction;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class BGPActionJson implements BGPAction {

    @Getter static TypeReference<BGPActionJson> singleTypeRef = new TypeReference<>() {};

    @Getter static TypeReference<List<BGPActionJson>> listTypeRef = new TypeReference<>() {};


    @JsonProperty("href")
    private String href;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private BGPActionType type;

    @JsonProperty("description")
    private String description;

    @JsonProperty("state")
    private BGPActionState state;

    @JsonProperty("changelog")
    private ChangeLog changelog;
}
