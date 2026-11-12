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

package com.eqixiac.equinix.operations.model.json.creators;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.eqixiac.equinix.operations.enums.AssetSearchSource;
import lombok.Getter;

import java.util.List;

/**
 * Request for searching assets ({@code POST /v1/assets/search}, {@code AssetsSearchRequest}).
 *
 * <p>The serialized body carries a typed {@code filter} ({@code ibxs}, {@code cages},
 * {@code productTypes} and a {@code dateRange}). The optional search query parameters
 * ({@code q}, {@code source}, {@code exactMatch}, {@code sorts}) are carried out-of-band (not
 * serialized in the body) and applied as query parameters by the client. {@code source} and
 * {@code sorts} are {@code collectionFormat: csv} in the spec and are sent comma-separated.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetSearchRequest {

    @JsonProperty("filter")
    private final AssetSearchFilter filter;

    @JsonIgnore
    private String q;

    @JsonIgnore
    private List<AssetSearchSource> source;

    @JsonIgnore
    private Boolean exactMatch;

    @JsonIgnore
    private List<String> sorts;

    /**
     * Creates an asset search request with a typed filter.
     *
     * @param filter the search filter, or {@code null} to search without filtering
     */
    public AssetSearchRequest(AssetSearchFilter filter) {
        this.filter = filter;
    }

    /**
     * Sets the free-text search query ({@code q} query parameter).
     *
     * @param q the search query
     * @return this request
     */
    public AssetSearchRequest q(String q) {
        this.q = q;
        return this;
    }

    /**
     * Restricts the fields the free-text query is matched against ({@code source} query parameter).
     *
     * @param source the fields to search, or {@code null} to search all fields
     * @return this request
     */
    public AssetSearchRequest source(List<AssetSearchSource> source) {
        this.source = source == null ? null : List.copyOf(source);
        return this;
    }

    /**
     * Sets whether the free-text query should match exactly ({@code exactMatch} query parameter).
     *
     * @param exactMatch {@code true} for an exact match
     * @return this request
     */
    public AssetSearchRequest exactMatch(Boolean exactMatch) {
        this.exactMatch = exactMatch;
        return this;
    }

    /**
     * Sets the result sort expressions ({@code sorts} query parameter).
     *
     * @param sorts the sort fields from the spec enum, for example {@code IBX} or
     *              {@code -INSTALLED_DATE} (a leading {@code -} sorts descending)
     * @return this request
     */
    public AssetSearchRequest sorts(List<String> sorts) {
        this.sorts = sorts;
        return this;
    }
}
