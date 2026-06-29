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

import api.equinix.javasdk.iam.client.internal.AccessPolicyClient;
import api.equinix.javasdk.iam.client.internal.EffectivePermissionClient;
import api.equinix.javasdk.iam.client.internal.PermissionSetClient;
import api.equinix.javasdk.iam.client.internal.PolicyMaskClient;
import api.equinix.javasdk.iam.client.internal.PrincipalPolicyClient;
import api.equinix.javasdk.iam.client.internal.ResourceTypeClient;
import api.equinix.javasdk.iam.client.internal.RoleAssignmentClient;
import api.equinix.javasdk.iam.client.internal.RoleClient;

public interface IAMConfig {

    RoleClient getRoleClient();

    RoleAssignmentClient getRoleAssignmentClient();

    AccessPolicyClient getAccessPolicyClient();

    PermissionSetClient getPermissionSetClient();

    PrincipalPolicyClient getPrincipalPolicyClient();

    PolicyMaskClient getPolicyMaskClient();

    EffectivePermissionClient getEffectivePermissionClient();

    ResourceTypeClient getResourceTypeClient();
}
