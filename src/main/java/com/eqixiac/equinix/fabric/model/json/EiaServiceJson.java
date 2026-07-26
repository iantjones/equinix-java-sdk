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

import com.eqixiac.equinix.fabric.enums.EiaServiceState;
import com.eqixiac.equinix.fabric.enums.EiaServiceType;
import com.eqixiac.equinix.fabric.enums.EiaServiceUseCase;
import com.eqixiac.equinix.fabric.model.Project;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;
import com.eqixiac.equinix.fabric.model.implementation.EiaServiceAccount;
import com.eqixiac.equinix.fabric.model.implementation.EiaServiceBilling;
import com.eqixiac.equinix.fabric.model.implementation.EiaServiceChange;
import com.eqixiac.equinix.fabric.model.implementation.EiaServiceLocation;
import com.eqixiac.equinix.fabric.model.implementation.EiaServiceOrder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EiaServiceJson {

    @Getter static TypeReference<List<EiaServiceJson>> listTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("href")
    private String href;

    @JsonProperty("type")
    private EiaServiceType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("bandwidth")
    private Integer bandwidth;

    @JsonProperty("bandwidthCommit")
    private Integer bandwidthCommit;

    @JsonProperty("state")
    private EiaServiceState state;

    @JsonProperty("useCase")
    private EiaServiceUseCase useCase;

    @JsonProperty("change")
    private EiaServiceChange change;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;

    @JsonProperty("locations")
    private List<EiaServiceLocation> locations;

    @JsonProperty("billing")
    private EiaServiceBilling billing;

    @JsonProperty("account")
    private EiaServiceAccount account;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("order")
    private EiaServiceOrder order;
}
