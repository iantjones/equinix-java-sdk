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

package com.eqixiac.equinix.iam.model;

import java.util.List;

/**
 * A named set of role actions that can be granted to a principal, as returned by the IAM roles
 * catalog ({@code GET /v1/roles}, operationId {@code listRoles}; and
 * {@code GET /v1/projects/{projectId}/roles}, operationId {@code listRolesByProjectId}).
 *
 * <p>This is a read-only response view (spec schema {@code RoleDetails}).</p>
 */
public interface Role {

    /**
     * @return the immutable identifier of this role (e.g. {@code role:550e8400-...})
     */
    String getRoleId();

    /**
     * @return the human-friendly name of the role
     */
    String getName();

    /**
     * @return the description of the role (may be {@code null})
     */
    String getDescription();

    /**
     * @return the scope types this role may be assigned to (e.g. {@code PROJECT}, {@code ORGANIZATION})
     */
    List<String> getAssignmentScopeTypes();

    /**
     * @return the list of permissions (action + description) assigned to this role
     */
    List<Role.Permission> getPermissions();

    interface Permission {

        /**
         * @return the fully-qualified name of a role action (e.g. {@code iam.role.assignment.read})
         */
        String getAction();

        /**
         * @return the human-readable description of the action
         */
        String getDescription();
    }
}
