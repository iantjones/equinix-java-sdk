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

import com.eqixiac.equinix.customerportal.enums.BillingFrequency;
import com.eqixiac.equinix.customerportal.enums.InvoiceFormat;
import com.eqixiac.equinix.customerportal.model.implementation.BillingContactInfo;
import com.eqixiac.equinix.customerportal.model.implementation.BillingInvoice;
import com.eqixiac.equinix.customerportal.model.implementation.BillingPayment;

import java.util.List;

/**
 * A billing account, as returned by the Billing v1 finance accounts API. The list endpoint returns
 * account summaries, while a single-account fetch additionally populates the billing preferences,
 * contact, available invoices and payments.
 */
public interface BillingAccount {

    String getAccountNumber();

    String getAccountName();

    String getParentAccountNumber();

    BillingFrequency getBillingFrequency();

    String getCurrencyCode();

    String getInvoiceLanguage();

    InvoiceFormat getInvoiceFormat();

    List<String> getAccountIbxs();

    BillingContactInfo getBillingContact();

    List<BillingInvoice> getInvoices();

    List<BillingPayment> getPayments();
}
