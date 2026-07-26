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

package com.eqixiac.equinix.customerportal.model;

import com.eqixiac.equinix.customerportal.enums.TransactionType;
import com.eqixiac.equinix.customerportal.model.implementation.BillingContact;
import com.eqixiac.equinix.customerportal.model.implementation.CustomerDetail;
import com.eqixiac.equinix.customerportal.model.implementation.PaymentInstructions;
import com.eqixiac.equinix.customerportal.model.implementation.PriorAdjustmentInfo;
import com.eqixiac.equinix.customerportal.model.implementation.TaxInfo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

public interface InvoiceSummary {
    
    String getTransactionId();

    String getBusinessLegalEntity();

    TransactionType getTransactionType();

    LocalDate getTransactionDate();

    CustomerDetail getCustomerDetails();

    String getBillingPurchaseOrder();

    String getBillingCycle();

    BillingContact getBillingContacts();

    LocalDate getPaymentDueDate();

    String getPaymentTerms();

    Currency getCurrencyCode();

    BigDecimal getTotalRecurringAmount();

    BigDecimal getTotalNonRecurringAmount();

    BigDecimal getTotalAmount();

    BigDecimal getTotalAdjustment();

    String getVatNumber();

    PaymentInstructions getPaymentInstructions();

    List<TaxInfo> getTaxInfo();

    List<PriorAdjustmentInfo> getPriorAdjustmentInfo();

    BigDecimal getTotalPriorAdjustmentAmount();
}