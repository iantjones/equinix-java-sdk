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
import api.equinix.javasdk.iam.client.internal.RoleClient;
import api.equinix.javasdk.iam.client.internal.IamQueryParams;
import api.equinix.javasdk.iam.model.json.RoleList;

import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the IAM roles catalog. The role responses are read-only and
 * use opaque-token ({@code nextPageToken}) pagination rather than the core's offset-based paging,
 * so the deserialized {@link RoleList} (carrying {@code list} + {@code nextPageToken}) is returned
 * directly; callers fetch subsequent pages by passing {@code getNextPageToken()} back as the
 * {@code pageToken} argument.
 */
public class RoleClientImpl extends ClientBase implements RoleClient {

    public RoleClientImpl(IAMConfigImpl configClient) {
        super(configClient, "IAM", "Roles");
    }

    @Override
    public RoleList listRoles(String pageToken, Integer pageSize, String projectId, String projectErn) {
        Map<String, List<String>> queryParams = IamQueryParams.builder()
                .add("pageToken", pageToken)
                .add("pageSize", pageSize)
                .add("projectId", projectId)
                .add("projectErn", projectErn)
                .build();
        return getAs("ListRoles", null, queryParams, RoleList.class);
    }

    @Override
    public RoleList listRolesByProjectId(String projectId, String pageToken, Integer pageSize, String projectErn) {
        Map<String, List<String>> queryParams = IamQueryParams.builder()
                .add("pageToken", pageToken)
                .add("pageSize", pageSize)
                .add("projectErn", projectErn)
                .build();
        return getAs("ListRolesByProjectId", Map.of("projectId", projectId), queryParams, RoleList.class);
    }
}
