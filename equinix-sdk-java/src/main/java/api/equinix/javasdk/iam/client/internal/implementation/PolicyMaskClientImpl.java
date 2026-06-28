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
import api.equinix.javasdk.iam.client.internal.PolicyMaskClient;
import api.equinix.javasdk.iam.model.PolicyMask;
import api.equinix.javasdk.iam.model.json.PolicyMaskJson;
import api.equinix.javasdk.iam.model.json.PolicyMaskList;
import api.equinix.javasdk.iam.model.json.creators.CreatePolicyMaskRequest;
import api.equinix.javasdk.iam.model.json.creators.LastRevRequest;
import api.equinix.javasdk.iam.model.json.creators.UpdatePolicyMaskRequest;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for project-scoped IAM policy masks. Policy mask responses are
 * read-only and list operations use opaque-token ({@code nextPageToken}) pagination, so the
 * deserialized JSON models are returned directly.
 */
public class PolicyMaskClientImpl extends ClientBase implements PolicyMaskClient {

    public PolicyMaskClientImpl(IAMConfigImpl configClient) {
        super(configClient, "IAM", "PolicyMasks");
    }

    @Override
    public PolicyMaskList list(String projectId, String pageToken, Integer pageSize) {
        Map<String, List<String>> queryParams = IamQueryParams.builder()
                .add("pageToken", pageToken)
                .add("pageSize", pageSize)
                .build();
        return getAs("ListPolicyMasks", Map.of("projectId", projectId), queryParams, PolicyMaskList.class);
    }

    @Override
    public PolicyMask create(String projectId, CreatePolicyMaskRequest request) {
        return postForType("CreatePolicyMask", Map.of("projectId", projectId), request,
                new TypeReference<PolicyMaskJson>() {
                });
    }

    @Override
    public PolicyMask getByUuid(String projectId, String policyMaskId) {
        return getAs("GetPolicyMask", Map.of("projectId", projectId, "policyMaskId", policyMaskId),
                null, PolicyMaskJson.class);
    }

    @Override
    public PolicyMask update(String projectId, String policyMaskId, UpdatePolicyMaskRequest request) {
        return postForType("UpdatePolicyMask", Map.of("projectId", projectId, "policyMaskId", policyMaskId),
                request, new TypeReference<PolicyMaskJson>() {
                });
    }

    @Override
    public Boolean delete(String projectId, String policyMaskId, String lastRev) {
        // The spec requires the LastRevBody {lastRev} on the DELETE for optimistic concurrency.
        // The body is serialized here; the core HTTP layer must enclose entities on DELETE for it
        // to reach the wire (see RequestFactory).
        return booleanOp("DeletePolicyMask", RequestType.SINGLE,
                Map.of("projectId", projectId, "policyMaskId", policyMaskId), null, new LastRevRequest(lastRev));
    }

    @Override
    public PolicyMask enable(String projectId, String policyMaskId, String lastRev) {
        return postForType("EnablePolicyMask", Map.of("projectId", projectId, "policyMaskId", policyMaskId),
                new LastRevRequest(lastRev), new TypeReference<PolicyMaskJson>() {
                });
    }

    @Override
    public PolicyMask disable(String projectId, String policyMaskId, String lastRev) {
        return postForType("DisablePolicyMask", Map.of("projectId", projectId, "policyMaskId", policyMaskId),
                new LastRevRequest(lastRev), new TypeReference<PolicyMaskJson>() {
                });
    }
}
