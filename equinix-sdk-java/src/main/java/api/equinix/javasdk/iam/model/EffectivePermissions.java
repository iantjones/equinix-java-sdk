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

/**
 * The effective (resolved) permissions for a principal within a project and service, as
 * returned by the IAM effective-permissions operation.
 *
 * <p>This is a read-only response view (spec schema {@code EffectivePermissions}).</p>
 */
public interface EffectivePermissions {

    /**
     * @return the principal the permissions are resolved for
     */
    String getPrincipalId();

    /**
     * @return the project the permissions are resolved within
     */
    String getProjectId();

    /**
     * @return the service the permissions are resolved for
     */
    String getServiceId();

    /**
     * @return the identifiers of the access policies that contributed to the effective permissions
     */
    List<String> getAccessPolicyIds();

    /**
     * @return the effective permission entries (each describes a set of actions and the
     *         resources/metros/ibxs/cages and condition that scope them)
     */
    List<Permission> getPermissions();

    /**
     * A single effective-permission entry: the caller may perform any of the {@link #getActions()}
     * on the referenced resources (or any <em>except</em> those referenced), optionally gated by a
     * Cedar {@link #getCondition()} expression.
     */
    interface Permission {

        /**
         * @return the set of actions the caller is permitted to perform
         */
        List<String> getActions();

        /**
         * @return the resources (by ERN) the actions are scoped to, or {@code null} when unrestricted
         */
        ResourceSelector getResources();

        /**
         * @return the metro codes the actions are scoped to, or {@code null} when unrestricted
         */
        ResourceSelector getMetroCodes();

        /**
         * @return the IBX ids the actions are scoped to, or {@code null} when unrestricted
         */
        ResourceSelector getIbxIds();

        /**
         * @return the cage ids the actions are scoped to, or {@code null} when unrestricted
         */
        ResourceSelector getCageIds();

        /**
         * @return a Cedar policy-language expression that must be true for the actions to apply,
         *         or {@code null}
         */
        String getCondition();
    }

    /**
     * An {@code anyOf} selector that is either an inclusion set (the listed values) or an exclusion
     * set ({@code {except: [...]}}). Exactly one of {@link #getInclude()} / {@link #getExcept()} is
     * populated for a given selector.
     */
    interface ResourceSelector {

        /**
         * @return the included values, or {@code null} when this selector is an exclusion set
         */
        List<String> getInclude();

        /**
         * @return the excluded values, or {@code null} when this selector is an inclusion set
         */
        List<String> getExcept();
    }
}
