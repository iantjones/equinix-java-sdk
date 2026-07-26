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

package com.eqixiac.equinix.projects.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.http.ParameterMapper;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.projects.client.implementation.ProjectsConfigImpl;
import com.eqixiac.equinix.projects.client.internal.ProjectClient;
import com.eqixiac.equinix.projects.model.Project;
import com.eqixiac.equinix.projects.model.json.ProjectJson;

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

    public Page<ProjectJson> list(Boolean includePermissions, Boolean includeInbox) {
        Map<String, List<String>> queryParams = new HashMap<>();
        if (includePermissions != null) {
            queryParams.put("includePermissions", ParameterMapper.singleParamList(includePermissions));
        }
        if (includeInbox != null) {
            queryParams.put("includeInbox", ParameterMapper.singleParamList(includeInbox));
        }
        return listPage("ListProjects", queryParams.isEmpty() ? null : queryParams);
    }
}
