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

package com.eqixiac.equinix.iam.client.internal;

import com.eqixiac.equinix.iam.model.AccessPolicy;
import com.eqixiac.equinix.iam.model.AccessPolicyGrant;
import com.eqixiac.equinix.iam.model.json.AccessPolicyGrantList;
import com.eqixiac.equinix.iam.model.json.AccessPolicyList;
import com.eqixiac.equinix.iam.model.json.creators.AddGrantRequest;
import com.eqixiac.equinix.iam.model.json.creators.CreateAccessPolicyRequest;
import com.eqixiac.equinix.iam.model.json.creators.UpdateAccessPolicyRequest;

/**
 * Internal client for project-scoped IAM access policies and their grants
 * ({@code /v1/projects/{projectId}/accessPolicies}). Operations (by operationId):
 * {@code listAccessPolicies}, {@code createAccessPolicy}, {@code getAccessPolicy},
 * {@code updateAccessPolicy}, {@code deleteAccessPolicy}, {@code enableAccessPolicy},
 * {@code disableAccessPolicy}, {@code listGrants}, {@code addGrant}, {@code removeGrant}.
 */
public interface AccessPolicyClient {

    AccessPolicyList list(String projectId, String pageToken, Integer pageSize);

    AccessPolicy create(String projectId, CreateAccessPolicyRequest request);

    AccessPolicy getByUuid(String projectId, String accessPolicyId);

    AccessPolicy update(String projectId, String accessPolicyId, UpdateAccessPolicyRequest request);

    Boolean delete(String projectId, String accessPolicyId, String lastRev);

    AccessPolicy enable(String projectId, String accessPolicyId, String lastRev);

    AccessPolicy disable(String projectId, String accessPolicyId, String lastRev);

    AccessPolicyGrantList listGrants(String projectId, String accessPolicyId, String pageToken, Integer pageSize);

    AccessPolicyGrant addGrant(String projectId, String accessPolicyId, AddGrantRequest request);

    Boolean removeGrant(String projectId, String accessPolicyId, String grantId);
}
