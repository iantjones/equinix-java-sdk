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

package api.equinix.javasdk.projects.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.projects.client.internal.implementation.ProjectClientImpl;
import api.equinix.javasdk.projects.model.Project;
import api.equinix.javasdk.projects.model.json.ProjectJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class ProjectWrapper extends ResourceImpl<Project> implements Project {

    @Delegate(excludes = ProjectMutability.class)
    private ProjectJson jsonObject;
    @Getter
    private final Pageable<Project> serviceClient;

    public ProjectWrapper(ProjectJson projectJson, Pageable<Project> serviceClient) {
        this.jsonObject = projectJson;
        this.serviceClient = serviceClient;
    }

    public Boolean delete() {
        this.jsonObject = ((ProjectClientImpl)this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((ProjectClientImpl)this.serviceClient).refresh(this.getUuid());
    }

    private interface ProjectMutability {
        Boolean delete();
        void refresh();
    }
}
