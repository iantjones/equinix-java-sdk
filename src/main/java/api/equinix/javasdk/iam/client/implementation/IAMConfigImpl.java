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

import api.equinix.javasdk.core.client.Config;
import api.equinix.javasdk.core.client.EquinixClient;
import api.equinix.javasdk.iam.client.IAMConfig;
import api.equinix.javasdk.iam.client.internal.implementation.AccessPolicyClientImpl;
import api.equinix.javasdk.iam.client.internal.implementation.EffectivePermissionClientImpl;
import api.equinix.javasdk.iam.client.internal.implementation.PermissionSetClientImpl;
import api.equinix.javasdk.iam.client.internal.implementation.PolicyMaskClientImpl;
import api.equinix.javasdk.iam.client.internal.implementation.PrincipalPolicyClientImpl;
import api.equinix.javasdk.iam.client.internal.implementation.ResourceTypeClientImpl;
import api.equinix.javasdk.iam.client.internal.implementation.RoleAssignmentClientImpl;
import api.equinix.javasdk.iam.client.internal.implementation.RoleClientImpl;
import lombok.Getter;

@Getter
public class IAMConfigImpl extends Config implements IAMConfig {

    private final RoleClientImpl roleClient;

    private final RoleAssignmentClientImpl roleAssignmentClient;

    private final AccessPolicyClientImpl accessPolicyClient;

    private final PermissionSetClientImpl permissionSetClient;

    private final PrincipalPolicyClientImpl principalPolicyClient;

    private final PolicyMaskClientImpl policyMaskClient;

    private final EffectivePermissionClientImpl effectivePermissionClient;

    private final ResourceTypeClientImpl resourceTypeClient;

    public IAMConfigImpl(EquinixClient equinixClient) {
        super(equinixClient);
        this.roleClient = new RoleClientImpl(this);
        this.roleAssignmentClient = new RoleAssignmentClientImpl(this);
        this.accessPolicyClient = new AccessPolicyClientImpl(this);
        this.permissionSetClient = new PermissionSetClientImpl(this);
        this.principalPolicyClient = new PrincipalPolicyClientImpl(this);
        this.policyMaskClient = new PolicyMaskClientImpl(this);
        this.effectivePermissionClient = new EffectivePermissionClientImpl(this);
        this.resourceTypeClient = new ResourceTypeClientImpl(this);
    }
}
