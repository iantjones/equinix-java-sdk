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

package api.equinix.javasdk.projects.client.implementation;

import api.equinix.javasdk.Projects;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.projects.client.ProjectList;
import api.equinix.javasdk.projects.client.internal.ProjectClient;
import api.equinix.javasdk.projects.model.Project;
import api.equinix.javasdk.projects.model.json.ProjectJson;
import api.equinix.javasdk.projects.model.json.creators.ProjectOperator;
import api.equinix.javasdk.projects.model.wrappers.ProjectWrapper;

public class ProjectListImpl implements ProjectList {

    private final Projects serviceManager;

    private final ProjectClient<Project> serviceClient;

    public ProjectListImpl(ProjectClient<Project> serviceClient, Projects serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<Project> list() {
        Page<Project, ProjectJson> responsePage = this.serviceClient.list();
        PaginatedList<Project> projectList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, ProjectWrapper::new);
        return new PaginatedList<>(projectList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public Project getByUuid(String uuid) {
        ProjectJson projectJson = this.serviceClient.getByUuid(uuid);
        return new ProjectWrapper(projectJson, this.serviceClient);
    }

    public ProjectOperator.ProjectBuilder define() {
        return new ProjectOperator(this.serviceClient).create();
    }
}
