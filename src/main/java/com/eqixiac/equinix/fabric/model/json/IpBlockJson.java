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

import com.eqixiac.equinix.fabric.enums.IpBlockOwnership;
import com.eqixiac.equinix.fabric.enums.IpBlockProductType;
import com.eqixiac.equinix.fabric.enums.IpBlockState;
import com.eqixiac.equinix.fabric.model.IpBlock;
import com.eqixiac.equinix.fabric.model.Project;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;
import com.eqixiac.equinix.fabric.model.implementation.Error;
import com.eqixiac.equinix.fabric.model.implementation.IpBlockAccount;
import com.eqixiac.equinix.fabric.model.implementation.IpBlockAsset;
import com.eqixiac.equinix.fabric.model.implementation.IpBlockChange;
import com.eqixiac.equinix.fabric.model.implementation.IpBlockLocation;
import com.eqixiac.equinix.fabric.model.implementation.IpBlockOrder;
import com.eqixiac.equinix.fabric.model.implementation.IpBlockRegulations;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class IpBlockJson {

    @Getter static TypeReference<List<IpBlockJson>> listTypeRef = new TypeReference<>() {};


    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("href")
    private String href;

    @JsonProperty("type")
    private IpBlockProductType type;

    @JsonProperty("state")
    private IpBlockState state;

    @JsonProperty("ownership")
    private IpBlockOwnership ownership;

    @JsonProperty("prefixLength")
    private Integer prefixLength;

    @JsonProperty("prefix")
    private String prefix;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("location")
    private IpBlockLocation location;

    @JsonProperty("order")
    private IpBlockOrder order;

    @JsonProperty("account")
    private IpBlockAccount account;

    @JsonProperty("regulations")
    private IpBlockRegulations regulations;

    @JsonProperty("assets")
    private List<IpBlockAsset> assets;

    @JsonProperty("change")
    private IpBlockChange change;

    @JsonProperty("error")
    private Error error;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;
}
