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

package api.equinix.javasdk.customerportal.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.BillingAccountSearchClient;
import api.equinix.javasdk.customerportal.model.BillingAccountV2;
import api.equinix.javasdk.customerportal.model.json.BillingAccountV2Json;
import api.equinix.javasdk.customerportal.model.json.creators.BillingAccountSearchRequest;

import java.util.Map;

public class BillingAccountSearchClientImpl extends ResourceClientBase<BillingAccountV2, BillingAccountV2Json>
        implements BillingAccountSearchClient<BillingAccountV2> {

    public BillingAccountSearchClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "BillingAccountsSearch", BillingAccountV2Json.class);
    }

    @Override
    protected BillingAccountV2 wrap(BillingAccountV2Json json) {
        return json;
    }

    public Page<BillingAccountV2Json> search(BillingAccountSearchRequest request) {
        return searchPage("SearchBillingAccounts", request);
    }

    public BillingAccountV2Json getByAccountNumber(String accountNumber) {
        return getOne("GetBillingAccountByNumber", Map.of("accountNumber", accountNumber));
    }

    public BillingAccountV2Json getByAccountId(String accountId) {
        return getOne("GetBillingAccountById", Map.of("accountId", accountId));
    }
}
