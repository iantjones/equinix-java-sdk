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
import api.equinix.javasdk.iam.client.implementation.IAMConfigImpl;
import api.equinix.javasdk.iam.client.internal.IamQueryParams;
import api.equinix.javasdk.iam.client.internal.PrincipalPolicyClient;
import api.equinix.javasdk.iam.model.PrincipalPolicy;
import api.equinix.javasdk.iam.model.json.PrincipalPolicyJson;
import api.equinix.javasdk.iam.model.json.PrincipalPolicyList;
import api.equinix.javasdk.iam.model.json.creators.LastRevRequest;
import api.equinix.javasdk.iam.model.json.creators.UpdatePrincipalPolicyRequest;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for project-scoped IAM principal policies. Principal policy
 * responses are read-only and list operations use opaque-token ({@code nextPageToken}) pagination,
 * so the deserialized JSON models are returned directly.
 */
public class PrincipalPolicyClientImpl extends ClientBase implements PrincipalPolicyClient {

    public PrincipalPolicyClientImpl(IAMConfigImpl configClient) {
        super(configClient, "IAM", "PrincipalPolicies");
    }

    @Override
    public PrincipalPolicyList list(String projectId, String pageToken, Integer pageSize) {
        Map<String, List<String>> queryParams = IamQueryParams.builder()
                .add("pageToken", pageToken)
                .add("pageSize", pageSize)
                .build();
        return getAs("ListPrincipalPolicies", Map.of("projectId", projectId), queryParams, PrincipalPolicyList.class);
    }

    @Override
    public PrincipalPolicy getByUuid(String projectId, String userPrincipal) {
        return getAs("GetPrincipalPolicy", Map.of("projectId", projectId, "userPrincipal", userPrincipal),
                null, PrincipalPolicyJson.class);
    }

    @Override
    public PrincipalPolicy update(String projectId, String userPrincipal, UpdatePrincipalPolicyRequest request) {
        return postForType("UpdatePrincipalPolicy", Map.of("projectId", projectId, "userPrincipal", userPrincipal),
                request, new TypeReference<PrincipalPolicyJson>() {
                });
    }

    @Override
    public PrincipalPolicy enable(String projectId, String userPrincipal, String lastRev) {
        return postForType("EnablePrincipalPolicy", Map.of("projectId", projectId, "userPrincipal", userPrincipal),
                new LastRevRequest(lastRev), new TypeReference<PrincipalPolicyJson>() {
                });
    }

    @Override
    public PrincipalPolicy disable(String projectId, String userPrincipal, String lastRev) {
        return postForType("DisablePrincipalPolicy", Map.of("projectId", projectId, "userPrincipal", userPrincipal),
                new LastRevRequest(lastRev), new TypeReference<PrincipalPolicyJson>() {
                });
    }
}
