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

package api.equinix.javasdk.projects.client.internal;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.projects.model.Project;
import api.equinix.javasdk.projects.model.json.ProjectJson;

/**
 * Internal client for the read-only Equinix Resource Manager projects endpoint.
 *
 * @param <T> the public model type
 */
public interface ProjectClient<T> extends Pageable<T> {

    /**
     * Retrieves a page of projects, optionally including permissions and inbox-classified projects.
     *
     * @param includePermissions whether to include the user permissions on each project, or {@code null} for the default
     * @param includeInbox whether to include inbox-classified projects, or {@code null} for the default
     * @return a page of projects
     */
    Page<Project, ProjectJson> list(Boolean includePermissions, Boolean includeInbox);
}
