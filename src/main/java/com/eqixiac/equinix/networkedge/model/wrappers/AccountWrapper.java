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

package com.eqixiac.equinix.networkedge.model.wrappers;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.networkedge.model.Account;
import com.eqixiac.equinix.networkedge.model.json.AccountJson;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 *
 * @author ianjones
 */
public class AccountWrapper extends ResourceImpl<Account> implements Account {

    @Delegate
    private AccountJson jsonObject;
    @Getter
    private final Pageable<Account> serviceClient;

    public AccountWrapper(AccountJson accountJson, Pageable<Account> serviceClient) {
        this.jsonObject = accountJson;
        this.serviceClient = serviceClient;
    }
}
