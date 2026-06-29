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

package api.equinix.javasdk.iam.client.implementation;

import api.equinix.javasdk.IAM;
import api.equinix.javasdk.iam.client.IAMRoleAssignments;
import api.equinix.javasdk.iam.client.internal.RoleAssignmentClient;
import api.equinix.javasdk.iam.model.RoleAssignment;
import api.equinix.javasdk.iam.model.json.RoleAssignmentList;
import api.equinix.javasdk.iam.model.json.creators.CreateRoleAssignmentRequest;

public class IAMRoleAssignmentsImpl implements IAMRoleAssignments {

    private final IAM serviceManager;

    private final RoleAssignmentClient roleAssignmentClient;

    public IAMRoleAssignmentsImpl(RoleAssignmentClient roleAssignmentClient, IAM serviceManager) {
        this.serviceManager = serviceManager;
        this.roleAssignmentClient = roleAssignmentClient;
    }

    public RoleAssignmentList list(String assignmentScopeId, String assignmentScopeType) {
        return this.roleAssignmentClient.list(assignmentScopeId, assignmentScopeType, null, null);
    }

    public RoleAssignmentList list(String assignmentScopeId, String assignmentScopeType, String pageToken, Integer pageSize) {
        return this.roleAssignmentClient.list(assignmentScopeId, assignmentScopeType, pageToken, pageSize);
    }

    public RoleAssignment create(CreateRoleAssignmentRequest request) {
        return this.roleAssignmentClient.create(request);
    }

    public RoleAssignment getByUuid(String roleAssignmentId) {
        return this.roleAssignmentClient.getByUuid(roleAssignmentId);
    }

    public Boolean delete(String roleAssignmentId) {
        return this.roleAssignmentClient.delete(roleAssignmentId);
    }
}
