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

package api.equinix.javasdk.customerportal.client.implementation;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.client.BillingAccounts;
import api.equinix.javasdk.customerportal.client.internal.BillingAccountClient;
import api.equinix.javasdk.customerportal.model.BillingAccount;
import api.equinix.javasdk.customerportal.model.json.BillingAccountJson;

public class BillingAccountsImpl implements BillingAccounts {

    private final CustomerPortal serviceManager;

    private final BillingAccountClient<BillingAccount> serviceClient;

    public BillingAccountsImpl(BillingAccountClient<BillingAccount> serviceClient, CustomerPortal serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<BillingAccount> summaries() {
        Page<BillingAccount, BillingAccountJson> responsePage = this.serviceClient.summaries();
        PaginatedList<BillingAccount> accountList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(accountList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public BillingAccount getByAccountNumber(String accountNumber) {
        return this.serviceClient.getByAccountNumber(accountNumber);
    }

    public byte[] downloadInvoiceDocument(String accountNumber, String invoiceId, String documentId) {
        return this.serviceClient.downloadInvoiceDocument(accountNumber, invoiceId, documentId);
    }
}
