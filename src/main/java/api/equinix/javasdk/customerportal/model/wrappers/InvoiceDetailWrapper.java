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
import api.equinix.javasdk.customerportal.enums.ActivityType;
import api.equinix.javasdk.customerportal.enums.Frequency;
import api.equinix.javasdk.customerportal.model.InvoiceDetail;
import api.equinix.javasdk.customerportal.model.implementation.AdditionalInfo;
import api.equinix.javasdk.customerportal.model.implementation.BillingContact;
import api.equinix.javasdk.customerportal.model.implementation.CustomerDetail;
import api.equinix.javasdk.customerportal.model.json.InvoiceDetailJson;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;

public class InvoiceDetailWrapper extends ResourceImpl<InvoiceDetail> implements InvoiceDetail {

    private InvoiceDetailJson json;
    @Getter
    private final Pageable<InvoiceDetail> serviceClient;

    public InvoiceDetailWrapper(InvoiceDetailJson invoiceDetailJson, Pageable<InvoiceDetail> serviceClient) {
        this.json = invoiceDetailJson;
        this.serviceClient = serviceClient;
    }

    public String getTransactionId() {
        return  this.json.getTransactionId();
    }

    public String getBusinessLegalEntity() {
        return  this.json.getBusinessLegalEntity();
    }

    public String getTransactionType() {
        return  this.json.getTransactionType();
    }

    public LocalDate getTransactionDate() {
        return  this.json.getTransactionDate();
    }

    public String getOrderId() {
        return  this.json.getOrderId();
    }

    public String getLineNumber() {
        return  this.json.getLineNumber();
    }

    public String getSubLineNumber() {
        return  this.json.getSubLineNumber();
    }

    public CustomerDetail getCustomerDetails() {
        return  this.json.getCustomerDetails();
    }

    public String getPurchaseOrderNumber() {
        return  this.json.getPurchaseOrderNumber();
    }

    public String getBillingAgreementId() {
        return  this.json.getBillingAgreementId();
    }

    public String getCustomerReferenceId() {
        return  this.json.getCustomerReferenceId();
    }

    public List<String> getIbxs() {
        return  this.json.getIbxs();
    }

    public List<BillingContact> getContacts() {
        return  this.json.getContacts();
    }

    public LocalDateTime getOrderBookedDate() {
        return  this.json.getOrderBookedDate();
    }

    public ActivityType getActivityType() {
        return  this.json.getActivityType();
    }

    public String getProductCategory() {
        return  this.json.getProductCategory();
    }

    public String getProductDescription() {
        return  this.json.getProductDescription();
    }

    public String getProductCode() {
        return  this.json.getProductCode();
    }

    public String getDetailedDescription() {
        return  this.json.getDetailedDescription();
    }

    public String getIbxDescription() {
        return  this.json.getIbxDescription();
    }

    public Double getQuantity() {
        return  this.json.getQuantity();
    }

    public String getUnitOfMeasure() {
        return  this.json.getUnitOfMeasure();
    }

    public BigDecimal getUnitPrice() {
        return  this.json.getUnitPrice();
    }

    public Frequency getFrequency() {
        return  this.json.getFrequency();
    }

    public Currency getCurrencyCode() {
        return  this.json.getCurrencyCode();
    }

    public Currency getLocalCurrencyCode() {
        return  this.json.getLocalCurrencyCode();
    }

    public BigDecimal getNonRecurringAmount() {
        return  this.json.getNonRecurringAmount();
    }

    public BigDecimal getRecurringAmount() {
        return  this.json.getRecurringAmount();
    }

    public BigDecimal getAdjustment() {
        return  this.json.getAdjustment();
    }

    public BigDecimal getTaxAmount() {
        return  this.json.getTaxAmount();
    }

    public BigDecimal getTotalAmount() {
        return  this.json.getTotalAmount();
    }

    public String getLegacyOrderId() {
        return  this.json.getLegacyOrderId();
    }

    public List<AdditionalInfo> getAdditionalInfo() {
        return  this.json.getAdditionalInfo();
    }
}
