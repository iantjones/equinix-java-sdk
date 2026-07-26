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

/**
 * An assignment of a {@link Role} to a principal within a scope, as returned by the IAM role
 * assignment operations ({@code GET /v1/roleAssignments}, operationId {@code listRoleAssignments};
 * {@code POST /v1/roleAssignments}, operationId {@code createRoleAssignment};
 * {@code GET /v1/roleAssignments/{roleAssignmentId}}, operationId {@code getRoleAssignment}).
 *
 * <p>This is a read-only response view (spec schema {@code RoleAssignment}).</p>
 */
public interface RoleAssignment {

    /**
     * @return the immutable identifier of this role assignment (e.g. {@code roleassignment:550e8400-...})
     */
    String getRoleAssignmentId();

    /**
     * @return the identifier of the project that owns this assignment
     */
    String getProjectId();

    /**
     * @return the principal (or group) assigned to the role
     */
    String getPrincipal();

    /**
     * @return the identifier of the role assigned to the principal
     */
    String getRoleId();

    /**
     * @return the name of the role assigned to the principal
     */
    String getRoleName();

    /**
     * @return the scope this assignment applies to
     */
    RoleAssignment.AssignmentScope getAssignmentScope();

    interface AssignmentScope {

        /**
         * @return the identifier of the object this scope refers to
         */
        String getId();

        /**
         * @return the type of the object this scope refers to (e.g. {@code PROJECT}, {@code ORGANIZATION})
         */
        String getType();

        /**
         * @return the name of the object this scope refers to (may be {@code null})
         */
        String getName();

        /**
         * @return the owning project when the scope {@code type} is {@code PORT}, otherwise {@code null}
         */
        Parent getParent();
    }

    interface Parent {

        /**
         * @return the identifier of the project that owns this port
         */
        String getId();

        /**
         * @return the type of the parent object (always {@code PROJECT})
         */
        String getType();

        /**
         * @return the name of the parent project (may be {@code null})
         */
        String getName();
    }
}
