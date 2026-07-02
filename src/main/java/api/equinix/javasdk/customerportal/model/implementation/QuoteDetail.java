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

package api.equinix.javasdk.customerportal.model.implementation;

import api.equinix.javasdk.customerportal.enums.OrderLineRequestType;
import api.equinix.javasdk.customerportal.enums.UnitOfMeasure;
import api.equinix.javasdk.customerportal.enums.OrderProductType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * A single quote line item ({@code quote_details}). Carries the line hierarchy
 * ({@code lineId}/{@code parentLineId}/{@code rootLineId}), product identification
 * ({@code productType}, {@code productCode}, {@code productName}, {@code productDescription}),
 * location ({@code ibx}, {@code cage}), quantity ({@code quantity}, {@code unitOfMeasure}),
 * unit and total {@link QuotePricing pricing}, the {@code requestType} and any
 * product-specific {@link AdditionalInfo additionalInfo}.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuoteDetail {

    @JsonProperty("lineId")
    private String lineId;

    @JsonProperty("parentLineId")
    private String parentLineId;

    @JsonProperty("rootLineId")
    private String rootLineId;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("cage")
    private String cage;

    @JsonProperty("productDescription")
    private String productDescription;

    @JsonProperty("productType")
    private OrderProductType productType;

    @JsonProperty("productCode")
    private String productCode;

    @JsonProperty("productName")
    private String productName;

    @JsonProperty("assetizable")
    private Boolean assetizable;

    @JsonProperty("lineItemModified")
    private Boolean lineItemModified;

    @JsonProperty("quantity")
    private BigDecimal quantity;

    @JsonProperty("unitOfMeasure")
    private UnitOfMeasure unitOfMeasure;

    @JsonProperty("unitPricing")
    private List<QuotePricing> unitPricing;

    @JsonProperty("totalPricing")
    private List<QuotePricing> totalPricing;

    @JsonProperty("requestType")
    private OrderLineRequestType requestType;

    @JsonProperty("additionalInfo")
    private List<AdditionalInfo> additionalInfo;
}
