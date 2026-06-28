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

import api.equinix.javasdk.iam.model.PolicyMask;
import api.equinix.javasdk.iam.model.json.PolicyMaskList;
import api.equinix.javasdk.iam.model.json.creators.CreatePolicyMaskRequest;
import api.equinix.javasdk.iam.model.json.creators.UpdatePolicyMaskRequest;

/**
 * Internal client for project-scoped IAM policy masks
 * ({@code /v1/projects/{projectId}/policyMasks}). Operations (by operationId):
 * {@code listPolicyMasks}, {@code createPolicyMask}, {@code getPolicyMask},
 * {@code updatePolicyMask}, {@code deletePolicyMask}, {@code enablePolicyMask},
 * {@code disablePolicyMask}.
 */
public interface PolicyMaskClient {

    /** {@code listPolicyMasks} */
    PolicyMaskList list(String projectId, String pageToken, Integer pageSize);

    /** {@code createPolicyMask} */
    PolicyMask create(String projectId, CreatePolicyMaskRequest request);

    /** {@code getPolicyMask} */
    PolicyMask getByUuid(String projectId, String policyMaskId);

    /** {@code updatePolicyMask} */
    PolicyMask update(String projectId, String policyMaskId, UpdatePolicyMaskRequest request);

    /** {@code deletePolicyMask} */
    Boolean delete(String projectId, String policyMaskId);

    /** {@code enablePolicyMask} */
    PolicyMask enable(String projectId, String policyMaskId, String lastRev);

    /** {@code disablePolicyMask} */
    PolicyMask disable(String projectId, String policyMaskId, String lastRev);
}
