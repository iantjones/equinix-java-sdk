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

package com.eqixiac.equinix.internetaccess.model.json;

import com.eqixiac.equinix.internetaccess.enums.PriceCategory;
import com.eqixiac.equinix.internetaccess.enums.ProductType;
import com.eqixiac.equinix.internetaccess.model.Price;
import com.eqixiac.equinix.internetaccess.model.implementation.CustomerAccount;
import com.eqixiac.equinix.internetaccess.model.implementation.IpBlockProductPrice;
import com.eqixiac.equinix.internetaccess.model.implementation.PriceSummary;
import com.eqixiac.equinix.internetaccess.model.implementation.ProductPriceCharge;
import com.eqixiac.equinix.internetaccess.model.implementation.ServicePrice;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Read-only JSON model for a {@link Price} returned by the Equinix Internet Access (EIA) v1
 * price search {@code POST /internetAccess/v1/prices/search}. Implements {@link Price}
 * directly, so no wrapper is required.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceJson implements Price {

    @JsonProperty("href")
    private String href;

    @JsonProperty("type")
    private ProductType type;

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("category")
    private PriceCategory category;

    @JsonProperty("charges")
    private List<ProductPriceCharge> charges;

    @JsonProperty("summary")
    private PriceSummary summary;

    @JsonProperty("account")
    private CustomerAccount account;

    @JsonProperty("service")
    private ServicePrice service;

    @JsonProperty("ipBlock")
    private IpBlockProductPrice ipBlock;
}
