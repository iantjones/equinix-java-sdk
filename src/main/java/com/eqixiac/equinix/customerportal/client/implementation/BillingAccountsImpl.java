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

package com.eqixiac.equinix.customerportal.client.implementation;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.customerportal.client.BillingAccounts;
import com.eqixiac.equinix.customerportal.client.internal.BillingAccountClient;
import com.eqixiac.equinix.customerportal.model.BillingAccount;
import com.eqixiac.equinix.customerportal.model.json.BillingAccountJson;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BillingAccountsImpl implements BillingAccounts {

    private final BillingAccountClient<BillingAccount> serviceClient;

    private final CustomerPortal serviceManager;

    public PaginatedList<BillingAccount> summaries() {
        Page<BillingAccountJson> responsePage = this.serviceClient.summaries();
        PaginatedList<BillingAccount> accountList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(accountList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PaginatedList<BillingAccount> summaries(String sorts) {
        Page<BillingAccountJson> responsePage = this.serviceClient.summaries(sorts);
        PaginatedList<BillingAccount> accountList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(accountList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public BillingAccount getByAccountNumber(String accountNumber) {
        return this.serviceClient.getByAccountNumber(accountNumber);
    }

    public BillingAccount getByAccountNumber(String accountNumber, String months) {
        return this.serviceClient.getByAccountNumber(accountNumber, months);
    }

    public byte[] downloadInvoiceDocument(String accountNumber, String invoiceId, String documentId) {
        return this.serviceClient.downloadInvoiceDocument(accountNumber, invoiceId, documentId);
    }
}
