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

package com.eqixiac.equinix.operations.model.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Envelope for the trouble ticket types reference endpoint, wrapping the list of supported types.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TroubleTicketTypesResponseJson {

    /** The live service spells the key {@code troubleticketTypes} (observed 2026-11-12); both are read. */
    @JsonProperty("troubleTicketTypes")
    @com.fasterxml.jackson.annotation.JsonAlias("troubleticketTypes")
    private List<TroubleTicketTypeJson> troubleTicketTypes;
}
