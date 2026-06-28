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

package api.equinix.javasdk.iam.client.implementation;

import api.equinix.javasdk.IAM;
import api.equinix.javasdk.iam.client.IAMEffectivePermissions;
import api.equinix.javasdk.iam.client.internal.EffectivePermissionClient;
import api.equinix.javasdk.iam.model.EffectivePermissions;

public class IAMEffectivePermissionsImpl implements IAMEffectivePermissions {

    private final IAM serviceManager;

    private final EffectivePermissionClient effectivePermissionClient;

    public IAMEffectivePermissionsImpl(EffectivePermissionClient effectivePermissionClient, IAM serviceManager) {
        this.serviceManager = serviceManager;
        this.effectivePermissionClient = effectivePermissionClient;
    }

    public EffectivePermissions get(String projectId, String serviceId) {
        return this.effectivePermissionClient.getEffectivePermissions(projectId, serviceId, null);
    }

    public EffectivePermissions get(String projectId, String serviceId, String projectErn) {
        return this.effectivePermissionClient.getEffectivePermissions(projectId, serviceId, projectErn);
    }
}
