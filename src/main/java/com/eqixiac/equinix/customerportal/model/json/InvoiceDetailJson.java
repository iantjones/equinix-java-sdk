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

import com.eqixiac.equinix.customerportal.enums.TransactionType;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.model.deserializers.LocalDateDeserializer;
import com.eqixiac.equinix.customerportal.enums.ActivityType;
import com.eqixiac.equinix.customerportal.enums.Channel;
import com.eqixiac.equinix.customerportal.enums.Frequency;
import com.eqixiac.equinix.customerportal.enums.Region;
import com.eqixiac.equinix.customerportal.enums.SubChannel;
import com.eqixiac.equinix.customerportal.model.InvoiceDetail;
import com.eqixiac.equinix.customerportal.model.InvoiceSummary;
import com.eqixiac.equinix.core.model.KeyValuePair;
import com.eqixiac.equinix.customerportal.model.implementation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceDetailJson {


    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("businessLegalEntity")
    private String businessLegalEntity;

    @JsonProperty("region")
    private Region region;

    @JsonProperty("countryCode")
    private String countryCode;

    @JsonProperty("transactionType")
    private TransactionType transactionType;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonProperty("transactionDate")
    private LocalDate transactionDate;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("channel")
    private Channel channel;

    @JsonProperty("subChannel")
    private SubChannel subChannel;

    @JsonProperty("lineNumber")
    private String lineNumber;

    @JsonProperty("subLineNumber")
    private String subLineNumber;

    @JsonProperty("customerDetails")
    private CustomerDetail customerDetails;

    @JsonProperty("purchaseOrderNumber")
    private String purchaseOrderNumber;

    @JsonProperty("billingAgreementId")
    private String billingAgreementId;

    @JsonProperty("customerReferenceId")
    private String customerReferenceId;

    @JsonProperty("priorAdjustmentReference")
    private String priorAdjustmentReference;

    @JsonProperty("ibxs")
    private List<String> ibxs;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonProperty("recurringStartDate")
    private LocalDate recurringStartDate;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonProperty("recurringEndDate")
    private LocalDate recurringEndDate;

    @JsonProperty("contacts")
    private List<BillingContact> contacts;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonProperty("orderBookedDate")
    private LocalDate orderBookedDate;

    @JsonProperty("activityType")
    private ActivityType activityType;

    @JsonProperty("productCategory")
    private String productCategory;

    @JsonProperty("productDescription")
    private String productDescription;

    @JsonProperty("productCode")
    private String productCode;

    @JsonProperty("productName")
    private String productName;

    @JsonProperty("detailedDescription")
    private String detailedDescription;

    @JsonProperty("ibxDescription")
    private String ibxDescription;

    @JsonProperty("quantity")
    private Double quantity;

    @JsonProperty("unitOfMeasure")
    private String unitOfMeasure;

    @JsonProperty("unitPrice")
    private BigDecimal unitPrice;
    
    @JsonProperty("frequency")
    private Frequency frequency;
    
    @JsonProperty("currencyCode")
    private Currency currencyCode;
    
    @JsonProperty("localCurrencyCode")
    private Currency localCurrencyCode;

    @JsonProperty("exchangeRate")
    private BigDecimal exchangeRate;

    @JsonProperty("nonRecurringAmount")
    private BigDecimal nonRecurringAmount;
    
    @JsonProperty("recurringAmount")
    private BigDecimal recurringAmount;
    
    @JsonProperty("adjustment")
    private BigDecimal adjustment;

    @JsonProperty("taxAmount")
    private BigDecimal taxAmount;

    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;

    @JsonProperty("legacyOrderId")
    private String legacyOrderId;

    @JsonProperty("additionalInfo")
    private List<KeyValuePair> additionalInfo;

    @JsonProperty("termsOfUse")
    private List<TermsOfUse> termsOfUse;
}