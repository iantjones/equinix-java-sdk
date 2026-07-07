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
 * <p>Most list results are opaque-token paginated: each response carries a {@code nextPageToken};
 * pass it back as the {@code pageToken} argument to fetch the next page. The
 * {@code listResourceTypeActions} operation is an exception — it uses cursor-based pagination keyed
 * on {@code lastAction} (the {@code action} of the last item received).</p>
 */
public interface IAMResourceTypes {

    /**
     * Lists the registered resource types for a service within a project, first page
     * (operationId {@code listResourceTypes}).
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @param serviceId the fully qualified service id to list resource types for (required)
     * @return the first page of resource types
     */
    ResourceTypeList listResourceTypes(String projectId, String serviceId);

    /**
     * Lists the registered resource types for a service within a project, controlling pagination
     * (operationId {@code listResourceTypes}).
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @param serviceId the fully qualified service id to list resource types for (required)
     * @param pageToken the opaque page token from a prior response, or {@code null} for the first page
     * @param pageSize the maximum number of results per page, or {@code null} for the server default
     * @param projectErn the project ERN (optional, mutually exclusive with {@code projectId}), or {@code null}
     * @return the requested page of resource types
     */
    ResourceTypeList listResourceTypes(String projectId, String serviceId, String pageToken, Integer pageSize,
                                       String projectErn);

    /**
     * Lists the available actions for a service within a project, first page
     * (operationId {@code listActions}).
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @param serviceId the fully qualified service id to list actions for (required)
     * @return the first page of actions
     */
    ActionList listActions(String projectId, String serviceId);

    /**
     * Lists the available actions for a service within a project, controlling pagination
     * (operationId {@code listActions}).
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @param serviceId the fully qualified service id to list actions for (required)
     * @param pageToken the opaque page token from a prior response, or {@code null} for the first page
     * @param pageSize the maximum number of results per page, or {@code null} for the server default
     * @param projectErn the project ERN (optional, mutually exclusive with {@code projectId}), or {@code null}
     * @return the requested page of actions
     */
    ActionList listActions(String projectId, String serviceId, String pageToken, Integer pageSize, String projectErn);

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
     * @param projectErn the project ERN (optional, mutually exclusive with {@code projectId}), or {@code null}
     * @return the requested page of action sets
     */
    ServiceActionSetList listActionSets(String projectId, String serviceId, String pageToken, Integer pageSize,
                                        String projectErn);

    /**
     * Pages the resource-type-to-action mappings for a service and resource type within a project,
     * first page (operationId {@code pageResourceTypeActions}).
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @param serviceId the fully qualified service id owning the resource type (required)
     * @param resourceType the resource type within the service (required)
     * @return the first page of resource-type actions
     */
    ResourceTypeActionPage listResourceTypeActions(String projectId, String serviceId, String resourceType);

    /**
     * Pages the resource-type-to-action mappings for a service and resource type within a project,
     * controlling cursor-based pagination (operationId {@code pageResourceTypeActions}).
     *
     * <p>This operation uses cursor-based pagination keyed on {@code lastAction}: pass the
     * {@code action} of the last item received from the previous page as {@code lastAction} to
     * retrieve the next page.</p>
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @param serviceId the fully qualified service id owning the resource type (required)
     * @param resourceType the resource type within the service (required)
     * @param resourceTypeServiceId the service id of the resource type when different from {@code serviceId}, or {@code null}
     * @param lastAction the id of the last action received (the cursor), or {@code null} for the first page
     * @param pageSize the maximum number of results per page, or {@code null} for the server default
     * @param projectErn the project ERN (optional, mutually exclusive with {@code projectId}), or {@code null}
     * @return the requested page of resource-type actions
     */
    ResourceTypeActionPage listResourceTypeActions(String projectId, String serviceId, String resourceType,
                                                   String resourceTypeServiceId, String lastAction, Integer pageSize,
                                                   String projectErn);

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
