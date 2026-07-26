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

package com.eqixiac.equinix;

import com.eqixiac.equinix.core.auth.EquinixCredentials;
import com.eqixiac.equinix.core.auth.EquinixCredentialsProvider;
import com.eqixiac.equinix.core.auth.EquinixStaticCredentialsProvider;
import com.eqixiac.equinix.projects.client.ProjectList;
import com.eqixiac.equinix.projects.client.implementation.ProjectsConfigImpl;
import com.eqixiac.equinix.projects.client.implementation.ProjectListImpl;
import com.eqixiac.equinix.projects.client.implementation.ProjectsConfigImpl;

/**
 * The primary entry point for accessing Equinix Projects APIs.
 *
 * <p>Projects are read-only organizational containers associated with a root organization,
 * grouping related Equinix infrastructure components under named projects.</p>
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
 * @see com.eqixiac.equinix.core.auth.BasicEquinixCredentials
 */
public final class Projects extends EquinixClient {

    private ProjectList projects;

    final private ProjectsConfigImpl projectsConfig;

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
        this(new EquinixStaticCredentialsProvider(equinixCredentials), isSandBoxed);
    }

    /**
     * Creates a new Projects client whose credentials are resolved through the given provider.
     * Authentication occurs automatically on the first API call.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     */
    public Projects(EquinixCredentialsProvider credentialsProvider) {
        this(credentialsProvider, false);
    }

    /**
     * Creates a new Projects client over a custom credentials provider, with optional sandbox mode.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     * @param isSandBoxed {@code true} to use the sandbox environment for testing; {@code false} for production
     */
    public Projects(EquinixCredentialsProvider credentialsProvider, boolean isSandBoxed) {
        super(credentialsProvider, isSandBoxed);

        String paramFile = "json/apiParams_Projects.json";
        equinixClient.appendApiParams(paramFile);

        this.projectsConfig = new ProjectsConfigImpl(equinixClient);
    }

    /**
     * Creates a new Projects client with explicit {@link EquinixConfig} options.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     * @param config the construction-time options
     */
    public Projects(EquinixCredentials equinixCredentials, EquinixConfig config) {
        this(new EquinixStaticCredentialsProvider(equinixCredentials), config);
    }

    /**
     * Creates a new Projects client over a custom credentials provider with explicit
     * {@link EquinixConfig} options.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     * @param config the construction-time options
     */
    public Projects(EquinixCredentialsProvider credentialsProvider, EquinixConfig config) {
        super(credentialsProvider, config);

        String paramFile = "json/apiParams_Projects.json";
        equinixClient.appendApiParams(paramFile);

        this.projectsConfig = new ProjectsConfigImpl(equinixClient);
    }

    /**
     * Package-private constructor for {@link Equinix} sessions: builds this domain client over a
     * shared core client (one OAuth token + connection pool across domains).
     */
    Projects(com.eqixiac.equinix.core.client.EquinixClient sharedCore) {
        super(sharedCore);
        equinixClient.appendApiParams("json/apiParams_Projects.json");
        this.projectsConfig = new ProjectsConfigImpl(equinixClient);
    }

    /**
     * Returns the client for listing Equinix projects.
     * Projects are read-only organizational containers for grouping related infrastructure resources.
     *
     * @return the {@link ProjectList} client for listing projects
     */
    public ProjectList projects() {
        if (this.projects == null) {
            this.projects = new ProjectListImpl(this.projectsConfig.getProjectClient(), this);
        }
        return projects;
    }
}
