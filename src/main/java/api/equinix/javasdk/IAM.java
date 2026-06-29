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

package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.EquinixCredentials;
import api.equinix.javasdk.core.model.Service;
import api.equinix.javasdk.iam.client.IAMAccessPolicies;
import api.equinix.javasdk.iam.client.IAMConfig;
import api.equinix.javasdk.iam.client.IAMEffectivePermissions;
import api.equinix.javasdk.iam.client.IAMPermissionSets;
import api.equinix.javasdk.iam.client.IAMPolicyMasks;
import api.equinix.javasdk.iam.client.IAMPrincipalPolicies;
import api.equinix.javasdk.iam.client.IAMResourceTypes;
import api.equinix.javasdk.iam.client.IAMRoleAssignments;
import api.equinix.javasdk.iam.client.IAMRoles;
import api.equinix.javasdk.iam.client.implementation.IAMAccessPoliciesImpl;
import api.equinix.javasdk.iam.client.implementation.IAMConfigImpl;
import api.equinix.javasdk.iam.client.implementation.IAMEffectivePermissionsImpl;
import api.equinix.javasdk.iam.client.implementation.IAMPermissionSetsImpl;
import api.equinix.javasdk.iam.client.implementation.IAMPolicyMasksImpl;
import api.equinix.javasdk.iam.client.implementation.IAMPrincipalPoliciesImpl;
import api.equinix.javasdk.iam.client.implementation.IAMResourceTypesImpl;
import api.equinix.javasdk.iam.client.implementation.IAMRoleAssignmentsImpl;
import api.equinix.javasdk.iam.client.implementation.IAMRolesImpl;

/**
 * The primary entry point for accessing the Equinix Identity and Access Management (IAM) API
 * (Access v1) — Equinix's attribute-based access control (ABAC) and role-based access control
 * (RBAC) plane.
 *
 * <p>IAM lets you describe <em>who</em> can do <em>what</em> across Equinix services through
 * several cooperating resource families:</p>
 *
 * <ul>
 *   <li>{@link #roles()} / {@link #roleAssignments()} — the RBAC layer: pre-defined roles and
 *       the assignment of a role to a principal within a scope.</li>
 *   <li>{@link #accessPolicies()} — project-scoped ABAC access policies, including their
 *       {@code grants} (which principals/groups/projects the policy is granted to).</li>
 *   <li>{@link #permissionSets()} — named, reusable permission sets within a project.</li>
 *   <li>{@link #principalPolicies()} — per-principal policies within a governance domain.</li>
 *   <li>{@link #policyMasks()} — project-scoped masks controlling which managed policies and
 *       permission sets are available.</li>
 *   <li>{@link #effectivePermissions()} — the computed effective permissions a principal holds
 *       within a project for a service.</li>
 *   <li>{@link #resourceTypes()} — discovery of resource types, actions, action sets, resource
 *       type actions and a service's policy schema.</li>
 * </ul>
 *
 * <p>Each accessor uses lazy initialization — the internal client is created on first access and
 * reused for subsequent calls.</p>
 *
 * <h3>Service host</h3>
 * <p>The Access v1 OpenAPI specification declares its own service host
 * ({@code https://access.eqix.equinix.com}). This SDK, however, routes every domain — IAM included
 * — through the unified Equinix API gateway ({@code https://api.equinix.com}) configured on the
 * shared {@link EquinixClient}, which fronts the per-service hosts and exposes the same paths. The
 * relative request URIs in {@code apiParams_IAM.json} are therefore resolved against that single
 * gateway host rather than the spec's per-service {@code access.eqix.equinix.com} server. This is
 * the intended SDK-wide design; no per-domain host override is provided.</p>
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * BasicEquinixCredentials credentials = new BasicEquinixCredentials("clientId", "clientSecret");
 * IAM iam = new IAM(credentials);
 *
 * RoleList roles = iam.roles().list();
 * AccessPolicy policy = iam.accessPolicies().getByUuid("project:abc-123", "accesspolicy:my-policy");
 * }</pre>
 *
 * @author ianjones
 * @see api.equinix.javasdk.core.auth.BasicEquinixCredentials
 */
public final class IAM extends EquinixClient implements Service {

    private IAMRoles roles;

    private IAMRoleAssignments roleAssignments;

    private IAMAccessPolicies accessPolicies;

    private IAMPermissionSets permissionSets;

    private IAMPrincipalPolicies principalPolicies;

    private IAMPolicyMasks policyMasks;

    private IAMEffectivePermissions effectivePermissions;

    private IAMResourceTypes resourceTypes;

    final private IAMConfig iamConfig;

    /**
     * Creates a new IAM client using the provided credentials.
     * Authentication occurs automatically on the first API call.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     */
    public IAM(EquinixCredentials equinixCredentials) {
        this(equinixCredentials, false);
    }

    /**
     * Creates a new IAM client with optional sandbox mode.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     * @param isSandBoxed {@code true} to use the sandbox environment for testing; {@code false} for production
     */
    public IAM(EquinixCredentials equinixCredentials, boolean isSandBoxed) {
        super(equinixCredentials, isSandBoxed);

        String paramFile = "json/apiParams_IAM.json";
        equinixClient.appendApiParams(paramFile);

        this.iamConfig = new IAMConfigImpl(equinixClient);
    }

    /**
     * Package-private constructor for {@link Equinix} sessions: builds this domain client over a
     * shared core client (one OAuth token + connection pool across domains).
     */
    IAM(api.equinix.javasdk.core.client.EquinixClient sharedCore) {
        super(sharedCore);
        equinixClient.appendApiParams("json/apiParams_IAM.json");
        this.iamConfig = new IAMConfigImpl(equinixClient);
    }

    /**
     * Returns the client for the IAM roles catalog
     * ({@code GET /v1/roles}, {@code GET /v1/projects/{projectId}/roles}).
     *
     * @return the {@link IAMRoles} client
     */
    public IAMRoles roles() {
        if (this.roles == null) {
            this.roles = new IAMRolesImpl(this.iamConfig.getRoleClient(), this);
        }
        return roles;
    }

    /**
     * Returns the client for IAM role assignments — associating a principal with a role in a scope
     * ({@code /v1/roleAssignments}).
     *
     * @return the {@link IAMRoleAssignments} client
     */
    public IAMRoleAssignments roleAssignments() {
        if (this.roleAssignments == null) {
            this.roleAssignments = new IAMRoleAssignmentsImpl(this.iamConfig.getRoleAssignmentClient(), this);
        }
        return roleAssignments;
    }

    /**
     * Returns the client for project-scoped IAM access policies and their grants
     * ({@code /v1/projects/{projectId}/accessPolicies}).
     *
     * @return the {@link IAMAccessPolicies} client
     */
    public IAMAccessPolicies accessPolicies() {
        if (this.accessPolicies == null) {
            this.accessPolicies = new IAMAccessPoliciesImpl(this.iamConfig.getAccessPolicyClient(), this);
        }
        return accessPolicies;
    }

    /**
     * Returns the client for project-scoped IAM permission sets
     * ({@code /v1/projects/{projectId}/permissionSets}).
     *
     * @return the {@link IAMPermissionSets} client
     */
    public IAMPermissionSets permissionSets() {
        if (this.permissionSets == null) {
            this.permissionSets = new IAMPermissionSetsImpl(this.iamConfig.getPermissionSetClient(), this);
        }
        return permissionSets;
    }

    /**
     * Returns the client for project-scoped IAM principal policies
     * ({@code /v1/projects/{projectId}/principalPolicies}).
     *
     * @return the {@link IAMPrincipalPolicies} client
     */
    public IAMPrincipalPolicies principalPolicies() {
        if (this.principalPolicies == null) {
            this.principalPolicies = new IAMPrincipalPoliciesImpl(this.iamConfig.getPrincipalPolicyClient(), this);
        }
        return principalPolicies;
    }

    /**
     * Returns the client for project-scoped IAM policy masks
     * ({@code /v1/projects/{projectId}/policyMasks}).
     *
     * @return the {@link IAMPolicyMasks} client
     */
    public IAMPolicyMasks policyMasks() {
        if (this.policyMasks == null) {
            this.policyMasks = new IAMPolicyMasksImpl(this.iamConfig.getPolicyMaskClient(), this);
        }
        return policyMasks;
    }

    /**
     * Returns the client for the IAM effective-permissions computation
     * ({@code GET /v1/projects/{projectId}/effectivePermissions}).
     *
     * @return the {@link IAMEffectivePermissions} client
     */
    public IAMEffectivePermissions effectivePermissions() {
        if (this.effectivePermissions == null) {
            this.effectivePermissions = new IAMEffectivePermissionsImpl(this.iamConfig.getEffectivePermissionClient(), this);
        }
        return effectivePermissions;
    }

    /**
     * Returns the client for IAM policy-authoring discovery — resource types, actions, action sets,
     * resource type actions and the service policy schema
     * ({@code /v1/projects/{projectId}/resourceTypes} and friends).
     *
     * @return the {@link IAMResourceTypes} client
     */
    public IAMResourceTypes resourceTypes() {
        if (this.resourceTypes == null) {
            this.resourceTypes = new IAMResourceTypesImpl(this.iamConfig.getResourceTypeClient(), this);
        }
        return resourceTypes;
    }
}
