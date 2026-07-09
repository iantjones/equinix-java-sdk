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
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.client.InternetAccessSignaturePolicies;
import api.equinix.javasdk.internetaccess.client.internal.SignaturePolicyClient;
import api.equinix.javasdk.internetaccess.model.SignaturePolicy;
import api.equinix.javasdk.internetaccess.model.json.SignaturePolicyJson;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InternetAccessSignaturePoliciesImpl implements InternetAccessSignaturePolicies {

    private final SignaturePolicyClient serviceClient;

    private final InternetAccess serviceManager;

    public PaginatedList<SignaturePolicy> list() {
        return list(null);
    }

    public PaginatedList<SignaturePolicy> list(String countryCode) {
        Page<SignaturePolicyJson> responsePage = this.serviceClient.list(countryCode);
        return ResponseHandler.toPaginatedList(responsePage, this.serviceClient, (json, client) -> json);
    }
}
