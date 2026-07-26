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

package com.eqixiac.equinix.iam.client;

import com.eqixiac.equinix.iam.model.AccessPolicy;
import com.eqixiac.equinix.iam.model.AccessPolicyGrant;
import com.eqixiac.equinix.iam.model.json.AccessPolicyGrantList;
import com.eqixiac.equinix.iam.model.json.AccessPolicyList;
import com.eqixiac.equinix.iam.model.json.creators.AddGrantRequest;
import com.eqixiac.equinix.iam.model.json.creators.CreateAccessPolicyRequest;
import com.eqixiac.equinix.iam.model.json.creators.UpdateAccessPolicyRequest;

/**
 * Client interface for project-scoped IAM access policies and their grants
 * ({@code /v1/projects/{projectId}/accessPolicies}).
 *
 * <p>Provides the access policy lifecycle — list / create / get / update / delete and the
 * enable/disable lifecycle toggles — plus management of the policy's grants (which principals,
 * groups or projects the policy is granted to). All operations are scoped to a {@code projectId}.</p>
 *
 * <p><b>ERN addressing.</b> The single-policy operations (get / update / enable / disable / grants)
 * additionally accept the spec's ERN-addressing form, where {@code projectId} is the literal
 * {@code "*"} and {@code accessPolicyId} is an Equinix Resource Name (ERN) — or a managed-policy id
 * — rather than a local {@code accesspolicy:} id. Because an ERN contains reserved characters
 * ({@code :} and {@code /}), the API requires it to be URL-encoded when used as a path segment.
 * This SDK does not provide a dedicated {@code byErn} overload: the shared core path-substitution
 * layer does not percent-encode individual path segments, and adding that encoding would require a
 * core change. Callers needing ERN addressing must therefore pass a pre-URL-encoded ERN as the
 * {@code accessPolicyId} argument together with {@code projectId == "*"}.</p>
 */
public interface IAMAccessPolicies {

    /**
     * Lists the access policies of a project (first page).
     *
     * @param projectId the project identifier
     * @return the first page of access policies
     */
    AccessPolicyList list(String projectId);

    /**
     * Lists the access policies of a project, controlling pagination.
     *
     * @param projectId the project identifier
     * @param pageToken the opaque page token from a prior response, or {@code null} for the first page
     * @param pageSize the maximum number of results per page, or {@code null} for the server default
     * @return the requested page of access policies
     */
    AccessPolicyList list(String projectId, String pageToken, Integer pageSize);

    /**
     * Creates a new access policy in a project.
     *
     * @param projectId the project identifier
     * @param request the access policy to create
     * @return the created access policy
     */
    AccessPolicy create(String projectId, CreateAccessPolicyRequest request);

    /**
     * Retrieves a single access policy by id.
     *
     * @param projectId the project identifier
     * @param accessPolicyId the access policy identifier
     * @return the matching access policy
     */
    AccessPolicy getByUuid(String projectId, String accessPolicyId);

    /**
     * Updates an access policy.
     *
     * @param projectId the project identifier
     * @param accessPolicyId the access policy identifier
     * @param request the update (the request's {@code lastRev} guards against concurrent updates)
     * @return the updated access policy
     */
    AccessPolicy update(String projectId, String accessPolicyId, UpdateAccessPolicyRequest request);

    /**
     * Deletes an access policy.
     *
     * <p>The spec requires the policy's last-known revision in the request body for
     * optimistic-concurrency-controlled deletion.</p>
     *
     * @param projectId the project identifier
     * @param accessPolicyId the access policy identifier
     * @param lastRev the last-known revision of the policy (required; from {@link AccessPolicy#getRev()})
     * @return {@code true} if the deletion request was accepted
     */
    Boolean delete(String projectId, String accessPolicyId, String lastRev);

    /**
     * Enables a previously disabled access policy.
     *
     * @param projectId the project identifier
     * @param accessPolicyId the access policy identifier
     * @param lastRev the last-known revision of the policy (for concurrency control), or {@code null}
     * @return the updated access policy
     */
    AccessPolicy enable(String projectId, String accessPolicyId, String lastRev);

    /**
     * Disables an access policy.
     *
     * @param projectId the project identifier
     * @param accessPolicyId the access policy identifier
     * @param lastRev the last-known revision of the policy (for concurrency control), or {@code null}
     * @return the updated access policy
     */
    AccessPolicy disable(String projectId, String accessPolicyId, String lastRev);

    /**
     * Lists the grants of an access policy (first page).
     *
     * @param projectId the project identifier
     * @param accessPolicyId the access policy identifier
     * @return the first page of grants
     */
    AccessPolicyGrantList listGrants(String projectId, String accessPolicyId);

    /**
     * Lists the grants of an access policy, controlling pagination.
     *
     * @param projectId the project identifier
     * @param accessPolicyId the access policy identifier
     * @param pageToken the opaque page token from a prior response, or {@code null} for the first page
     * @param pageSize the maximum number of results per page, or {@code null} for the server default
     * @return the requested page of grants
     */
    AccessPolicyGrantList listGrants(String projectId, String accessPolicyId, String pageToken, Integer pageSize);

    /**
     * Adds a grant to an access policy.
     *
     * @param projectId the project identifier
     * @param accessPolicyId the access policy identifier
     * @param request the grant to add (the grantee and optional {@code lastRev})
     * @return the created grant
     */
    AccessPolicyGrant addGrant(String projectId, String accessPolicyId, AddGrantRequest request);

    /**
     * Removes a grant from an access policy.
     *
     * @param projectId the project identifier
     * @param accessPolicyId the access policy identifier
     * @param grantId the grant identifier
     * @return {@code true} if the removal request was accepted
     */
    Boolean removeGrant(String projectId, String accessPolicyId, String grantId);
}
