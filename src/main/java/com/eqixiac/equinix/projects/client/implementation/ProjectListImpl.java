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

package com.eqixiac.equinix.projects.client.implementation;

import com.eqixiac.equinix.Projects;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.projects.client.ProjectList;
import com.eqixiac.equinix.projects.client.internal.ProjectClient;
import com.eqixiac.equinix.projects.model.Project;
import com.eqixiac.equinix.projects.model.json.ProjectJson;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProjectListImpl implements ProjectList {

    private final ProjectClient<Project> serviceClient;

    private final Projects serviceManager;

    public PaginatedList<Project> list() {
        return this.list(null, null);
    }

    public PaginatedList<Project> list(Boolean includePermissions, Boolean includeInbox) {
        Page<ProjectJson> responsePage = this.serviceClient.list(includePermissions, includeInbox);
        PaginatedList<Project> projectList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(projectList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }
}
