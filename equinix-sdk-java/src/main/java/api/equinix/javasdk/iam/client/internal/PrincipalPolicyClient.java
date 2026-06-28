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

package api.equinix.javasdk.iam.client.internal;

import api.equinix.javasdk.iam.model.PrincipalPolicy;
import api.equinix.javasdk.iam.model.json.PrincipalPolicyList;
import api.equinix.javasdk.iam.model.json.creators.UpdatePrincipalPolicyRequest;

/**
 * Internal client for project-scoped IAM principal policies
 * ({@code /v1/projects/{projectId}/principalPolicies}). Principal policies are not created or
 * deleted directly; they are listed, retrieved, updated and toggled. Operations (by operationId):
 * {@code listPrincipalPolicies}, {@code getPrincipalPolicy}, {@code updatePrincipalPolicy},
 * {@code enablePrincipalPolicy}, {@code disablePrincipalPolicy}.
 */
public interface PrincipalPolicyClient {

    /** {@code listPrincipalPolicies} */
    PrincipalPolicyList list(String projectId, String pageToken, Integer pageSize);

    /** {@code getPrincipalPolicy} */
    PrincipalPolicy getByUuid(String projectId, String userPrincipal);

    /** {@code updatePrincipalPolicy} */
    PrincipalPolicy update(String projectId, String userPrincipal, UpdatePrincipalPolicyRequest request);

    /** {@code enablePrincipalPolicy} */
    PrincipalPolicy enable(String projectId, String userPrincipal, String lastRev);

    /** {@code disablePrincipalPolicy} */
    PrincipalPolicy disable(String projectId, String userPrincipal, String lastRev);
}
