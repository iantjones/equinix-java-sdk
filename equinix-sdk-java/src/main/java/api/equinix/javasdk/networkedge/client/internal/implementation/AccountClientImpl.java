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
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.AccountClient;
import api.equinix.javasdk.networkedge.model.Account;
import api.equinix.javasdk.networkedge.model.json.AccountJson;
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
        AccountJson.NestedList nestedList = getAs("ListAccounts", Map.of("metroCode", metroCode.toString()), null, AccountJson.NestedList.class);
        return nestedList.getData();
    }

    /** {@inheritDoc} */
    public byte[] getOrderSummary(RequestBuilder.OrderSummary requestBuilder) {
        return bytesOp("GetOrderSummary", null, Utils.newMap(requestBuilder));
    }
}
