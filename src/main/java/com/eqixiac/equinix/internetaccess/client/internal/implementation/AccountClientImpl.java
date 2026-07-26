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

package com.eqixiac.equinix.internetaccess.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.http.ParameterMapper;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.internetaccess.client.implementation.InternetAccessConfigImpl;
import com.eqixiac.equinix.internetaccess.client.internal.AccountClient;
import com.eqixiac.equinix.internetaccess.model.AccountAgreement;
import com.eqixiac.equinix.internetaccess.model.AccountDetails;
import com.eqixiac.equinix.internetaccess.model.json.AccountAgreementJson;
import com.eqixiac.equinix.internetaccess.model.json.AccountDetailsJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1 account lookups
 * ({@code GET /internetAccess/v1/accounts}, {@code GET /internetAccess/v1/accounts/{accountNumber}}
 * and {@code GET /internetAccess/v1/accounts/{accountNumber}/agreements}). The account responses are
 * read-only, so the deserialized {@link AccountDetailsJson} (which implements {@link AccountDetails}
 * directly) is returned without a wrapper. The agreements collection deserializes to a separate
 * element type ({@link AccountAgreementJson} implementing {@link AccountAgreement}), so its paginated
 * request — which carries both a path parameter and a query parameter — is built manually and carries
 * its own page-item mapper for pages 2+ (see {@code EquinixRequest#getPageItemMapper()}).
 */
public class AccountClientImpl extends ResourceClientBase<AccountDetails, AccountDetailsJson> implements AccountClient {

    public AccountClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "AccountsV1", AccountDetailsJson.class);
    }

    @Override
    protected AccountDetails wrap(AccountDetailsJson json) {
        return json;
    }

    public Page<AccountDetailsJson> list(String operationalUnitsIbx, String projectId) {
        Map<String, List<String>> queryParams = new HashMap<>();
        ParameterMapper.addAdditionalValue(queryParams, "operationalUnits.ibxs.ibx", operationalUnitsIbx);
        if (projectId != null) {
            ParameterMapper.addAdditionalValue(queryParams, "project.projectId", projectId);
        }
        return listPage("ListAccounts", queryParams);
    }

    public AccountDetailsJson getByNumber(String accountNumber) {
        return getOne("GetAccount", Map.of("accountNumber", accountNumber));
    }

    public Page<AccountAgreementJson> agreements(String accountNumber, String ibx) {
        EquinixRequest<AccountAgreement> request = buildRequestWithPathParams("ListAccountAgreements",
                RequestType.PAGINATED, Map.of("accountNumber", accountNumber), AccountAgreementJson.class);
        request.addSingleQueryParameter("ibx", ibx);
        // Dual-shape paging: this request's elements (AccountAgreementJson) are not this client's
        // JSON model (AccountDetailsJson), so pages 2+ — fetched through the shared inherited
        // nextPage, which by default re-maps items with wrap(AccountDetailsJson) — must carry
        // their own item mapper. Identity, because AccountAgreementJson implements
        // AccountAgreement directly (mirrors the page-1 mapping in
        // InternetAccessAccountsImpl#agreements).
        request.setPageItemMapper(UnaryOperator.identity());
        return ResponseHandler.handlePaginatedListResponse(invoke(request), request);
    }
}
