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

package api.equinix.javasdk.ibxsmartview.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Typed request body for the system-alerts search POST endpoint ({@code SearchRequest} in the
 * spec). Carries the filter, request-side pagination and sort options.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchRequest {

    @JsonProperty("filter")
    private final SearchFilter filter;

    @JsonProperty("pagination")
    private final SearchPagination pagination;

    @JsonProperty("sort")
    private final List<SearchSort> sort;

    public SearchRequest(SearchFilter filter, SearchPagination pagination, List<SearchSort> sort) {
        this.filter = filter;
        this.pagination = pagination;
        this.sort = sort;
    }
}
