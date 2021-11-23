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

package api.equinix.javasdk.customerportal.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.customerportal.enums.TransactionType;
import api.equinix.javasdk.customerportal.model.InvoiceSummary;
import api.equinix.javasdk.customerportal.model.implementation.BillingContact;
import api.equinix.javasdk.customerportal.model.implementation.CustomerDetail;
import api.equinix.javasdk.customerportal.model.implementation.PaymentInstructions;
import api.equinix.javasdk.customerportal.model.implementation.TaxInfo;
import api.equinix.javasdk.customerportal.model.json.InvoiceSummaryJson;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

public class InvoiceSummaryWrapper extends ResourceImpl<InvoiceSummary> implements InvoiceSummary {

    private InvoiceSummaryJson json;
    @Getter
    private final Pageable<InvoiceSummary> serviceClient;

    public InvoiceSummaryWrapper(InvoiceSummaryJson invoiceSummaryJson, Pageable<InvoiceSummary> serviceClient) {
        this.json = invoiceSummaryJson;
        this.serviceClient = serviceClient;
    }

    public String getTransactionId() {
        return this.json.getTransactionId();
    }

    public TransactionType getTransactionType() {
        return this.json.getTransactionType();
    }

    public LocalDate getTransactionDate() {
        return this.json.getTransactionDate();
    }

    public CustomerDetail getCustomerDetails() {
        return this.json.getCustomerDetails();
    }

    public String getBillingPurchaseOrder() {
        return this.json.getBillingPurchaseOrder();
    }

    public String getBillingCycle() {
        return this.json.getBillingCycle();
    }

    public BillingContact getBillingContacts() {
        return this.json.getBillingContacts();
    }

    public LocalDate getPaymentDueDate() {
        return this.json.getPaymentDueDate();
    }

    public String getPaymentTerms() {
        return this.json.getPaymentTerms();
    }

    public Currency getCurrencyCode() {
        return this.json.getCurrencyCode();
    }

    public BigDecimal getTotalRecurringAmount() {
        return this.json.getTotalRecurringAmount();
    }

    public BigDecimal getTotalNonRecurringAmount() {
        return this.json.getTotalNonRecurringAmount();
    }

    public BigDecimal getTotalAmount() {
        return this.json.getTotalAmount();
    }

    public BigDecimal getTotalAdjustment() {
        return this.json.getTotalAdjustment();
    }

    public String getVatNumber() {
        return this.json.getVatNumber();
    }

    public PaymentInstructions getPaymentInstructions() {
        return this.json.getPaymentInstructions();
    }

    public List<TaxInfo> getTaxInfo() {
        return this.json.getTaxInfo();
    }
}
