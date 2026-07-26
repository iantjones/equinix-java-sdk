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

package com.eqixiac.equinix.projects.model;

import com.eqixiac.equinix.projects.model.implementation.ProjectPermission;

import java.util.List;
import java.util.Map;

/**
 * A project associated with a root organization, as returned by the Equinix Resource
 * Manager. Projects are read-only organizational containers used to scope and group
 * related Equinix resources.
 */
public interface Project {

    /**
     * Returns the unique identifier of the project.
     *
     * @return the project id
     */
    String getProjectId();

    /**
     * Returns the human-readable name of the project.
     *
     * @return the project name
     */
    String getProjectName();

    /**
     * Returns whether this project is an inbox project.
     *
     * @return {@code true} if this is an inbox project
     */
    Boolean getInboxResource();

    /**
     * Returns the identifier of the parent organization.
     *
     * @return the parent organization id
     */
    String getParentOrganizationId();

    /**
     * Returns the resource labels as key/value pairs.
     *
     * @return the project labels
     */
    Map<String, String> getLabels();

    /**
     * Returns whether the returned project is a SILENT project.
     *
     * @return {@code true} if this is a silent project
     */
    Boolean getSilentProject();

    /**
     * Returns the available user permissions on the project resource. Populated only when the
     * list request is made with {@code includePermissions} enabled; otherwise {@code null}.
     *
     * @return the list of permissions, or {@code null} if not requested
     */
    List<ProjectPermission> getPermissions();
}
