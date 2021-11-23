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
import api.equinix.javasdk.customerportal.enums.TransactionType;
import api.equinix.javasdk.customerportal.model.InvoiceSummary;
import api.equinix.javasdk.customerportal.model.implementation.BillingContact;
import api.equinix.javasdk.customerportal.model.implementation.CustomerDetail;
import api.equinix.javasdk.customerportal.model.implementation.PaymentInstructions;
import api.equinix.javasdk.customerportal.model.implementation.TaxInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

@Getter
public class InvoiceSummaryJson {

    @Getter static TypeReference<Page<InvoiceSummary, InvoiceSummaryJson>> pagedTypeRef = new TypeReference<>() {};

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("transactionType")
    private TransactionType transactionType;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonProperty("transactionDate")
    private LocalDate transactionDate;

    @JsonProperty("customerDetails")
    private CustomerDetail customerDetails;

    @JsonProperty("billingPurchaseOrder")
    private String billingPurchaseOrder;

    @JsonProperty("billingCycle")
    private String billingCycle;

    @JsonProperty("billingContacts")
    private BillingContact billingContacts;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonProperty("paymentDueDate")
    private LocalDate paymentDueDate;

    @JsonProperty("paymentTerms")
    private String paymentTerms;

    @JsonProperty("currencyCode")
    private Currency currencyCode;

    @JsonProperty("totalRecurringAmount")
    private BigDecimal totalRecurringAmount;

    @JsonProperty("totalNonRecurringAmount")
    private BigDecimal totalNonRecurringAmount;

    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;

    @JsonProperty("totalAdjustment")
    private BigDecimal totalAdjustment;

    @JsonProperty("vatNumber")
    private String vatNumber;

    @JsonProperty("paymentInstructions")
    private PaymentInstructions paymentInstructions;

    @JsonProperty("taxInfo")
    private List<TaxInfo> taxInfo;
}