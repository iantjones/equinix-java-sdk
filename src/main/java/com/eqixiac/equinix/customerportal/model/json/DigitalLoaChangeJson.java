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

package com.eqixiac.equinix.customerportal.model.json;

import com.eqixiac.equinix.customerportal.enums.LoaChangeStatus;
import com.eqixiac.equinix.customerportal.enums.LoaChangeType;
import com.eqixiac.equinix.customerportal.model.DigitalLoaChange;
import com.eqixiac.equinix.customerportal.model.implementation.LoaChangeResult;
import com.eqixiac.equinix.customerportal.model.implementation.LoaLink;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DigitalLoaChangeJson implements DigitalLoaChange {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private LoaChangeType changeType;

    @JsonProperty("status")
    private LoaChangeStatus status;

    @JsonProperty("createdDateTime")
    private String createdDateTime;

    @JsonProperty("updatedDateTime")
    private String updatedDateTime;

    @JsonProperty("data")
    private String data;

    @JsonProperty("description")
    private String description;

    @JsonProperty("href")
    private String href;

    @JsonProperty("links")
    private List<LoaLink> links;

    @JsonProperty("result")
    private LoaChangeResult result;
}
