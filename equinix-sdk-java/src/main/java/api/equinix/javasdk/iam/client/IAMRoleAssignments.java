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

import api.equinix.javasdk.iam.model.RoleAssignment;
import api.equinix.javasdk.iam.model.json.RoleAssignmentList;
import api.equinix.javasdk.iam.model.json.creators.CreateRoleAssignmentRequest;

/**
 * Client interface for IAM role assignments — associating a principal with a role within a scope.
 *
 * <p>Supports listing assignments for a scope
 * ({@code GET /v1/roleAssignments}, operationId {@code listRoleAssignments}), creating an
 * assignment ({@code POST /v1/roleAssignments}, operationId {@code createRoleAssignment}),
 * retrieving one by id ({@code GET /v1/roleAssignments/{roleAssignmentId}}, operationId
 * {@code getRoleAssignment}) and deleting one
 * ({@code DELETE /v1/roleAssignments/{roleAssignmentId}}, operationId {@code deleteRoleAssignment}).</p>
 */
public interface IAMRoleAssignments {

    /**
     * Lists the role assignments for a given scope (first page).
     *
     * @param assignmentScopeId the id of the scope object (required)
     * @param assignmentScopeType the type of the scope object, e.g. {@code PROJECT} (required)
     * @return the first page of role assignments
     */
    RoleAssignmentList list(String assignmentScopeId, String assignmentScopeType);

    /**
     * Lists the role assignments for a given scope, controlling pagination.
     *
     * @param assignmentScopeId the id of the scope object (required)
     * @param assignmentScopeType the type of the scope object, e.g. {@code PROJECT} (required)
     * @param pageToken the opaque page token from a prior response, or {@code null} for the first page
     * @param pageSize the maximum number of results per page, or {@code null} for the server default
     * @return the requested page of role assignments
     */
    RoleAssignmentList list(String assignmentScopeId, String assignmentScopeType, String pageToken, Integer pageSize);

    /**
     * Creates a new role assignment.
     *
     * @param request the role assignment to create (principal, role and scope)
     * @return the created role assignment
     */
    RoleAssignment create(CreateRoleAssignmentRequest request);

    /**
     * Retrieves a single role assignment by its identifier.
     *
     * @param roleAssignmentId the role assignment identifier
     * @return the matching role assignment
     */
    RoleAssignment getByUuid(String roleAssignmentId);

    /**
     * Deletes a role assignment by its identifier.
     *
     * @param roleAssignmentId the role assignment identifier
     * @return {@code true} if the deletion request was accepted
     */
    Boolean delete(String roleAssignmentId);
}
