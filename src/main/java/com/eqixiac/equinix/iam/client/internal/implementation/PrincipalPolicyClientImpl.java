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
import com.eqixiac.equinix.iam.client.implementation.IAMConfigImpl;
import com.eqixiac.equinix.core.http.request.QueryParamBuilder;
import com.eqixiac.equinix.iam.client.internal.PrincipalPolicyClient;
import com.eqixiac.equinix.iam.model.PrincipalPolicy;
import com.eqixiac.equinix.iam.model.json.PrincipalPolicyJson;
import com.eqixiac.equinix.iam.model.json.PrincipalPolicyList;
import com.eqixiac.equinix.iam.model.json.creators.LastRevRequest;
import com.eqixiac.equinix.iam.model.json.creators.UpdatePrincipalPolicyRequest;
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
        Map<String, List<String>> queryParams = QueryParamBuilder.builder()
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
