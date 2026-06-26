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

package api.equinix.javasdk.projects.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.projects.client.implementation.ProjectsConfigImpl;
import api.equinix.javasdk.projects.client.internal.ProjectClient;
import api.equinix.javasdk.projects.model.Project;
import api.equinix.javasdk.projects.model.json.ProjectJson;
import api.equinix.javasdk.projects.model.json.creators.ProjectCreatorJson;
import api.equinix.javasdk.projects.model.wrappers.ProjectWrapper;

public class ProjectClientImpl extends ResourceClientBase<Project, ProjectJson> implements ProjectClient<Project> {

    public ProjectClientImpl(ProjectsConfigImpl configClient) {
        super(configClient, "Projects", "Projects", ProjectJson.class);
    }

    @Override
    protected Project wrap(ProjectJson json) {
        return new ProjectWrapper(json, this);
    }

    public Page<Project, ProjectJson> list() {
        return listPage("ListProjects");
    }

    public ProjectJson getByUuid(String uuid) {
        return getOne("GetProject", uuid);
    }

    public ProjectJson create(ProjectCreatorJson projectCreatorJson) {
        return postOne("CreateProject", projectCreatorJson);
    }

    public ProjectJson update(String uuid, ProjectCreatorJson projectCreatorJson) {
        return updateOne("UpdateProject", uuid, projectCreatorJson);
    }

    public ProjectJson delete(String uuid) {
        return deleteOne("DeleteProject", uuid);
    }

    public ProjectJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
