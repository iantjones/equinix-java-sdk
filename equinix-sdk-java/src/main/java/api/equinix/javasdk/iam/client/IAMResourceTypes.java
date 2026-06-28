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

package api.equinix.javasdk.iam.client;

import api.equinix.javasdk.iam.model.ServicePolicySchema;
import api.equinix.javasdk.iam.model.json.ActionList;
import api.equinix.javasdk.iam.model.json.ResourceTypeActionPage;
import api.equinix.javasdk.iam.model.json.ResourceTypeList;
import api.equinix.javasdk.iam.model.json.ServiceActionSetList;

/**
 * Client interface for discovering the IAM access model: resource types, actions, action sets,
 * resource-type-to-action mappings, and per-service policy schemas. All operations are
 * project-scoped reads.
 *
 * <p>List results are opaque-token paginated: each response carries a {@code nextPageToken};
 * pass it back as the {@code pageToken} argument to fetch the next page.</p>
 */
public interface IAMResourceTypes {

    /**
     * Lists the registered resource types for a project, first page
     * (operationId {@code listResourceTypes}).
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @return the first page of resource types
     */
    ResourceTypeList listResourceTypes(String projectId);

    /**
     * Lists the registered resource types for a project, controlling pagination
     * (operationId {@code listResourceTypes}).
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @param pageToken the opaque page token from a prior response, or {@code null} for the first page
     * @param pageSize the maximum number of results per page, or {@code null} for the server default
     * @return the requested page of resource types
     */
    ResourceTypeList listResourceTypes(String projectId, String pageToken, Integer pageSize);

    /**
     * Lists the available actions for a project, first page (operationId {@code listActions}).
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @return the first page of actions
     */
    ActionList listActions(String projectId);

    /**
     * Lists the available actions for a project, controlling pagination
     * (operationId {@code listActions}).
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @param pageToken the opaque page token from a prior response, or {@code null} for the first page
     * @param pageSize the maximum number of results per page, or {@code null} for the server default
     * @return the requested page of actions
     */
    ActionList listActions(String projectId, String pageToken, Integer pageSize);

    /**
     * Lists the action sets for a service within a project, first page
     * (operationId {@code listActionSets}).
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @param serviceId the service to list action sets for
     * @return the first page of action sets
     */
    ServiceActionSetList listActionSets(String projectId, String serviceId);

    /**
     * Lists the action sets for a service within a project, controlling pagination
     * (operationId {@code listActionSets}).
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @param serviceId the service to list action sets for
     * @param pageToken the opaque page token from a prior response, or {@code null} for the first page
     * @param pageSize the maximum number of results per page, or {@code null} for the server default
     * @return the requested page of action sets
     */
    ServiceActionSetList listActionSets(String projectId, String serviceId, String pageToken, Integer pageSize);

    /**
     * Pages the resource-type-to-action mappings for a project, first page
     * (operationId {@code pageResourceTypeActions}).
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @return the first page of resource-type actions
     */
    ResourceTypeActionPage pageResourceTypeActions(String projectId);

    /**
     * Pages the resource-type-to-action mappings for a project, controlling pagination
     * (operationId {@code pageResourceTypeActions}).
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @param pageToken the opaque page token from a prior response, or {@code null} for the first page
     * @param pageSize the maximum number of results per page, or {@code null} for the server default
     * @return the requested page of resource-type actions
     */
    ResourceTypeActionPage pageResourceTypeActions(String projectId, String pageToken, Integer pageSize);

    /**
     * Gets the policy schema for a service within a project
     * (operationId {@code getServicePolicySchema}).
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @param serviceId the service to fetch the policy schema for
     * @return the service policy schema
     */
    ServicePolicySchema getServicePolicySchema(String projectId, String serviceId);
}
