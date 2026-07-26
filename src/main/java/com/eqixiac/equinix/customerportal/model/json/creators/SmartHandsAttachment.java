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

package com.eqixiac.equinix.customerportal.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Reference to an attachment uploaded via the Attachments API, to be attached to a
 * smart hands order. Both id and name are required.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SmartHandsAttachment {

    @JsonProperty("id")
    private final String id;

    @JsonProperty("name")
    private final String name;

    public SmartHandsAttachment(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
