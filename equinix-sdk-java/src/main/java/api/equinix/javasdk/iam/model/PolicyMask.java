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

package api.equinix.javasdk.iam.model;

import java.util.Map;

/**
 * A policy mask that constrains the managed policies and permission sets available to a
 * principal, as returned by the IAM policy-mask operations.
 *
 * <p>This is a read-only response view (spec schema {@code PolicyMask}). The
 * {@code managedPolicies}/{@code managedPermissionSets} fields are a {@code oneOf} of the literal
 * string {@code "none"} or an array of ids, and {@code subtract} is a structured object; all three
 * are exposed as lossless {@link PolicyExpression} values preserving whichever form was returned.</p>
 */
public interface PolicyMask {

    /**
     * @return the policy mask identifier
     */
    String getPolicyMaskId();

    /**
     * @return the Equinix Resource Name (ERN) of the policy mask
     */
    String getErn();

    /**
     * @return the description of the policy mask (may be {@code null})
     */
    String getDescription();

    /**
     * @return the user-controlled tags on the policy mask
     */
    Map<String, String> getTags();

    /**
     * @return the managed policies as a lossless {@link PolicyExpression} — either the string
     *         {@code "none"} or an array of {@code managedpolicy:} ids
     */
    PolicyExpression getManagedPolicies();

    /**
     * @return the managed permission sets as a lossless {@link PolicyExpression} — either the string
     *         {@code "none"} or an array of {@code managedset:} ids
     */
    PolicyExpression getManagedPermissionSets();

    /**
     * @return the {@code subtract} object as a lossless {@link PolicyExpression} (carrying nested
     *         {@code managedPolicies}/{@code managedPermissionSets} arrays), or {@code null}
     */
    PolicyExpression getSubtract();

    /**
     * @return the opaque revision of the policy mask (used as {@code lastRev})
     */
    String getRev();

    /**
     * @return {@code true} when the policy mask is disabled, otherwise {@code null}
     */
    Boolean getDisabledPolicy();

    /**
     * @return the principal that created the policy mask
     */
    String getCreatedBy();

    /**
     * @return the creation timestamp
     */
    String getCreatedAt();

    /**
     * @return the principal that last updated the policy mask
     */
    String getUpdatedBy();

    /**
     * @return the last-updated timestamp
     */
    String getUpdatedAt();
}
