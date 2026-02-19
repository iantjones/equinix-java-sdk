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

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.projects.client.implementation.ProjectsConfigImpl;
import api.equinix.javasdk.projects.client.internal.ProjectClient;
import api.equinix.javasdk.projects.model.Project;
import api.equinix.javasdk.projects.model.json.ProjectJson;
import api.equinix.javasdk.projects.model.json.creators.ProjectCreatorJson;
import api.equinix.javasdk.projects.model.wrappers.ProjectWrapper;

import java.util.Map;

public class ProjectClientImpl extends PageableBase implements ProjectClient<Project> {

    public ProjectClientImpl(ProjectsConfigImpl configClient) {
        super(configClient, "Projects", "Projects");
    }

    public Page<Project, ProjectJson> list() {
        EquinixRequest<Project> equinixRequest = this.buildRequest("ListProjects", RequestType.PAGINATED, ProjectJson.getPagedTypeRef());
        EquinixResponse<Project> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public ProjectJson getByUuid(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<ProjectJson> equinixRequest = this.buildRequestWithPathParams("GetProject", RequestType.SINGLE, pParams, ProjectJson.getSingleTypeRef());
        EquinixResponse<ProjectJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public ProjectJson create(ProjectCreatorJson projectCreatorJson) {
        EquinixRequest<ProjectJson> equinixRequest = this.buildRequest("CreateProject", RequestType.SINGLE, ProjectJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, projectCreatorJson);
        EquinixResponse<ProjectJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public ProjectJson update(String uuid, ProjectCreatorJson projectCreatorJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<ProjectJson> equinixRequest = this.buildRequestWithPathParams("UpdateProject", RequestType.SINGLE, pParams, ProjectJson.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, projectCreatorJson);
        EquinixResponse<ProjectJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public ProjectJson delete(String uuid) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        EquinixRequest<Project> equinixRequest = this.buildRequestWithPathParams("DeleteProject", RequestType.SINGLE, pParams, ProjectJson.getSingleTypeRef());
        EquinixResponse<Project> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public ProjectJson refresh(String uuid) {
        return this.getByUuid(uuid);
    }

    public PaginatedList<Project> nextPage(PaginatedRequest<Project> equinixRequest) {
        EquinixResponse<Project> equinixResponse = this.invoke(equinixRequest);
        Page<Project, ProjectJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Project> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, ProjectWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
