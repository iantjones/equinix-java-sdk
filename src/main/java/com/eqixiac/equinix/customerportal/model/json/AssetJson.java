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

import com.eqixiac.equinix.customerportal.enums.AssetStatus;
import com.eqixiac.equinix.customerportal.model.Asset;
import com.eqixiac.equinix.customerportal.model.implementation.AssetAdditionalDetails;
import com.eqixiac.equinix.customerportal.model.implementation.AssetProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * JSON model for an installed-base asset ({@code asset}/{@code assetsSummary} schema, Assets v1 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetJson implements Asset {

    @JsonProperty("assetNumber")
    private String assetNumber;

    @JsonProperty("serialNumber")
    private String serialNumber;

    @JsonProperty("orderNumber")
    private String orderNumber;

    @JsonProperty("productName")
    private String productName;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("cage")
    private String cage;

    @JsonProperty("productDescription")
    private String productDescription;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("installationDate")
    private String installationDate;

    @JsonProperty("customerReferenceNumber")
    private String customerReferenceNumber;

    @JsonProperty("status")
    private AssetStatus status;

    @JsonProperty("productDetails")
    private List<AssetProperty> productDetails;

    @JsonProperty("additionalDetails")
    private AssetAdditionalDetails additionalDetails;
}
