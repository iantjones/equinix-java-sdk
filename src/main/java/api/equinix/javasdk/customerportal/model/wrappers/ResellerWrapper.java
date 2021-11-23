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

package api.equinix.javasdk.customerportal.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.customerportal.enums.AccountType;
import api.equinix.javasdk.customerportal.model.Reseller;
import api.equinix.javasdk.customerportal.model.json.ResellerJson;
import lombok.Getter;

import java.util.List;

public class ResellerWrapper extends ResourceImpl<Reseller> implements Reseller {

    private ResellerJson json;
    @Getter
    private final Pageable<Reseller> serviceClient;

    public ResellerWrapper(ResellerJson resellerJson, Pageable<Reseller> serviceClient) {
        this.json = resellerJson;
        this.serviceClient = serviceClient;
    }

    public String getAccountNumber() {
        return null;
    }

    public String getAccountName() {
        return null;
    }

    public AccountType getAccountType() {
        return null;
    }

    public List<String> getIbxs() {
        return null;
    }
}
