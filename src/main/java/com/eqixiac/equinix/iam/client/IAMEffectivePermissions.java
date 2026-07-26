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

package com.eqixiac.equinix.iam.client;

import com.eqixiac.equinix.iam.model.EffectivePermissions;

/**
 * Client interface for resolving IAM effective (computed) permissions.
 *
 * <p>Resolves the permissions a principal effectively holds within a project for a given service
 * ({@code GET /v1/projects/{projectId}/effectivePermissions}, operationId
 * {@code getEffectivePermissions}).</p>
 */
public interface IAMEffectivePermissions {

    /**
     * Resolves the effective permissions for a project and service
     * (operationId {@code getEffectivePermissions}).
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @param serviceId the service to resolve permissions for
     * @return the resolved effective permissions
     */
    EffectivePermissions get(String projectId, String serviceId);

    /**
     * Resolves the effective permissions for a project and service, optionally scoped by project ERN
     * (operationId {@code getEffectivePermissions}).
     *
     * @param projectId the project identifier (e.g. {@code project:abc-123})
     * @param serviceId the service to resolve permissions for
     * @param projectErn the project ERN to scope resolution to, or {@code null}
     * @return the resolved effective permissions
     */
    EffectivePermissions get(String projectId, String serviceId, String projectErn);
}
