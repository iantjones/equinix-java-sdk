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

package com.eqixiac.equinix.iam.client.implementation;

import com.eqixiac.equinix.IAM;
import com.eqixiac.equinix.iam.client.IAMRoleAssignments;
import com.eqixiac.equinix.iam.client.internal.RoleAssignmentClient;
import com.eqixiac.equinix.iam.model.RoleAssignment;
import com.eqixiac.equinix.iam.model.json.RoleAssignmentList;
import com.eqixiac.equinix.iam.model.json.creators.CreateRoleAssignmentRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IAMRoleAssignmentsImpl implements IAMRoleAssignments {

    private final RoleAssignmentClient roleAssignmentClient;

    private final IAM serviceManager;

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
