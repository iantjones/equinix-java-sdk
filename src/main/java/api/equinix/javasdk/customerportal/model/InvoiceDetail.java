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

package api.equinix.javasdk.customerportal.model;

import api.equinix.javasdk.customerportal.enums.TransactionType;
import api.equinix.javasdk.customerportal.enums.ActivityType;
import api.equinix.javasdk.customerportal.enums.Channel;
import api.equinix.javasdk.customerportal.enums.Frequency;
import api.equinix.javasdk.customerportal.enums.Region;
import api.equinix.javasdk.customerportal.enums.SubChannel;
import api.equinix.javasdk.core.model.KeyValuePair;
import api.equinix.javasdk.customerportal.model.implementation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

public interface InvoiceDetail {


    String getTransactionId();

    String getBusinessLegalEntity();

    Region getRegion();

    String getCountryCode();

    TransactionType getTransactionType();

    LocalDate getTransactionDate();

    String getOrderId();

    Channel getChannel();

    SubChannel getSubChannel();

    String getLineNumber();

    String getSubLineNumber();

    CustomerDetail getCustomerDetails();

    String getPurchaseOrderNumber();

    String getBillingAgreementId();

    String getCustomerReferenceId();

    String getPriorAdjustmentReference();

    List<String> getIbxs();

    LocalDate getRecurringStartDate();

    LocalDate getRecurringEndDate();

    List<BillingContact> getContacts();

    LocalDate getOrderBookedDate();

    ActivityType getActivityType();

    String getProductCategory();

    String getProductDescription();

    String getProductCode();

    String getProductName();

    String getDetailedDescription();

    String getIbxDescription();

    Double getQuantity();

    String getUnitOfMeasure();

    BigDecimal getUnitPrice();

    Frequency getFrequency();

    Currency getCurrencyCode();

    Currency getLocalCurrencyCode();

    BigDecimal getExchangeRate();

    BigDecimal getNonRecurringAmount();

    BigDecimal getRecurringAmount();

    BigDecimal getAdjustment();

    BigDecimal getTaxAmount();

    BigDecimal getTotalAmount();

    String getLegacyOrderId();

    List<KeyValuePair> getAdditionalInfo();

    List<TermsOfUse> getTermsOfUse();
}