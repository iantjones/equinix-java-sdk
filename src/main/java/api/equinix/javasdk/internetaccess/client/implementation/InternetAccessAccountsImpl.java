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

package api.equinix.javasdk.internetaccess.client.implementation;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.client.InternetAccessAccounts;
import api.equinix.javasdk.internetaccess.client.internal.AccountClient;
import api.equinix.javasdk.internetaccess.model.AccountAgreement;
import api.equinix.javasdk.internetaccess.model.AccountDetails;
import api.equinix.javasdk.internetaccess.model.json.AccountAgreementJson;
import api.equinix.javasdk.internetaccess.model.json.AccountDetailsJson;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InternetAccessAccountsImpl implements InternetAccessAccounts {

    private final AccountClient serviceClient;

    private final InternetAccess serviceManager;

    public PaginatedList<AccountDetails> list(String operationalUnitsIbx) {
        return list(operationalUnitsIbx, null);
    }

    public PaginatedList<AccountDetails> list(String operationalUnitsIbx, String projectId) {
        Page<AccountDetailsJson> responsePage = this.serviceClient.list(operationalUnitsIbx, projectId);
        return ResponseHandler.toPaginatedList(responsePage, this.serviceClient, (json, client) -> json);
    }

    public AccountDetails getByNumber(String accountNumber) {
        return this.serviceClient.getByNumber(accountNumber);
    }

    @SuppressWarnings("unchecked")
    public PaginatedList<AccountAgreement> agreements(String accountNumber, String ibx) {
        Page<AccountAgreementJson> responsePage = this.serviceClient.agreements(accountNumber, ibx);
        // The internal client is a Pageable<AccountDetails>; its inherited nextPage(...) deserializes
        // each subsequent page using the request's own response type (AccountAgreementJson) and maps
        // the items with the request's page-item mapper (set by AccountClientImpl#agreements — the
        // client's own wrap() is typed for AccountDetailsJson and would ClassCastException), so
        // reusing it for agreement paging is correct — only the generic parameter is laundered.
        Pageable<AccountAgreement> pageableClient = (Pageable<AccountAgreement>) (Object) this.serviceClient;
        PaginatedList<AccountAgreement> agreementList = ResponseHandler.mapPaginatedList(responsePage.getItems(), pageableClient, (json, client) -> json);
        return new PaginatedList<>(agreementList, pageableClient, responsePage.getAssociatedRequest(),
                responsePage.getAssociatedResponse(), responsePage.getPagination());
    }
}
