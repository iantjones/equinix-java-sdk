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

package api.equinix.javasdk.ibxsmartview.model.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

/**
 * Deserialization holder for the {@code /dcim/v3/powerEvents/search} response
 * ({@code PowerEventsPaginatedResponse}). Unlike the standard Equinix page shape, this endpoint
 * returns the pagination fields ({@code limit}, {@code offset}, {@code totalCount}) at the top
 * level alongside the {@code items} array, so it cannot be deserialized directly into the core
 * {@code Page} (which expects a nested {@code pagination} object).
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PowerEventsPaginatedResponseJson {

    @JsonProperty("items")
    private ArrayList<PowerEventJson> items;

    @JsonProperty("limit")
    private Integer limit;

    @JsonProperty("offset")
    private Integer offset;

    @JsonProperty("totalCount")
    private Integer totalCount;
}
