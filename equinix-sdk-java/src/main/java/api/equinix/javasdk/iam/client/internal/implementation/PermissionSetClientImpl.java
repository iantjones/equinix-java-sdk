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
import api.equinix.javasdk.iam.client.internal.PermissionSetClient;
import api.equinix.javasdk.iam.model.PermissionSet;
import api.equinix.javasdk.iam.model.json.PermissionSetJson;
import api.equinix.javasdk.iam.model.json.PermissionSetList;
import api.equinix.javasdk.iam.model.json.creators.CreatePermissionSetRequest;
import api.equinix.javasdk.iam.model.json.creators.LastRevRequest;
import api.equinix.javasdk.iam.model.json.creators.UpdatePermissionSetRequest;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for project-scoped IAM permission sets. Permission set responses
 * are read-only and list operations use opaque-token ({@code nextPageToken}) pagination, so the
 * deserialized JSON models are returned directly.
 */
public class PermissionSetClientImpl extends ClientBase implements PermissionSetClient {

    public PermissionSetClientImpl(IAMConfigImpl configClient) {
        super(configClient, "IAM", "PermissionSets");
    }

    @Override
    public PermissionSetList list(String projectId, String pageToken, Integer pageSize) {
        Map<String, List<String>> queryParams = IamQueryParams.builder()
                .add("pageToken", pageToken)
                .add("pageSize", pageSize)
                .build();
        return getAs("ListPermissionSets", Map.of("projectId", projectId), queryParams, PermissionSetList.class);
    }

    @Override
    public PermissionSet create(String projectId, CreatePermissionSetRequest request) {
        return postForType("CreatePermissionSet", Map.of("projectId", projectId), request,
                new TypeReference<PermissionSetJson>() {
                });
    }

    @Override
    public PermissionSet getByUuid(String projectId, String permissionSetId) {
        return getAs("GetPermissionSet", Map.of("projectId", projectId, "permissionSetId", permissionSetId),
                null, PermissionSetJson.class);
    }

    @Override
    public PermissionSet update(String projectId, String permissionSetId, UpdatePermissionSetRequest request) {
        return postForType("UpdatePermissionSet", Map.of("projectId", projectId, "permissionSetId", permissionSetId),
                request, new TypeReference<PermissionSetJson>() {
                });
    }

    @Override
    public Boolean delete(String projectId, String permissionSetId, String lastRev) {
        // The spec requires the LastRevBody {lastRev} on the DELETE for optimistic concurrency.
        // The body is serialized here; the core HTTP layer must enclose entities on DELETE for it
        // to reach the wire (see RequestFactory).
        return booleanOp("DeletePermissionSet", RequestType.SINGLE,
                Map.of("projectId", projectId, "permissionSetId", permissionSetId), null, new LastRevRequest(lastRev));
    }
}
