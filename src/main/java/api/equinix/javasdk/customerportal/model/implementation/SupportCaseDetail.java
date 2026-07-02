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
import java.util.Map;

/**
 * A line-item detail of a support case
 * ({@code SingleCaseResponseV2.otherDetails.details}). Captures the substantive product,
 * location and status fields of an order/case line.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupportCaseDetail {

    @JsonProperty("lineId")
    private String lineId;

    @JsonProperty("parentLineId")
    private String parentLineId;

    @JsonProperty("productType")
    private String productType;

    @JsonProperty("productOfferingCode")
    private String productOfferingCode;

    @JsonProperty("productCode")
    private String productCode;

    @JsonProperty("productName")
    private String productName;

    @JsonProperty("status")
    private String status;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("cage")
    private String cage;

    @JsonProperty("requestType")
    private String requestType;

    @JsonProperty("activityId")
    private String activityId;

    @JsonProperty("updatedDateTime")
    private String updatedDateTime;

    @JsonProperty("estimatedCompletion")
    private SupportCaseEstimatedCompletion estimatedCompletion;

    /**
     * Additional {@code {key, value}} information on the case line (e.g. {@code IS_RECURRING}).
     * The wire value is a boolean and is coerced to its {@code String} form.
     */
    @JsonProperty("additionalInfo")
    private List<AdditionalInfo> additionalInfo;

    @JsonProperty("purchaseOrder")
    private Map<String, Object> purchaseOrder;

    @JsonProperty("modifiable")
    private Boolean modifiable;

    @JsonProperty("cancellable")
    private Boolean cancellable;

    @JsonProperty("reOpen")
    private String reOpen;

    @JsonProperty("respond")
    private Boolean respond;
}
