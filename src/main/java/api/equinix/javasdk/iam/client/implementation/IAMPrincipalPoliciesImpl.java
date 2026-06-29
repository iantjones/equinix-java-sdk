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

package api.equinix.javasdk.iam.client.implementation;

import api.equinix.javasdk.IAM;
import api.equinix.javasdk.iam.client.IAMPrincipalPolicies;
import api.equinix.javasdk.iam.client.internal.PrincipalPolicyClient;
import api.equinix.javasdk.iam.model.PrincipalPolicy;
import api.equinix.javasdk.iam.model.json.PrincipalPolicyList;
import api.equinix.javasdk.iam.model.json.creators.UpdatePrincipalPolicyRequest;

public class IAMPrincipalPoliciesImpl implements IAMPrincipalPolicies {

    private final IAM serviceManager;

    private final PrincipalPolicyClient principalPolicyClient;

    public IAMPrincipalPoliciesImpl(PrincipalPolicyClient principalPolicyClient, IAM serviceManager) {
        this.serviceManager = serviceManager;
        this.principalPolicyClient = principalPolicyClient;
    }

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
