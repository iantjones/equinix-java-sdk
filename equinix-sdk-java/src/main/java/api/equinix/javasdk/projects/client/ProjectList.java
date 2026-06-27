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

package api.equinix.javasdk.projects.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.projects.model.Project;
import api.equinix.javasdk.projects.model.json.creators.ProjectOperator;

/**
 * Client interface for managing Equinix projects. Provides methods to list, retrieve,
 * and create projects that serve as organizational containers for grouping related
 * Equinix resources and services.
 */
public interface ProjectList {

    /**
     * Lists all projects for the current account.
     *
     * @return a paginated list of projects
     */
    PaginatedList<Project> list();

    /**
     * Retrieves a specific project by its unique identifier.
     *
     * @param uuid the unique identifier of the project
     * @return the project
     */
    Project getByUuid(String uuid);

    /**
     * Returns a builder for defining and creating a new project.
     *
     * @return a project builder
     */
    ProjectOperator.ProjectBuilder define();
}
