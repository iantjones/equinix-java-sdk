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

import com.eqixiac.equinix.iam.model.PolicyMask;
import com.eqixiac.equinix.iam.model.json.PolicyMaskList;
import com.eqixiac.equinix.iam.model.json.creators.CreatePolicyMaskRequest;
import com.eqixiac.equinix.iam.model.json.creators.UpdatePolicyMaskRequest;

/**
 * Client interface for project-scoped IAM policy masks
 * ({@code /v1/projects/{projectId}/policyMasks}).
 *
 * <p>A policy mask constrains the permissions an underlying policy would otherwise grant. This
 * interface provides the policy mask lifecycle — list / create / get / update / delete plus the
 * enable/disable lifecycle toggles. All operations are scoped to a {@code projectId}.</p>
 */
public interface IAMPolicyMasks {

    /**
     * Lists the policy masks of a project (first page).
     *
     * @param projectId the project identifier
     * @return the first page of policy masks
     */
    PolicyMaskList list(String projectId);

    /**
     * Lists the policy masks of a project, controlling pagination.
     *
     * @param projectId the project identifier
     * @param pageToken the opaque page token from a prior response, or {@code null} for the first page
     * @param pageSize the maximum number of results per page, or {@code null} for the server default
     * @return the requested page of policy masks
     */
    PolicyMaskList list(String projectId, String pageToken, Integer pageSize);

    /**
     * Creates a new policy mask in a project.
     *
     * @param projectId the project identifier
     * @param request the policy mask to create
     * @return the created policy mask
     */
    PolicyMask create(String projectId, CreatePolicyMaskRequest request);

    /**
     * Retrieves a single policy mask by id.
     *
     * @param projectId the project identifier
     * @param policyMaskId the policy mask identifier
     * @return the matching policy mask
     */
    PolicyMask getByUuid(String projectId, String policyMaskId);

    /**
     * Updates a policy mask.
     *
     * @param projectId the project identifier
     * @param policyMaskId the policy mask identifier
     * @param request the update (the request's {@code lastRev} guards against concurrent updates)
     * @return the updated policy mask
     */
    PolicyMask update(String projectId, String policyMaskId, UpdatePolicyMaskRequest request);

    /**
     * Deletes a policy mask.
     *
     * <p>The spec requires the policy mask's last-known revision in the request body for
     * optimistic-concurrency-controlled deletion.</p>
     *
     * @param projectId the project identifier
     * @param policyMaskId the policy mask identifier
     * @param lastRev the last-known revision of the mask (required; from {@link PolicyMask#getRev()})
     * @return {@code true} if the deletion request was accepted
     */
    Boolean delete(String projectId, String policyMaskId, String lastRev);

    /**
     * Enables a previously disabled policy mask.
     *
     * @param projectId the project identifier
     * @param policyMaskId the policy mask identifier
     * @param lastRev the last-known revision of the mask (for concurrency control), or {@code null}
     * @return the updated policy mask
     */
    PolicyMask enable(String projectId, String policyMaskId, String lastRev);

    /**
     * Disables a policy mask.
     *
     * @param projectId the project identifier
     * @param policyMaskId the policy mask identifier
     * @param lastRev the last-known revision of the mask (for concurrency control), or {@code null}
     * @return the updated policy mask
     */
    PolicyMask disable(String projectId, String policyMaskId, String lastRev);
}
