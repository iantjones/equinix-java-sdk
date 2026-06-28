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
import api.equinix.javasdk.customerportal.client.BillingAccountsSearch;
import api.equinix.javasdk.customerportal.client.internal.BillingAccountSearchClient;
import api.equinix.javasdk.customerportal.model.BillingAccountV2;
import api.equinix.javasdk.customerportal.model.json.BillingAccountV2Json;
import api.equinix.javasdk.customerportal.model.json.creators.BillingAccountSearchRequest;

public class BillingAccountsSearchImpl implements BillingAccountsSearch {

    private final CustomerPortal serviceManager;

    private final BillingAccountSearchClient<BillingAccountV2> serviceClient;

    public BillingAccountsSearchImpl(BillingAccountSearchClient<BillingAccountV2> serviceClient, CustomerPortal serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<BillingAccountV2> search(BillingAccountSearchRequest request) {
        Page<BillingAccountV2, BillingAccountV2Json> responsePage = this.serviceClient.search(request);
        PaginatedList<BillingAccountV2> accountList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(accountList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public BillingAccountV2 getByAccountNumber(String accountNumber) {
        return this.serviceClient.getByAccountNumber(accountNumber);
    }

    public BillingAccountV2 getByAccountId(String accountId) {
        return this.serviceClient.getByAccountId(accountId);
    }
}
