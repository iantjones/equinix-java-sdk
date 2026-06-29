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
import api.equinix.javasdk.iam.client.internal.EffectivePermissionClient;
import api.equinix.javasdk.core.http.request.QueryParamBuilder;
import api.equinix.javasdk.iam.model.EffectivePermissions;
import api.equinix.javasdk.iam.model.json.EffectivePermissionsJson;

import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the IAM effective-permissions resolution. The response is a
 * single read-only object, so the deserialized {@link EffectivePermissionsJson} is returned
 * directly via {@link #getEffectivePermissions(String, String, String)} (operationId
 * {@code getEffectivePermissions}).
 */
public class EffectivePermissionClientImpl extends ClientBase implements EffectivePermissionClient {

    public EffectivePermissionClientImpl(IAMConfigImpl configClient) {
        super(configClient, "IAM", "EffectivePermissions");
    }

    @Override
    public EffectivePermissions getEffectivePermissions(String projectId, String serviceId, String projectErn) {
        Map<String, List<String>> queryParams = QueryParamBuilder.builder()
                .add("serviceId", serviceId)
                .add("projectErn", projectErn)
                .build();
        return getAs("GetEffectivePermissions", Map.of("projectId", projectId), queryParams, EffectivePermissionsJson.class);
    }
}
