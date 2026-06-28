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

package api.equinix.javasdk.iam.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for creating an IAM role assignment via {@code POST
 * /v1/projects/{projectId}/roleAssignments} (operationId {@code createRoleAssignment}, spec schema
 * {@code CreateRoleAssignmentInput}).
 *
 * <p>The {@code assignmentScope} property is a nested object modelled by the {@link AssignmentScope}
 * inner class.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateRoleAssignmentRequest {

    @JsonProperty("principal")
    private String principal;

    @JsonProperty("roleId")
    private String roleId;

    @JsonProperty("assignmentScope")
    private AssignmentScope assignmentScope;

    /**
     * Sets the principal (required) to which the role is assigned.
     *
     * @param principal the principal
     * @return this request for chaining
     */
    public CreateRoleAssignmentRequest principal(String principal) {
        this.principal = principal;
        return this;
    }

    /**
     * Sets the role identifier (required) being assigned.
     *
     * @param roleId the role id
     * @return this request for chaining
     */
    public CreateRoleAssignmentRequest roleId(String roleId) {
        this.roleId = roleId;
        return this;
    }

    /**
     * Sets the assignment scope that bounds the role assignment.
     *
     * @param assignmentScope the assignment scope
     * @return this request for chaining
     */
    public CreateRoleAssignmentRequest assignmentScope(AssignmentScope assignmentScope) {
        this.assignmentScope = assignmentScope;
        return this;
    }

    public String getPrincipal() {
        return principal;
    }

    public String getRoleId() {
        return roleId;
    }

    public AssignmentScope getAssignmentScope() {
        return assignmentScope;
    }

    /**
     * Nested object describing the scope of a role assignment — the resource (by {@code id} and
     * {@code type}) within which the assigned role applies, optionally with a {@code parent}
     * reference.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AssignmentScope {

        @JsonProperty("id")
        private String id;

        @JsonProperty("type")
        private String type;

        @JsonProperty("parent")
        private Object parent;

        /**
         * Sets the scope resource identifier.
         *
         * @param id the resource id
         * @return this assignment scope for chaining
         */
        public AssignmentScope id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the scope resource type.
         *
         * @param type the resource type
         * @return this assignment scope for chaining
         */
        public AssignmentScope type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the optional parent reference of the scope.
         *
         * @param parent the parent reference
         * @return this assignment scope for chaining
         */
        public AssignmentScope parent(Object parent) {
            this.parent = parent;
            return this;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public Object getParent() {
            return parent;
        }
    }
}
