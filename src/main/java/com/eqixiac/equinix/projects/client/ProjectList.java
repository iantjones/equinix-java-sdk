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

package com.eqixiac.equinix.projects.client;

import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.projects.model.Project;

/**
 * Client interface for the read-only Equinix Resource Manager projects endpoint. Projects are
 * organizational containers associated with a root organization that group related Equinix
 * resources and services.
 */
public interface ProjectList {

    /**
     * Lists the projects of the root organization based on the user's permissions.
     *
     * @return a paginated list of projects
     */
    PaginatedList<Project> list();

    /**
     * Lists the projects of the root organization, optionally including per-project user
     * permissions and inbox-classified projects.
     *
     * @param includePermissions whether to include the user permissions on each project, or {@code null} for the default
     * @param includeInbox whether to include inbox-classified projects, or {@code null} for the default
     * @return a paginated list of projects
     */
    PaginatedList<Project> list(Boolean includePermissions, Boolean includeInbox);
}
