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

package api.equinix.javasdk.customerportal.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.deserializers.LocalDateDeserializer;
import api.equinix.javasdk.customerportal.enums.ActivityType;
import api.equinix.javasdk.customerportal.enums.Frequency;
import api.equinix.javasdk.customerportal.model.InvoiceDetail;
import api.equinix.javasdk.customerportal.model.InvoiceSummary;
import api.equinix.javasdk.core.model.KeyValuePair;
import api.equinix.javasdk.customerportal.model.implementation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;

@Getter
public class InvoiceDetailJson {

    @Getter static TypeReference<Page<InvoiceDetail, InvoiceDetailJson>> pagedTypeRef = new TypeReference<>() {};

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("businessLegalEntity")
    private String businessLegalEntity;

    @JsonProperty("transactionType")
    private String transactionType;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonProperty("transactionDate")
    private LocalDate transactionDate;

    @JsonProperty("orderId")
    private String orderId;

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

    @JsonProperty("ibxs")
    private List<String> ibxs;

    @JsonProperty("contacts")
    private List<BillingContact> contacts;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonProperty("orderBookedDate")
    private LocalDateTime orderBookedDate;

    @JsonProperty("activityType")
    private ActivityType activityType;

    @JsonProperty("productCategory")
    private String productCategory;

    @JsonProperty("productDescription")
    private String productDescription;

    @JsonProperty("productCode")
    private String productCode;

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
}