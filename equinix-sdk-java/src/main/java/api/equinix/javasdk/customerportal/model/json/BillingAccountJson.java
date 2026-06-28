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

import api.equinix.javasdk.customerportal.enums.BillingFrequency;
import api.equinix.javasdk.customerportal.enums.InvoiceFormat;
import api.equinix.javasdk.customerportal.model.BillingAccount;
import api.equinix.javasdk.customerportal.model.implementation.BillingContactInfo;
import api.equinix.javasdk.customerportal.model.implementation.BillingInvoice;
import api.equinix.javasdk.customerportal.model.implementation.BillingPayment;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillingAccountJson implements BillingAccount {

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("parentAccountNumber")
    private String parentAccountNumber;

    @JsonProperty("billingFrequency")
    private BillingFrequency billingFrequency;

    @JsonProperty("currencyCode")
    private String currencyCode;

    @JsonProperty("invoiceLanguage")
    private String invoiceLanguage;

    @JsonProperty("invoiceFormat")
    private InvoiceFormat invoiceFormat;

    @JsonProperty("accountIbxs")
    private List<String> accountIbxs;

    @JsonProperty("billingContact")
    private BillingContactInfo billingContact;

    @JsonProperty("invoices")
    private List<BillingInvoice> invoices;

    @JsonProperty("payments")
    private List<BillingPayment> payments;
}
