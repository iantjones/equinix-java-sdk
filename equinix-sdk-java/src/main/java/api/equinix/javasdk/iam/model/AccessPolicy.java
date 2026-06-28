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

import java.util.List;
import java.util.Map;

/**
 * A project-scoped IAM access policy — a set of permissions that may be granted to principals,
 * groups or other projects. Returned by the access policy operations
 * ({@code listAccessPolicies}, {@code createAccessPolicy}, {@code getAccessPolicy},
 * {@code updateAccessPolicy}, {@code enableAccessPolicy}, {@code disableAccessPolicy}).
 *
 * <p>This is a read-only response view (spec schema {@code AccessPolicy}). The polymorphic
 * permission set entries are exposed as raw deserialized JSON via {@link #getPermissions()}.</p>
 */
public interface AccessPolicy {

    /**
     * @return the access policy identifier (e.g. {@code accesspolicy:my-policy} or a managed policy id)
     */
    String getAccessPolicyId();

    /**
     * @return the Equinix Resource Name (ERN) of the access policy
     */
    String getErn();

    /**
     * @return the description of the access policy (may be {@code null})
     */
    String getDescription();

    /**
     * @return the user-controlled tags on the policy
     */
    Map<String, String> getTags();

    /**
     * @return the policy's permission set entries, as raw deserialized JSON (polymorphic
     *         {@code UserRectSet} entries)
     */
    List<Object> getPermissions();

    /**
     * @return the policy's {@code intersect} permission entries, as raw deserialized JSON
     */
    List<Object> getIntersect();

    /**
     * @return the policy's {@code subtract} permission entries, as raw deserialized JSON
     */
    List<Object> getSubtract();

    /**
     * @return the opaque revision of the policy (used as {@code lastRev} for concurrency control)
     */
    String getRev();

    /**
     * @return {@code true} when the policy is disabled, otherwise {@code null}
     */
    Boolean getDisabledPolicy();

    /**
     * @return {@code true} when the policy is managed, otherwise {@code null}
     */
    Boolean getManaged();

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
}
