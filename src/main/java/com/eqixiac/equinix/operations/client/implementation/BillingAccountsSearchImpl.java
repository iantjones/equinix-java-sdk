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

package com.eqixiac.equinix.operations.client.implementation;

import com.eqixiac.equinix.Operations;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.operations.client.BillingAccountsSearch;
import com.eqixiac.equinix.operations.client.internal.BillingAccountSearchClient;
import com.eqixiac.equinix.operations.model.BillingAccountV2;
import com.eqixiac.equinix.operations.model.json.BillingAccountV2Json;
import com.eqixiac.equinix.operations.model.json.creators.BillingAccountSearchRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BillingAccountsSearchImpl implements BillingAccountsSearch {

    private final BillingAccountSearchClient<BillingAccountV2> serviceClient;

    private final Operations serviceManager;

    public PaginatedList<BillingAccountV2> search(BillingAccountSearchRequest request) {
        Page<BillingAccountV2Json> responsePage = this.serviceClient.search(request);
        PaginatedList<BillingAccountV2> accountList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(accountList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public BillingAccountV2 getByAccountNumber(String accountNumber) {
        return this.serviceClient.getByAccountNumber(accountNumber);
    }

    public BillingAccountV2 getByAccountId(String accountId) {
        return this.serviceClient.getByAccountId(accountId);
    }
}
