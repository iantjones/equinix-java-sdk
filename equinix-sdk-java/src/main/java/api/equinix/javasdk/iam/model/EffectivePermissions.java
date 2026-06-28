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
     * @return the effective permission entries, as raw deserialized JSON
     *         (each entry is a complex action/resources/condition object)
     */
    List<Object> getPermissions();
}
