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

package com.eqixiac.equinix.iam.client.internal;

import com.eqixiac.equinix.iam.model.RoleAssignment;
import com.eqixiac.equinix.iam.model.json.RoleAssignmentList;
import com.eqixiac.equinix.iam.model.json.creators.CreateRoleAssignmentRequest;

/**
 * Internal client for IAM role assignments:
 * {@code GET /v1/roleAssignments} (operationId {@code listRoleAssignments}),
 * {@code POST /v1/roleAssignments} (operationId {@code createRoleAssignment}),
 * {@code GET /v1/roleAssignments/{roleAssignmentId}} (operationId {@code getRoleAssignment}) and
 * {@code DELETE /v1/roleAssignments/{roleAssignmentId}} (operationId {@code deleteRoleAssignment}).
 */
public interface RoleAssignmentClient {

    RoleAssignmentList list(String assignmentScopeId, String assignmentScopeType, String pageToken, Integer pageSize);

    RoleAssignment create(CreateRoleAssignmentRequest request);

    RoleAssignment getByUuid(String roleAssignmentId);

    Boolean delete(String roleAssignmentId);
}
