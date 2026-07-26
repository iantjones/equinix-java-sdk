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

package com.eqixiac.equinix.ibxsmartview.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * A search filter composed of {@code and}/{@code or} arrays of {@link SearchCondition}
 * ({@code SearchFilter} in the spec).
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchFilter {

    @JsonProperty("and")
    private final List<SearchCondition> and;

    @JsonProperty("or")
    private final List<SearchCondition> or;

    public SearchFilter(List<SearchCondition> and, List<SearchCondition> or) {
        this.and = and;
        this.or = or;
    }
}
