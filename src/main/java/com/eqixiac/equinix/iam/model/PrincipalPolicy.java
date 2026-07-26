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

package com.eqixiac.equinix.iam.model;

import java.util.List;
import java.util.Map;

/**
 * An access policy attached directly to a principal (user), as returned by the IAM
 * principal-policy operations.
 *
 * <p>This is a read-only response view (spec schema {@code PrincipalPolicy}).</p>
 */
public interface PrincipalPolicy {

    /**
     * @return the user principal this policy is attached to
     */
    String getUserPrincipal();

    /**
     * @return the description of the policy (may be {@code null})
     */
    String getDescription();

    /**
     * @return the user-controlled tags on the policy
     */
    Map<String, String> getTags();

    /**
     * @return the permission entries (polymorphic {@code UserRectSet} members), each as a lossless
     *         {@link PolicyExpression}
     */
    List<PolicyExpression> getPermissions();

    /**
     * @return the opaque revision of the policy (used as {@code lastRev})
     */
    String getRev();

    /**
     * @return {@code true} when the policy is disabled, otherwise {@code null}
     */
    Boolean getDisabledPolicy();

    /**
     * @return the principal that created the policy
     */
    String getCreatedBy();

    /**
     * @return the creation timestamp
     */
    String getCreatedAt();

    /**
     * @return the principal that last updated the policy
     */
    String getUpdatedBy();

    /**
     * @return the last-updated timestamp
     */
    String getUpdatedAt();

    /**
     * @return the approval timestamp (may be {@code null})
     */
    String getApprovedAt();

    /**
     * @return the principal that approved the policy (may be {@code null})
     */
    String getApprovedBy();
}
