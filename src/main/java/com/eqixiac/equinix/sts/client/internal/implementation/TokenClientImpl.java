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

package com.eqixiac.equinix.sts.client.internal.implementation;

import com.eqixiac.equinix.core.client.ClientBase;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.request.RequestBody;
import com.eqixiac.equinix.sts.client.implementation.STSConfigImpl;
import com.eqixiac.equinix.sts.client.internal.TokenClient;
import com.eqixiac.equinix.sts.model.StsToken;
import com.eqixiac.equinix.sts.model.json.GrantedAccessPolicyPage;
import com.eqixiac.equinix.sts.model.json.StsTokenJson;
import com.eqixiac.equinix.sts.model.json.creators.ListPoliciesGrantedRequest;
import com.eqixiac.equinix.sts.model.json.creators.TokenRequest;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Internal client implementation for the STS pre-auth token operations.
 *
 * <p>{@code generateStsToken} is unusual in that it consumes
 * {@code application/x-www-form-urlencoded} rather than JSON; the form fields are attached as a
 * {@link RequestBody#form(java.util.Map)} body (encoded by the core request factory at dispatch),
 * leaving the rest of the dispatch — auth headers, retries, response handling — to the shared
 * infrastructure. The {@code TokenResponse} and {@code ListAccessPoliciesGrantedOutput} responses
 * are read-only, so the deserialized JSON models are returned directly.</p>
 */
public class TokenClientImpl extends ClientBase implements TokenClient {

    private static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

    public TokenClientImpl(STSConfigImpl configClient) {
        super(configClient, "STS", "Tokens");
    }

    @Override
    public StsToken generateStsToken(TokenRequest request) {
        EquinixRequest<StsTokenJson> equinixRequest =
                buildRequest("GenerateStsToken", RequestType.SINGLE, StsTokenJson.class);
        equinixRequest.setContentType(FORM_URLENCODED);
        equinixRequest.setBody(RequestBody.form(request.toFormFields()));
        return ResponseHandler.handleSingletonResponse(invoke(equinixRequest), equinixRequest);
    }

    @Override
    public GrantedAccessPolicyPage listAccessPoliciesGranted(ListPoliciesGrantedRequest request) {
        return postForType("ListAccessPoliciesGranted", null, request,
                new TypeReference<GrantedAccessPolicyPage>() {
                });
    }
}
