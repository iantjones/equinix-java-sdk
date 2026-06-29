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
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConfigImpl;
import api.equinix.javasdk.internetaccess.client.internal.SignaturePolicyClient;
import api.equinix.javasdk.internetaccess.model.SignaturePolicy;
import api.equinix.javasdk.internetaccess.model.json.SignaturePolicyJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1 signature-policies lookup
 * {@code GET /internetAccess/v1/signaturePolicies}. The {@code SignaturePolicy} response is
 * read-only, so the deserialized {@link SignaturePolicyJson} (which implements
 * {@link SignaturePolicy} directly) is returned without a wrapper.
 */
public class SignaturePolicyClientImpl extends ResourceClientBase<SignaturePolicy, SignaturePolicyJson> implements SignaturePolicyClient {

    public SignaturePolicyClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "SignaturePoliciesV1", SignaturePolicyJson.class);
    }

    @Override
    protected SignaturePolicy wrap(SignaturePolicyJson json) {
        return json;
    }

    public Page<SignaturePolicy, SignaturePolicyJson> list(String countryCode) {
        Map<String, List<String>> queryParams = new HashMap<>();
        if (countryCode != null) {
            Utils.addAdditionalValue(queryParams, "location.countryCode", countryCode);
        }
        return listPage("ListSignaturePolicies", queryParams);
    }
}
