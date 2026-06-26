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

package api.equinix.javasdk.networkedge.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.AccountClient;
import api.equinix.javasdk.networkedge.model.Account;
import api.equinix.javasdk.networkedge.model.json.AccountJson;
import api.equinix.javasdk.networkedge.model.json.BackupJson;
import api.equinix.javasdk.networkedge.model.wrappers.AccountWrapper;

import java.util.List;
import java.util.Map;

/**
 * <p>AccountClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class AccountClientImpl extends ResourceClientBase<Account, AccountJson> implements AccountClient<Account> {

    /**
     * <p>Constructor for AccountClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl} object.
     */
    public AccountClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "Accounts", AccountJson.class);
    }

    /** {@inheritDoc} */
    @Override
    protected Account wrap(AccountJson json) {
        return new AccountWrapper(json, this);
    }

    /** {@inheritDoc} */
    public List<AccountJson> list(MetroCode metroCode) {
        Map<String, String> pParams = Map.of("metroCode", metroCode.toString());
        EquinixRequest<Account> equinixRequest = this.buildRequestWithPathParams("ListAccounts", RequestType.LIST, pParams, AccountJson.class);
        EquinixResponse<Account> equinixResponse = this.invoke(equinixRequest);
        AccountJson.NestedList nestedList = Utils.handleSingletonResponse(equinixResponse, equinixRequest);
        return nestedList.getAccounts();
    }

    /** {@inheritDoc} */
    public byte[] getOrderSummary(RequestBuilder.OrderSummary requestBuilder) {
        Map<String, List<String>> qParams = Utils.newMap(requestBuilder);
        EquinixRequest<BackupJson> equinixRequest = this.buildRequestWithQueryParams("GetOrderSummary", RequestType.SINGLE, qParams);
        EquinixResponse<BackupJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleByteResponse(equinixResponse);
    }
}
