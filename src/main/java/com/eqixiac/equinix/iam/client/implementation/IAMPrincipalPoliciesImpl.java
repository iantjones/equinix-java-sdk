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

package com.eqixiac.equinix.iam.client.implementation;

import com.eqixiac.equinix.IAM;
import com.eqixiac.equinix.iam.client.IAMPrincipalPolicies;
import com.eqixiac.equinix.iam.client.internal.PrincipalPolicyClient;
import com.eqixiac.equinix.iam.model.PrincipalPolicy;
import com.eqixiac.equinix.iam.model.json.PrincipalPolicyList;
import com.eqixiac.equinix.iam.model.json.creators.UpdatePrincipalPolicyRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IAMPrincipalPoliciesImpl implements IAMPrincipalPolicies {

    private final PrincipalPolicyClient principalPolicyClient;

    private final IAM serviceManager;

    public PrincipalPolicyList list(String projectId) {
        return this.principalPolicyClient.list(projectId, null, null);
    }

    public PrincipalPolicyList list(String projectId, String pageToken, Integer pageSize) {
        return this.principalPolicyClient.list(projectId, pageToken, pageSize);
    }

    public PrincipalPolicy getByUuid(String projectId, String userPrincipal) {
        return this.principalPolicyClient.getByUuid(projectId, userPrincipal);
    }

    public PrincipalPolicy update(String projectId, String userPrincipal, UpdatePrincipalPolicyRequest request) {
        return this.principalPolicyClient.update(projectId, userPrincipal, request);
    }

    public PrincipalPolicy enable(String projectId, String userPrincipal, String lastRev) {
        return this.principalPolicyClient.enable(projectId, userPrincipal, lastRev);
    }

    public PrincipalPolicy disable(String projectId, String userPrincipal, String lastRev) {
        return this.principalPolicyClient.disable(projectId, userPrincipal, lastRev);
    }
}
