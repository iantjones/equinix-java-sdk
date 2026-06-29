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
 * A named, reusable set of permissions within a project, as returned by the IAM permission set
 * operations ({@code listPermissionSets}, {@code createPermissionSet}, {@code getPermissionSet},
 * {@code updatePermissionSet}).
 *
 * <p>This is a read-only response view (spec schema {@code PermissionSet}).</p>
 */
public interface PermissionSet {

    /**
     * @return the permission set identifier (e.g. {@code permissionset:my-set} or a managed set id)
     */
    String getPermissionSetId();

    /**
     * @return the Equinix Resource Name (ERN) of the permission set
     */
    String getErn();

    /**
     * @return the description of the permission set (may be {@code null})
     */
    String getDescription();

    /**
     * @return the user-controlled tags on the permission set
     */
    Map<String, String> getTags();

    /**
     * @return the permission set entries (polymorphic {@code UserRectSet} members), each as a
     *         lossless {@link PolicyExpression}
     */
    List<PolicyExpression> getPermissions();

    /**
     * @return the {@code intersect} permission entries, each as a lossless {@link PolicyExpression}
     */
    List<PolicyExpression> getIntersect();

    /**
     * @return the {@code subtract} permission entries, each as a lossless {@link PolicyExpression}
     */
    List<PolicyExpression> getSubtract();

    /**
     * @return the opaque revision of the permission set (used as {@code lastRev})
     */
    String getRev();

    /**
     * @return {@code true} when the permission set is managed, otherwise {@code null}
     */
    Boolean getManaged();

    /**
     * @return the principal that created the permission set
     */
    String getCreatedBy();

    /**
     * @return the creation timestamp
     */
    String getCreatedAt();

    /**
     * @return the principal that last updated the permission set
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
     * @return the principal that approved the permission set (may be {@code null})
     */
    String getApprovedBy();
}
