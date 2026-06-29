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

import api.equinix.javasdk.iam.model.json.RoleList;

/**
 * Client interface for the IAM roles catalog.
 *
 * <p>Lists the pre-defined roles available globally
 * ({@code GET /v1/roles}, operationId {@code listRoles}) or scoped to a project
 * ({@code GET /v1/projects/{projectId}/roles}, operationId {@code listRolesByProjectId}).</p>
 *
 * <p>Results are opaque-token paginated: each {@link RoleList} carries a {@code nextPageToken};
 * pass it back as the {@code pageToken} argument to fetch the next page.</p>
 */
public interface IAMRoles {

    /**
     * Lists all roles (first page).
     *
     * @return the first page of roles
     */
    RoleList list();

    /**
     * Lists all roles, controlling pagination and optional project ERN filtering.
     *
     * @param pageToken the opaque page token from a prior response, or {@code null} for the first page
     * @param pageSize the maximum number of results per page, or {@code null} for the server default
     * @param projectErn the project ERN to scope the listing to, or {@code null}
     * @return the requested page of roles
     */
    RoleList list(String pageToken, Integer pageSize, String projectErn);

    /**
     * Lists all roles, controlling pagination and optional project scoping by id or ERN.
     *
     * <p>{@code projectId} and {@code projectErn} are mutually exclusive — supply at most one.</p>
     *
     * @param pageToken the opaque page token from a prior response, or {@code null} for the first page
     * @param pageSize the maximum number of results per page, or {@code null} for the server default
     * @param projectId the project id to scope the listing to (mutually exclusive with
     *                  {@code projectErn}), or {@code null}
     * @param projectErn the project ERN to scope the listing to (mutually exclusive with
     *                  {@code projectId}), or {@code null}
     * @return the requested page of roles
     */
    RoleList list(String pageToken, Integer pageSize, String projectId, String projectErn);

    /**
     * Lists the roles available within a specific project (first page).
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @return the first page of project-scoped roles
     */
    RoleList listByProject(String projectId);

    /**
     * Lists the roles available within a specific project, controlling pagination.
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @param pageToken the opaque page token from a prior response, or {@code null} for the first page
     * @param pageSize the maximum number of results per page, or {@code null} for the server default
     * @param projectErn the project ERN (mutually exclusive with {@code projectId}), or {@code null}
     * @return the requested page of project-scoped roles
     */
    RoleList listByProject(String projectId, String pageToken, Integer pageSize, String projectErn);
}
