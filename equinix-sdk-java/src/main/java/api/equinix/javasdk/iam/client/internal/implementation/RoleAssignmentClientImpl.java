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

package api.equinix.javasdk.iam.client.internal.implementation;

import api.equinix.javasdk.core.client.ClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.iam.client.implementation.IAMConfigImpl;
import api.equinix.javasdk.iam.client.internal.IamQueryParams;
import api.equinix.javasdk.iam.client.internal.RoleAssignmentClient;
import api.equinix.javasdk.iam.model.RoleAssignment;
import api.equinix.javasdk.iam.model.json.RoleAssignmentJson;
import api.equinix.javasdk.iam.model.json.RoleAssignmentList;
import api.equinix.javasdk.iam.model.json.creators.CreateRoleAssignmentRequest;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for IAM role assignments. Role assignment responses are read-only
 * and the list operation uses opaque-token ({@code nextPageToken}) pagination, so the deserialized
 * JSON models are returned directly.
 */
public class RoleAssignmentClientImpl extends ClientBase implements RoleAssignmentClient {

    public RoleAssignmentClientImpl(IAMConfigImpl configClient) {
        super(configClient, "IAM", "RoleAssignments");
    }

    @Override
    public RoleAssignmentList list(String assignmentScopeId, String assignmentScopeType, String pageToken, Integer pageSize) {
        Map<String, List<String>> queryParams = IamQueryParams.builder()
                .add("assignmentScopeId", assignmentScopeId)
                .add("assignmentScopeType", assignmentScopeType)
                .add("pageToken", pageToken)
                .add("pageSize", pageSize)
                .build();
        return getAs("ListRoleAssignments", null, queryParams, RoleAssignmentList.class);
    }

    @Override
    public RoleAssignment create(CreateRoleAssignmentRequest request) {
        return postForType("CreateRoleAssignment", null, request, new TypeReference<RoleAssignmentJson>() {
        });
    }

    @Override
    public RoleAssignment getByUuid(String roleAssignmentId) {
        return getAs("GetRoleAssignment", Map.of("roleAssignmentId", roleAssignmentId), null, RoleAssignmentJson.class);
    }

    @Override
    public Boolean delete(String roleAssignmentId) {
        return booleanOp("DeleteRoleAssignment", RequestType.SINGLE, Map.of("roleAssignmentId", roleAssignmentId), null, null);
    }
}
