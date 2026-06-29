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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Additional details of a support case ({@code SingleCaseResponseV2.otherDetails}), carrying the
 * case category, its line-item details and status history, and the actions currently permitted on
 * the case ({@code modifiable}, {@code reOpen}, {@code cancellable}, {@code respond}).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupportCaseOtherDetails {

    @JsonProperty("category")
    private String category;

    @JsonProperty("subCategory")
    private String subCategory;

    @JsonProperty("description")
    private String description;

    @JsonProperty("statusHistory")
    private List<SupportCaseStatusHistory> statusHistory;

    @JsonProperty("purchaseOrders")
    private List<String> purchaseOrders;

    @JsonProperty("products")
    private List<String> products;

    @JsonProperty("details")
    private List<SupportCaseDetail> details;

    @JsonProperty("modifiable")
    private Boolean modifiable;

    @JsonProperty("reOpen")
    private Boolean reOpen;

    @JsonProperty("cancellable")
    private Boolean cancellable;

    @JsonProperty("respond")
    private Boolean respond;
}
