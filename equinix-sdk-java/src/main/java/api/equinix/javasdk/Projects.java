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

package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.EquinixCredentials;
import api.equinix.javasdk.core.model.Service;
import api.equinix.javasdk.projects.client.ProjectList;
import api.equinix.javasdk.projects.client.ProjectsConfig;
import api.equinix.javasdk.projects.client.implementation.ProjectListImpl;
import api.equinix.javasdk.projects.client.implementation.ProjectsConfigImpl;

/**
 * The primary entry point for accessing Equinix Projects APIs.
 *
 * <p>Projects provides organizational management for Equinix resources, allowing
 * users to group and manage related infrastructure components under named projects.</p>
 *
 * <p>All resource accessors use lazy initialization — internal clients are created on first access
 * and reused for subsequent calls.</p>
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * BasicEquinixCredentials credentials = new BasicEquinixCredentials("clientId", "clientSecret");
 * Projects projects = new Projects(credentials);
 *
 * // List all projects
 * PaginatedList<Project> projectList = projects.projects().list();
 * }</pre>
 *
 * @author ianjones
 * @see api.equinix.javasdk.core.auth.BasicEquinixCredentials
 */
public final class Projects extends EquinixClient implements Service {

    private ProjectList projects;

    final private ProjectsConfig projectsConfig;

    /**
     * Creates a new Projects client using the provided credentials.
     * Authentication occurs automatically on the first API call.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     */
    public Projects(EquinixCredentials equinixCredentials) {
        this(equinixCredentials, false);
    }

    /**
     * Creates a new Projects client with optional sandbox mode.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     * @param isSandBoxed {@code true} to use the sandbox environment for testing; {@code false} for production
     */
    public Projects(EquinixCredentials equinixCredentials, boolean isSandBoxed) {
        super(equinixCredentials, isSandBoxed);

        String paramFile = "json/apiParams_Projects.json";
        equinixClient.appendApiParams(paramFile);

        this.projectsConfig = new ProjectsConfigImpl(equinixClient);
    }

    /**
     * Returns the client for managing Equinix projects.
     * Projects are organizational containers for grouping related infrastructure resources.
     *
     * @return the {@link ProjectList} client for creating, listing, and managing projects
     */
    public ProjectList projects() {
        if (this.projects == null) {
            this.projects = new ProjectListImpl(this.projectsConfig.getProjectClient(), this);
        }
        return projects;
    }
}
