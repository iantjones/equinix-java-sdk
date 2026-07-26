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

import com.eqixiac.equinix.customerportal.enums.AssetProductType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Filter for an asset search ({@code filter} object of {@code AssetsSearchRequest}). All fields are
 * optional: {@code ibxs}, {@code cages}, {@code productTypes} and an installed-date {@code dateRange}.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetSearchFilter {

    @JsonProperty("ibxs")
    private List<String> ibxs;

    @JsonProperty("cages")
    private List<String> cages;

    @JsonProperty("productTypes")
    private List<AssetProductType> productTypes;

    @JsonProperty("dateRange")
    private AssetSearchDateRange dateRange;

    /**
     * Sets the IBX codes to filter by.
     *
     * @param ibxs the IBX codes
     * @return this filter
     */
    public AssetSearchFilter ibxs(List<String> ibxs) {
        this.ibxs = ibxs;
        return this;
    }

    /**
     * Sets the cages to filter by.
     *
     * @param cages the cages
     * @return this filter
     */
    public AssetSearchFilter cages(List<String> cages) {
        this.cages = cages;
        return this;
    }

    /**
     * Sets the product types to filter by.
     *
     * @param productTypes the product types
     * @return this filter
     */
    public AssetSearchFilter productTypes(List<AssetProductType> productTypes) {
        this.productTypes = productTypes;
        return this;
    }

    /**
     * Sets the installed-date range to filter by.
     *
     * @param dateRange the installed-date range
     * @return this filter
     */
    public AssetSearchFilter dateRange(AssetSearchDateRange dateRange) {
        this.dateRange = dateRange;
        return this;
    }
}
