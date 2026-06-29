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

package api.equinix.javasdk.iam.client.internal;

import api.equinix.javasdk.iam.model.EffectivePermissions;

/**
 * Internal client for the IAM effective-permissions resolution:
 * {@code GET /v1/projects/{projectId}/effectivePermissions} (operationId
 * {@code getEffectivePermissions}).
 */
public interface EffectivePermissionClient {

    /**
     * Resolves the effective permissions for the given project and service
     * (operationId {@code getEffectivePermissions}).
     *
     * @param projectId the project identifier (path parameter)
     * @param serviceId the service to resolve permissions for (required query parameter)
     * @param projectErn the project ERN to scope resolution to, or {@code null} (optional query parameter)
     * @return the resolved effective permissions
     */
    EffectivePermissions getEffectivePermissions(String projectId, String serviceId, String projectErn);
}
