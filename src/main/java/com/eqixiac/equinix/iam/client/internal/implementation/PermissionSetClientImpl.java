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

package com.eqixiac.equinix.iam.client.internal.implementation;

import com.eqixiac.equinix.core.client.ClientBase;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.iam.client.implementation.IAMConfigImpl;
import com.eqixiac.equinix.core.http.request.QueryParamBuilder;
import com.eqixiac.equinix.iam.client.internal.PermissionSetClient;
import com.eqixiac.equinix.iam.model.PermissionSet;
import com.eqixiac.equinix.iam.model.json.PermissionSetJson;
import com.eqixiac.equinix.iam.model.json.PermissionSetList;
import com.eqixiac.equinix.iam.model.json.creators.CreatePermissionSetRequest;
import com.eqixiac.equinix.iam.model.json.creators.LastRevRequest;
import com.eqixiac.equinix.iam.model.json.creators.UpdatePermissionSetRequest;
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
        Map<String, List<String>> queryParams = QueryParamBuilder.builder()
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
        // A DELETE that carries a RequestBody is sent as a body-enclosing DELETE by the core
        // request factory (HttpDeleteWithBody), so the lastRev payload reaches the wire.
        return booleanOp("DeletePermissionSet", RequestType.SINGLE,
                Map.of("projectId", projectId, "permissionSetId", permissionSetId), null, new LastRevRequest(lastRev));
    }
}
