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
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.projects.client.implementation.ProjectsConfigImpl;
import api.equinix.javasdk.projects.client.internal.ProjectClient;
import api.equinix.javasdk.projects.model.Project;
import api.equinix.javasdk.projects.model.json.ProjectJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client for the read-only Equinix Resource Manager projects endpoint
 * ({@code GET /resourceManager/v2/projects}). The JSON model implements the public interface
 * directly, so {@link #wrap(ProjectJson)} is the identity. The {@code limit}/{@code offset}
 * pagination parameters are supplied automatically by the SDK's paging machinery.
 *
 * @author ianjones
 */
public class ProjectClientImpl extends ResourceClientBase<Project, ProjectJson> implements ProjectClient<Project> {

    public ProjectClientImpl(ProjectsConfigImpl configClient) {
        super(configClient, "Projects", "Projects", ProjectJson.class);
    }

    @Override
    protected Project wrap(ProjectJson json) {
        return json;
    }

    public Page<Project, ProjectJson> list(Boolean includePermissions, Boolean includeInbox) {
        Map<String, List<String>> queryParams = new HashMap<>();
        if (includePermissions != null) {
            queryParams.put("includePermissions", Utils.singleParamList(includePermissions));
        }
        if (includeInbox != null) {
            queryParams.put("includeInbox", Utils.singleParamList(includeInbox));
        }
        return listPage("ListProjects", queryParams.isEmpty() ? null : queryParams);
    }
}
