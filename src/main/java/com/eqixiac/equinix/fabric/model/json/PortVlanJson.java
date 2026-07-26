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

import com.eqixiac.equinix.fabric.enums.LinkProtocolState;
import com.eqixiac.equinix.fabric.enums.LinkProtocolType;
import com.eqixiac.equinix.fabric.model.PortVlan;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;
import com.eqixiac.equinix.fabric.model.implementation.LinkProtocolConnection;
import com.eqixiac.equinix.fabric.model.implementation.LinkProtocolServiceToken;
import com.eqixiac.equinix.fabric.model.implementation.SubInterface;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PortVlanJson implements PortVlan {

    @Getter static TypeReference<List<PortVlanJson>> listTypeRef = new TypeReference<>() {};


    @JsonProperty("href")
    private String href;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("state")
    private LinkProtocolState state;

    @JsonProperty("type")
    private LinkProtocolType type;

    @JsonProperty("vlanTag")
    private Integer vlanTag;

    @JsonProperty("vni")
    private Integer vni;

    @JsonProperty("vlanTagMin")
    private Integer vlanTagMin;

    @JsonProperty("vlanTagMax")
    private Integer vlanTagMax;

    @JsonProperty("vlanSTag")
    private Integer vlanSTag;

    @JsonProperty("vlanCTag")
    private Integer vlanCTag;

    @JsonProperty("vlanCTagMin")
    private Integer vlanCTagMin;

    @JsonProperty("vlanCTagMax")
    private Integer vlanCTagMax;

    @JsonProperty("subInterface")
    private SubInterface subInterface;

    @JsonProperty("asset")
    private LinkProtocolConnection asset;

    @JsonProperty("serviceToken")
    private LinkProtocolServiceToken serviceToken;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;
}
