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

package com.eqixiac.equinix.ibxsmartview.model.json;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

/**
 * Deserialization holder for the {@code /dcim/v3/powerEvents/search} response.
 *
 * <p>Two shapes are read. The spec ({@code PowerEventsPaginatedResponse}) puts the paging fields
 * ({@code limit}, {@code offset}, {@code totalCount}) at the top level next to {@code items}. The
 * live service (observed 2026-11-11) returns the standard Equinix page instead:
 * {@code {data, pagination{offset, limit, total}}}. {@code data} is read as an alias of
 * {@code items}, and the accessors prefer the nested {@code pagination} values when present.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PowerEventsPaginatedResponseJson {

    @JsonProperty("items")
    @JsonAlias("data")
    private ArrayList<PowerEventJson> items;

    @JsonProperty("limit")
    private Integer limit;

    @JsonProperty("offset")
    private Integer offset;

    @JsonProperty("totalCount")
    private Integer totalCount;

    @JsonProperty("pagination")
    private NestedPagination pagination;

    /**
     * Returns the events on this page.
     *
     * @return the events, or {@code null} when the response carried none
     */
    public ArrayList<PowerEventJson> getItems() {
        return items;
    }

    /**
     * Returns the page size.
     *
     * @return {@code pagination.limit} when present, else the top-level {@code limit}
     */
    public Integer getLimit() {
        return pagination != null && pagination.limit != null ? pagination.limit : limit;
    }

    /**
     * Returns the page offset.
     *
     * @return {@code pagination.offset} when present, else the top-level {@code offset}
     */
    public Integer getOffset() {
        return pagination != null && pagination.offset != null ? pagination.offset : offset;
    }

    /**
     * Returns the total number of matching events.
     *
     * @return {@code pagination.total} when present, else the top-level {@code totalCount}
     */
    public Integer getTotalCount() {
        return pagination != null && pagination.total != null ? pagination.total : totalCount;
    }

    /** The live service's nested {@code pagination} block. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class NestedPagination {
        @JsonProperty("offset")
        Integer offset;
        @JsonProperty("limit")
        Integer limit;
        @JsonProperty("total")
        Integer total;
    }
}
