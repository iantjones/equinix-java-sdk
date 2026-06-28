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

package api.equinix.javasdk.sts.client.implementation;

import api.equinix.javasdk.STS;
import api.equinix.javasdk.sts.client.STSTokens;
import api.equinix.javasdk.sts.client.internal.TokenClient;
import api.equinix.javasdk.sts.model.StsToken;
import api.equinix.javasdk.sts.model.json.GrantedAccessPolicyPage;
import api.equinix.javasdk.sts.model.json.creators.ListPoliciesGrantedRequest;
import api.equinix.javasdk.sts.model.json.creators.TokenRequest;

public class STSTokensImpl implements STSTokens {

    private final STS serviceManager;

    private final TokenClient tokenClient;

    public STSTokensImpl(TokenClient tokenClient, STS serviceManager) {
        this.serviceManager = serviceManager;
        this.tokenClient = tokenClient;
    }

    public StsToken generate(TokenRequest request) {
        return this.tokenClient.generateStsToken(request);
    }

    public GrantedAccessPolicyPage listAccessPoliciesGranted(ListPoliciesGrantedRequest request) {
        return this.tokenClient.listAccessPoliciesGranted(request);
    }
}
