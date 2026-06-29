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

package api.equinix.javasdk.sts.client;

import api.equinix.javasdk.sts.model.StsToken;
import api.equinix.javasdk.sts.model.json.GrantedAccessPolicyPage;
import api.equinix.javasdk.sts.model.json.creators.ListPoliciesGrantedRequest;
import api.equinix.javasdk.sts.model.json.creators.TokenRequest;

/**
 * Client interface for the STS pre-auth token operations.
 *
 * <p>Exchanges an OIDC ID token for an Equinix STS access token via RFC&nbsp;8693 token exchange
 * ({@code POST /v1/token}, operationId {@code generateStsToken}) and lists the access policies
 * granted to a subject within a project ({@code POST /v1/accessPoliciesGranted}, operationId
 * {@code listAccessPoliciesGranted}).</p>
 */
public interface STSTokens {

    /**
     * Exchanges an ID token for an STS access token (RFC&nbsp;8693 token exchange).
     *
     * @param request the token-exchange request (grant type, subject token and optional scope)
     * @return the issued access token
     */
    StsToken generate(TokenRequest request);

    /**
     * Lists the access policies granted to the subject identified by the given token within a
     * project (first page or the page identified by the request's {@code pageToken}).
     *
     * @param request the granted-policies request (project, subject token and pagination)
     * @return a page of granted access policy ids
     */
    GrantedAccessPolicyPage listAccessPoliciesGranted(ListPoliciesGrantedRequest request);
}
