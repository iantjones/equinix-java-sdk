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

package com.eqixiac.equinix.sts.client.internal;

import com.eqixiac.equinix.sts.model.StsToken;
import com.eqixiac.equinix.sts.model.json.GrantedAccessPolicyPage;
import com.eqixiac.equinix.sts.model.json.creators.ListPoliciesGrantedRequest;
import com.eqixiac.equinix.sts.model.json.creators.TokenRequest;

/**
 * Internal client for the STS pre-auth token operations:
 * {@code POST /v1/token} (operationId {@code generateStsToken}) — the OAuth 2.0 / RFC&nbsp;8693
 * token exchange (consumes {@code application/x-www-form-urlencoded}) — and
 * {@code POST /v1/accessPoliciesGranted} (operationId {@code listAccessPoliciesGranted}).
 */
public interface TokenClient {

    StsToken generateStsToken(TokenRequest request);

    GrantedAccessPolicyPage listAccessPoliciesGranted(ListPoliciesGrantedRequest request);
}
