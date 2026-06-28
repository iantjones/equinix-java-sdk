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

package api.equinix.javasdk.iam.client;

import api.equinix.javasdk.iam.model.PrincipalPolicy;
import api.equinix.javasdk.iam.model.json.PrincipalPolicyList;
import api.equinix.javasdk.iam.model.json.creators.UpdatePrincipalPolicyRequest;

/**
 * Client interface for project-scoped IAM principal policies
 * ({@code /v1/projects/{projectId}/principalPolicies}).
 *
 * <p>A principal policy binds a user principal to the policy that governs their access within a
 * project. Principal policies are not created or deleted directly — instead they are listed,
 * retrieved, updated, and toggled via the enable/disable lifecycle. All operations are scoped to a
 * {@code projectId}.</p>
 */
public interface IAMPrincipalPolicies {

    /**
     * Lists the principal policies of a project (first page).
     *
     * @param projectId the project identifier
     * @return the first page of principal policies
     */
    PrincipalPolicyList list(String projectId);

    /**
     * Lists the principal policies of a project, controlling pagination.
     *
     * @param projectId the project identifier
     * @param pageToken the opaque page token from a prior response, or {@code null} for the first page
     * @param pageSize the maximum number of results per page, or {@code null} for the server default
     * @return the requested page of principal policies
     */
    PrincipalPolicyList list(String projectId, String pageToken, Integer pageSize);

    /**
     * Retrieves the principal policy for a single user principal.
     *
     * @param projectId the project identifier
     * @param userPrincipal the user principal identifier
     * @return the matching principal policy
     */
    PrincipalPolicy getByUuid(String projectId, String userPrincipal);

    /**
     * Updates a principal policy.
     *
     * @param projectId the project identifier
     * @param userPrincipal the user principal identifier
     * @param request the update (the request's {@code lastRev} guards against concurrent updates)
     * @return the updated principal policy
     */
    PrincipalPolicy update(String projectId, String userPrincipal, UpdatePrincipalPolicyRequest request);

    /**
     * Enables a previously disabled principal policy.
     *
     * @param projectId the project identifier
     * @param userPrincipal the user principal identifier
     * @param lastRev the last-known revision of the policy (for concurrency control), or {@code null}
     * @return the updated principal policy
     */
    PrincipalPolicy enable(String projectId, String userPrincipal, String lastRev);

    /**
     * Disables a principal policy.
     *
     * @param projectId the project identifier
     * @param userPrincipal the user principal identifier
     * @param lastRev the last-known revision of the policy (for concurrency control), or {@code null}
     * @return the updated principal policy
     */
    PrincipalPolicy disable(String projectId, String userPrincipal, String lastRev);
}
