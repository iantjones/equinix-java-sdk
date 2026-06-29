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

package api.equinix.javasdk.internetaccess.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConfigImpl;
import api.equinix.javasdk.internetaccess.client.internal.AccountClient;
import api.equinix.javasdk.internetaccess.model.AccountAgreement;
import api.equinix.javasdk.internetaccess.model.AccountDetails;
import api.equinix.javasdk.internetaccess.model.json.AccountAgreementJson;
import api.equinix.javasdk.internetaccess.model.json.AccountDetailsJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1 account lookups
 * ({@code GET /internetAccess/v1/accounts}, {@code GET /internetAccess/v1/accounts/{accountNumber}}
 * and {@code GET /internetAccess/v1/accounts/{accountNumber}/agreements}). The account responses are
 * read-only, so the deserialized {@link AccountDetailsJson} (which implements {@link AccountDetails}
 * directly) is returned without a wrapper. The agreements collection deserializes to a separate
 * element type ({@link AccountAgreementJson} implementing {@link AccountAgreement}), so its paginated
 * request — which carries both a path parameter and a query parameter — is built manually.
 */
public class AccountClientImpl extends ResourceClientBase<AccountDetails, AccountDetailsJson> implements AccountClient {

    public AccountClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "AccountsV1", AccountDetailsJson.class);
    }

    @Override
    protected AccountDetails wrap(AccountDetailsJson json) {
        return json;
    }

    public Page<AccountDetails, AccountDetailsJson> list(String operationalUnitsIbx, String projectId) {
        Map<String, List<String>> queryParams = new HashMap<>();
        Utils.addAdditionalValue(queryParams, "operationalUnits.ibxs.ibx", operationalUnitsIbx);
        if (projectId != null) {
            Utils.addAdditionalValue(queryParams, "project.projectId", projectId);
        }
        return listPage("ListAccounts", queryParams);
    }

    public AccountDetailsJson getByNumber(String accountNumber) {
        return getOne("GetAccount", Map.of("accountNumber", accountNumber));
    }

    public Page<AccountAgreement, AccountAgreementJson> agreements(String accountNumber, String ibx) {
        EquinixRequest<AccountAgreement> request = buildRequestWithPathParams("ListAccountAgreements",
                RequestType.PAGINATED, Map.of("accountNumber", accountNumber), AccountAgreementJson.class);
        request.addSingleQueryParameter("ibx", ibx);
        return Utils.handlePaginatedListResponse(invoke(request), request);
    }
}
