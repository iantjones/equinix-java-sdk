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

package com.eqixiac.equinix;

import com.eqixiac.equinix.core.auth.EquinixCredentials;
import com.eqixiac.equinix.core.auth.EquinixCredentialsProvider;
import com.eqixiac.equinix.core.auth.EquinixStaticCredentialsProvider;
import com.eqixiac.equinix.core.auth.BearerTokenProvider;
import com.eqixiac.equinix.iam.client.IAMAccessPolicies;
import com.eqixiac.equinix.iam.client.implementation.IAMConfigImpl;
import com.eqixiac.equinix.iam.client.IAMEffectivePermissions;
import com.eqixiac.equinix.iam.client.IAMPermissionSets;
import com.eqixiac.equinix.iam.client.IAMPolicyMasks;
import com.eqixiac.equinix.iam.client.IAMPrincipalPolicies;
import com.eqixiac.equinix.iam.client.IAMResourceTypes;
import com.eqixiac.equinix.iam.client.IAMRoleAssignments;
import com.eqixiac.equinix.iam.client.IAMRoles;
import com.eqixiac.equinix.iam.client.implementation.IAMAccessPoliciesImpl;
import com.eqixiac.equinix.iam.client.implementation.IAMConfigImpl;
import com.eqixiac.equinix.iam.client.implementation.IAMEffectivePermissionsImpl;
import com.eqixiac.equinix.iam.client.implementation.IAMPermissionSetsImpl;
import com.eqixiac.equinix.iam.client.implementation.IAMPolicyMasksImpl;
import com.eqixiac.equinix.iam.client.implementation.IAMPrincipalPoliciesImpl;
import com.eqixiac.equinix.iam.client.implementation.IAMResourceTypesImpl;
import com.eqixiac.equinix.iam.client.implementation.IAMRoleAssignmentsImpl;
import com.eqixiac.equinix.iam.client.implementation.IAMRolesImpl;

/**
 * The primary entry point for accessing the Equinix Identity and Access Management (IAM) API
 * (Access v1) - Equinix's attribute-based access control (ABAC) and role-based access control
 * (RBAC) plane.
 *
 * <p>IAM lets you describe <em>who</em> can do <em>what</em> across Equinix services through
 * several cooperating resource families:</p>
 *
 * <ul>
 *   <li>{@link #roles()} / {@link #roleAssignments()} - the RBAC layer: pre-defined roles and
 *       the assignment of a role to a principal within a scope.</li>
 *   <li>{@link #accessPolicies()} - project-scoped ABAC access policies, including their
 *       {@code grants} (which principals/groups/projects the policy is granted to).</li>
 *   <li>{@link #permissionSets()} - named, reusable permission sets within a project.</li>
 *   <li>{@link #principalPolicies()} - per-principal policies within a governance domain.</li>
 *   <li>{@link #policyMasks()} - project-scoped masks controlling which managed policies and
 *       permission sets are available.</li>
 *   <li>{@link #effectivePermissions()} - the computed effective permissions a principal holds
 *       within a project for a service.</li>
 *   <li>{@link #resourceTypes()} - discovery of resource types, actions, action sets, resource
 *       type actions and a service's policy schema.</li>
 * </ul>
 *
 * <p>Each accessor uses lazy initialization - the internal client is created on first access and
 * reused for subsequent calls.</p>
 *
 * <h3>Service host</h3>
 * <p>IAM requests go to the Access service's own host, {@code https://access.eqix.equinix.com},
 * declared as the {@code hostName} of the IAM functional area in {@code apiParams_IAM.json}. The
 * unified gateway {@code https://api.equinix.com} answers 404 for these paths. The OAuth
 * client-credentials login still goes to {@code api.equinix.com}, and the resulting bearer token is
 * sent to the Access host. Override the host with
 * {@code getEquinixClient().setDomainEndPoint("IAM", ...)}.</p>
 *
 * <h3>Tokens for project-scoped endpoints</h3>
 * <p>Organization-level calls such as {@link #roles()}{@code .list()} work with the
 * client-credentials token. The project-scoped endpoints ({@code /v1/projects/{projectId}/...}:
 * access policies, permission sets, principal policies, policy masks, effective permissions,
 * project roles and resource types) accept only an STS-issued <b>access-policy</b> token (live,
 * 2026-11-11). Build an {@code IAM} for them with {@link #IAM(BearerTokenProvider)}.</p>
 *
 * <h3>Project identifiers</h3>
 * <p>Two formats exist. IAM paths take the spec's {@code ProjectId}, {@code project:<id>} (for
 * example {@code project:abc-123}); the bare Resource Manager project UUID that {@link Projects}
 * returns fails the service's project-id format check. {@link com.eqixiac.equinix.iam.client.ProjectIds#of(String)}
 * converts either form to the {@code project:} form.</p>
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * BasicEquinixCredentials credentials = new BasicEquinixCredentials("clientId", "clientSecret");
 * IAM iam = new IAM(credentials);
 * RoleList roles = iam.roles().list();
 *
 * // Project-scoped: an STS access-policy token, refreshed by the supplier.
 * IAM projectIam = new IAM(() -> myTokenCache.currentAccessPolicyToken());
 * AccessPolicy policy = projectIam.accessPolicies()
 *     .getByUuid(ProjectIds.of(projectUuid), "accesspolicy:my-policy");
 * }</pre>
 *
 * @author ianjones
 * @see com.eqixiac.equinix.core.auth.BasicEquinixCredentials
 */
public final class IAM extends EquinixClient {

    private IAMRoles roles;

    private IAMRoleAssignments roleAssignments;

    private IAMAccessPolicies accessPolicies;

    private IAMPermissionSets permissionSets;

    private IAMPrincipalPolicies principalPolicies;

    private IAMPolicyMasks policyMasks;

    private IAMEffectivePermissions effectivePermissions;

    private IAMResourceTypes resourceTypes;

    final private IAMConfigImpl iamConfig;

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
        this(new EquinixStaticCredentialsProvider(equinixCredentials), isSandBoxed);
    }

    /**
     * Creates a new IAM client whose credentials are resolved through the given provider.
     * Authentication occurs automatically on the first API call.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     */
    public IAM(EquinixCredentialsProvider credentialsProvider) {
        this(credentialsProvider, false);
    }

    /**
     * Creates a new IAM client over a custom credentials provider, with optional sandbox mode.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     * @param isSandBoxed {@code true} to use the sandbox environment for testing; {@code false} for production
     */
    public IAM(EquinixCredentialsProvider credentialsProvider, boolean isSandBoxed) {
        super(credentialsProvider, isSandBoxed);

        String paramFile = "json/apiParams_IAM.json";
        equinixClient.appendApiParams(paramFile);

        this.iamConfig = new IAMConfigImpl(equinixClient);
    }

    /**
     * Creates a new IAM client with explicit {@link EquinixConfig} options.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     * @param config the construction-time options
     */
    public IAM(EquinixCredentials equinixCredentials, EquinixConfig config) {
        this(new EquinixStaticCredentialsProvider(equinixCredentials), config);
    }

    /**
     * Creates a new IAM client over a custom credentials provider with explicit
     * {@link EquinixConfig} options.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     * @param config the construction-time options
     */
    public IAM(EquinixCredentialsProvider credentialsProvider, EquinixConfig config) {
        super(credentialsProvider, config);

        String paramFile = "json/apiParams_IAM.json";
        equinixClient.appendApiParams(paramFile);

        this.iamConfig = new IAMConfigImpl(equinixClient);
    }

    /**
     * Creates an IAM client that sends caller-supplied bearer tokens instead of logging in with
     * client credentials. Use it for the project-scoped endpoints, which need an STS access-policy
     * token ({@code BearerTokenProvider.of(token)}, or {@code BearerTokenProvider.from(...)} to
     * refresh it). No Equinix client id or secret is used.
     *
     * @param tokenProvider supplies the access-policy token
     */
    public IAM(BearerTokenProvider tokenProvider) {
        this(tokenProvider, EquinixConfig.defaults());
    }

    /**
     * Creates an IAM client that sends caller-supplied bearer tokens, with explicit
     * {@link EquinixConfig} options.
     *
     * @param tokenProvider supplies the access-policy token
     * @param config the construction-time options
     */
    public IAM(BearerTokenProvider tokenProvider, EquinixConfig config) {
        super(tokenProvider, config);

        equinixClient.appendApiParams("json/apiParams_IAM.json");

        this.iamConfig = new IAMConfigImpl(equinixClient);
    }

    /**
     * Package-private constructor for {@link Equinix} sessions: builds this domain client over a
     * shared core client (one OAuth token + connection pool across domains).
     */
    IAM(com.eqixiac.equinix.core.client.EquinixClient sharedCore) {
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
     * Returns the client for IAM role assignments - associating a principal with a role in a scope
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
     * Returns the client for IAM policy-authoring discovery - resource types, actions, action sets,
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
