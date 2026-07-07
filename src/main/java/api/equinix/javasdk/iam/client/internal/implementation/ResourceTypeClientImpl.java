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
import api.equinix.javasdk.core.http.request.QueryParamBuilder;
import api.equinix.javasdk.iam.client.internal.ResourceTypeClient;
import api.equinix.javasdk.iam.model.ServicePolicySchema;
import api.equinix.javasdk.iam.model.json.ActionList;
import api.equinix.javasdk.iam.model.json.ResourceTypeActionPage;
import api.equinix.javasdk.iam.model.json.ResourceTypeList;
import api.equinix.javasdk.iam.model.json.ServiceActionSetList;
import api.equinix.javasdk.iam.model.json.ServicePolicySchemaJson;

import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the IAM access-model discovery operations. The list-shaped
 * responses are read-only and use opaque-token ({@code nextPageToken}) pagination rather than the
 * core's offset-based paging, so the deserialized list models are returned directly; callers fetch
 * subsequent pages by passing {@code getNextPageToken()} back as the {@code pageToken} argument.
 */
public class ResourceTypeClientImpl extends ClientBase implements ResourceTypeClient {

    public ResourceTypeClientImpl(IAMConfigImpl configClient) {
        super(configClient, "IAM", "ResourceTypes");
    }

    @Override
    public ResourceTypeList listResourceTypes(String projectId, String serviceId, String pageToken, Integer pageSize,
                                              String projectErn) {
        Map<String, List<String>> queryParams = QueryParamBuilder.builder()
                .add("serviceId", serviceId)
                .add("pageToken", pageToken)
                .add("pageSize", pageSize)
                .add("projectErn", projectErn)
                .build();
        return getAs("ListResourceTypes", Map.of("projectId", projectId), queryParams, ResourceTypeList.class);
    }

    @Override
    public ActionList listActions(String projectId, String serviceId, String pageToken, Integer pageSize,
                                  String projectErn) {
        Map<String, List<String>> queryParams = QueryParamBuilder.builder()
                .add("serviceId", serviceId)
                .add("pageToken", pageToken)
                .add("pageSize", pageSize)
                .add("projectErn", projectErn)
                .build();
        return getAs("ListActions", Map.of("projectId", projectId), queryParams, ActionList.class);
    }

    @Override
    public ServiceActionSetList listActionSets(String projectId, String serviceId, String pageToken, Integer pageSize,
                                               String projectErn) {
        Map<String, List<String>> queryParams = QueryParamBuilder.builder()
                .add("serviceId", serviceId)
                .add("pageToken", pageToken)
                .add("pageSize", pageSize)
                .add("projectErn", projectErn)
                .build();
        return getAs("ListActionSets", Map.of("projectId", projectId), queryParams, ServiceActionSetList.class);
    }

    @Override
    public ResourceTypeActionPage listResourceTypeActions(String projectId, String serviceId, String resourceType,
                                                          String resourceTypeServiceId, String lastAction,
                                                          Integer pageSize, String projectErn) {
        Map<String, List<String>> queryParams = QueryParamBuilder.builder()
                .add("serviceId", serviceId)
                .add("resourceType", resourceType)
                .add("resourceTypeServiceId", resourceTypeServiceId)
                .add("lastAction", lastAction)
                .add("pageSize", pageSize)
                .add("projectErn", projectErn)
                .build();
        return getAs("PageResourceTypeActions", Map.of("projectId", projectId), queryParams, ResourceTypeActionPage.class);
    }

    @Override
    public ServicePolicySchema getServicePolicySchema(String projectId, String serviceId) {
        Map<String, List<String>> queryParams = QueryParamBuilder.builder()
                .add("serviceId", serviceId)
                .build();
        return getAs("GetServicePolicySchema", Map.of("projectId", projectId), queryParams, ServicePolicySchemaJson.class);
    }
}
