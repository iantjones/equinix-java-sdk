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

import com.eqixiac.equinix.iam.model.PermissionSet;
import com.eqixiac.equinix.iam.model.json.PermissionSetList;
import com.eqixiac.equinix.iam.model.json.creators.CreatePermissionSetRequest;
import com.eqixiac.equinix.iam.model.json.creators.UpdatePermissionSetRequest;

/**
 * Client interface for project-scoped IAM permission sets
 * ({@code /v1/projects/{projectId}/permissionSets}).
 *
 * <p>Provides the permission set lifecycle — list / create / get / update / delete. A permission
 * set groups the granular permissions that a role or access policy confers. All operations are
 * scoped to a {@code projectId}.</p>
 *
 * <p><b>ERN addressing.</b> {@code getPermissionSet} additionally accepts the spec's ERN-addressing
 * form, where {@code projectId} is the literal {@code "*"} and {@code permissionSetId} is an Equinix
 * Resource Name (ERN) — or a managed-permission-set id — rather than a local {@code permissionset:}
 * id. Because an ERN contains reserved characters ({@code :} and {@code /}), the API requires it to
 * be URL-encoded when used as a path segment. This SDK does not provide a dedicated {@code byErn}
 * overload: the shared core path-substitution layer does not percent-encode individual path
 * segments, and adding that encoding would require a core change. Callers needing ERN addressing
 * must therefore pass a pre-URL-encoded ERN as the {@code permissionSetId} argument together with
 * {@code projectId == "*"}.</p>
 */
public interface IAMPermissionSets {

    /**
     * Lists the permission sets of a project (first page).
     *
     * @param projectId the project identifier
     * @return the first page of permission sets
     */
    PermissionSetList list(String projectId);

    /**
     * Lists the permission sets of a project, controlling pagination.
     *
     * @param projectId the project identifier
     * @param pageToken the opaque page token from a prior response, or {@code null} for the first page
     * @param pageSize the maximum number of results per page, or {@code null} for the server default
     * @return the requested page of permission sets
     */
    PermissionSetList list(String projectId, String pageToken, Integer pageSize);

    /**
     * Creates a new permission set in a project.
     *
     * @param projectId the project identifier
     * @param request the permission set to create
     * @return the created permission set
     */
    PermissionSet create(String projectId, CreatePermissionSetRequest request);

    /**
     * Retrieves a single permission set by id.
     *
     * @param projectId the project identifier
     * @param permissionSetId the permission set identifier
     * @return the matching permission set
     */
    PermissionSet getByUuid(String projectId, String permissionSetId);

    /**
     * Updates a permission set.
     *
     * @param projectId the project identifier
     * @param permissionSetId the permission set identifier
     * @param request the update (the request's {@code lastRev} guards against concurrent updates)
     * @return the updated permission set
     */
    PermissionSet update(String projectId, String permissionSetId, UpdatePermissionSetRequest request);

    /**
     * Deletes a permission set.
     *
     * <p>The spec requires the permission set's last-known revision in the request body for
     * optimistic-concurrency-controlled deletion.</p>
     *
     * @param projectId the project identifier
     * @param permissionSetId the permission set identifier
     * @param lastRev the last-known revision of the permission set (required; from {@link PermissionSet#getRev()})
     * @return {@code true} if the deletion request was accepted
     */
    Boolean delete(String projectId, String permissionSetId, String lastRev);
}
