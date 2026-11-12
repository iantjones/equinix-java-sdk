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

package com.eqixiac.equinix.sts.model;

import java.util.Objects;

/**
 * Typed values for the STS token-exchange {@code scope} ({@link
 * com.eqixiac.equinix.sts.model.json.creators.TokenRequest#scope(StsScope)}).
 *
 * <p>The live service (2026-11-11) accepts exactly these forms and rejects anything else with
 * {@code invalid_scope}: "scope must be an access policy ERN or a string of the form
 * roleassignments:&lt;org-id&gt; or projectpolicies:&lt;project-id&gt; or
 * principalpolicy:&lt;project-id&gt;". A free-text value such as {@code roles} is rejected.</p>
 *
 * <p>Project ids are sent bare (the service reports {@code projectpolicies:abc} as
 * {@code project:abc}); a {@code project:} prefix is stripped.</p>
 */
public final class StsScope {

    private final String value;

    private StsScope(String value) {
        this.value = value;
    }

    /**
     * An access-policy ERN given verbatim.
     *
     * @param accessPolicyErn the ERN, starting with {@code ern:}
     * @return the scope
     * @throws IllegalArgumentException if the value does not start with {@code ern:} or has not
     *                                  the seven colon-separated ERN segments
     */
    public static StsScope accessPolicyErn(String accessPolicyErn) {
        Objects.requireNonNull(accessPolicyErn, "accessPolicyErn");
        if (!accessPolicyErn.startsWith("ern:") || accessPolicyErn.split(":", -1).length != 7) {
            throw new IllegalArgumentException("not an ERN (ern:<cloud>:<service>:<region>:<project>:<type>:<id>): "
                    + accessPolicyErn);
        }
        return new StsScope(accessPolicyErn);
    }

    /**
     * The access-policy ERN for a policy in a project:
     * {@code ern:eqix:equinix/access:global:<project-id>:AccessPolicy:<policy-id>}.
     *
     * <p>Shape: the service's ERN validator accepts this form and rejects lower-case
     * {@code accesspolicy} and other service segments (observed 2026-11-11); a successful exchange
     * with it was not verified. Use {@link #accessPolicyErn(String)} for an ERN the console or IAM
     * gives you.</p>
     *
     * @param projectId      the project id, bare or {@code project:<id>}
     * @param accessPolicyId the policy id, bare or {@code accesspolicy:<id>}
     * @return the scope
     */
    public static StsScope accessPolicy(String projectId, String accessPolicyId) {
        return accessPolicyErn("ern:eqix:equinix/access:global:" + bareProject(projectId) + ":AccessPolicy:"
                + strip(accessPolicyId, "accesspolicy:", "accessPolicyId"));
    }

    /**
     * {@code roleassignments:<org-id>}: a token carrying the caller's role assignments in an organization.
     *
     * @param organizationId the organization id
     * @return the scope
     */
    public static StsScope roleAssignments(String organizationId) {
        return new StsScope("roleassignments:" + strip(organizationId, "organization:", "organizationId"));
    }

    /**
     * {@code projectpolicies:<project-id>}: a token carrying the project's policies.
     *
     * @param projectId the project id, bare or {@code project:<id>}
     * @return the scope
     */
    public static StsScope projectPolicies(String projectId) {
        return new StsScope("projectpolicies:" + bareProject(projectId));
    }

    /**
     * {@code principalpolicy:<project-id>}: a token carrying the caller's principal policy in a project.
     *
     * @param projectId the project id, bare or {@code project:<id>}
     * @return the scope
     */
    public static StsScope principalPolicy(String projectId) {
        return new StsScope("principalpolicy:" + bareProject(projectId));
    }

    private static String bareProject(String projectId) {
        return strip(projectId, "project:", "projectId");
    }

    private static String strip(String id, String prefix, String name) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        String trimmed = id.trim();
        String bare = trimmed.startsWith(prefix) ? trimmed.substring(prefix.length()) : trimmed;
        if (bare.isEmpty() || bare.contains(":")) {
            throw new IllegalArgumentException(name + " must be a single id segment: " + id);
        }
        return bare;
    }

    /**
     * Returns the wire value sent as the {@code scope} form field.
     *
     * @return the scope string
     */
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StsScope other && other.value.equals(value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
