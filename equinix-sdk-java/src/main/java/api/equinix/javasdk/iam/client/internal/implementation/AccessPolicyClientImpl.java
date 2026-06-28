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
import api.equinix.javasdk.iam.client.internal.AccessPolicyClient;
import api.equinix.javasdk.iam.client.internal.IamQueryParams;
import api.equinix.javasdk.iam.model.AccessPolicy;
import api.equinix.javasdk.iam.model.AccessPolicyGrant;
import api.equinix.javasdk.iam.model.json.AccessPolicyGrantJson;
import api.equinix.javasdk.iam.model.json.AccessPolicyGrantList;
import api.equinix.javasdk.iam.model.json.AccessPolicyJson;
import api.equinix.javasdk.iam.model.json.AccessPolicyList;
import api.equinix.javasdk.iam.model.json.creators.AddGrantRequest;
import api.equinix.javasdk.iam.model.json.creators.CreateAccessPolicyRequest;
import api.equinix.javasdk.iam.model.json.creators.LastRevRequest;
import api.equinix.javasdk.iam.model.json.creators.UpdateAccessPolicyRequest;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for project-scoped IAM access policies and their grants. Access
 * policy responses are read-only and list operations use opaque-token ({@code nextPageToken})
 * pagination, so the deserialized JSON models are returned directly.
 */
public class AccessPolicyClientImpl extends ClientBase implements AccessPolicyClient {

    public AccessPolicyClientImpl(IAMConfigImpl configClient) {
        super(configClient, "IAM", "AccessPolicies");
    }

    @Override
    public AccessPolicyList list(String projectId, String pageToken, Integer pageSize) {
        Map<String, List<String>> queryParams = IamQueryParams.builder()
                .add("pageToken", pageToken)
                .add("pageSize", pageSize)
                .build();
        return getAs("ListAccessPolicies", Map.of("projectId", projectId), queryParams, AccessPolicyList.class);
    }

    @Override
    public AccessPolicy create(String projectId, CreateAccessPolicyRequest request) {
        return postForType("CreateAccessPolicy", Map.of("projectId", projectId), request,
                new TypeReference<AccessPolicyJson>() {
                });
    }

    @Override
    public AccessPolicy getByUuid(String projectId, String accessPolicyId) {
        return getAs("GetAccessPolicy", Map.of("projectId", projectId, "accessPolicyId", accessPolicyId),
                null, AccessPolicyJson.class);
    }

    @Override
    public AccessPolicy update(String projectId, String accessPolicyId, UpdateAccessPolicyRequest request) {
        return postForType("UpdateAccessPolicy", Map.of("projectId", projectId, "accessPolicyId", accessPolicyId),
                request, new TypeReference<AccessPolicyJson>() {
                });
    }

    @Override
    public Boolean delete(String projectId, String accessPolicyId) {
        return booleanOp("DeleteAccessPolicy", RequestType.SINGLE,
                Map.of("projectId", projectId, "accessPolicyId", accessPolicyId), null, null);
    }

    @Override
    public AccessPolicy enable(String projectId, String accessPolicyId, String lastRev) {
        return postForType("EnableAccessPolicy", Map.of("projectId", projectId, "accessPolicyId", accessPolicyId),
                new LastRevRequest(lastRev), new TypeReference<AccessPolicyJson>() {
                });
    }

    @Override
    public AccessPolicy disable(String projectId, String accessPolicyId, String lastRev) {
        return postForType("DisableAccessPolicy", Map.of("projectId", projectId, "accessPolicyId", accessPolicyId),
                new LastRevRequest(lastRev), new TypeReference<AccessPolicyJson>() {
                });
    }

    @Override
    public AccessPolicyGrantList listGrants(String projectId, String accessPolicyId, String pageToken, Integer pageSize) {
        Map<String, List<String>> queryParams = IamQueryParams.builder()
                .add("pageToken", pageToken)
                .add("pageSize", pageSize)
                .build();
        return getAs("ListGrants", Map.of("projectId", projectId, "accessPolicyId", accessPolicyId),
                queryParams, AccessPolicyGrantList.class);
    }

    @Override
    public AccessPolicyGrant addGrant(String projectId, String accessPolicyId, AddGrantRequest request) {
        return postForType("AddGrant", Map.of("projectId", projectId, "accessPolicyId", accessPolicyId),
                request, new TypeReference<AccessPolicyGrantJson>() {
                });
    }

    @Override
    public Boolean removeGrant(String projectId, String accessPolicyId, String grantId) {
        return booleanOp("RemoveGrant", RequestType.SINGLE,
                Map.of("projectId", projectId, "accessPolicyId", accessPolicyId, "grantId", grantId), null, null);
    }
}
