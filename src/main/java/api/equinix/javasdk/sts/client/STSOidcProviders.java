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

package api.equinix.javasdk.sts.client;

import api.equinix.javasdk.sts.model.OidcProvider;
import api.equinix.javasdk.sts.model.json.OidcProviderPage;
import api.equinix.javasdk.sts.model.json.creators.CreateOidcProviderRequest;
import api.equinix.javasdk.sts.model.json.creators.PatchOidcProviderRequest;

/**
 * Client interface for managing trusted OIDC providers within a root project
 * ({@code /v1/projects/{projectId}/oidcProviders}).
 *
 * <p>Supports paging the providers of a project ({@code pageOidcProviders}), registering a new
 * provider ({@code createOidcProvider}), modifying ({@code patchOidcProvider}) and permanently
 * removing ({@code deleteOidcProvider}) one, and the reversible suspend/resume lifecycle
 * ({@code suspendOidcProvider} / {@code resumeOidcProvider}).</p>
 */
public interface STSOidcProviders {

    /**
     * Pages through the OIDC providers of a project (first page, excluding suspended).
     *
     * @param projectId the project identifier
     * @return the first page of OIDC providers
     */
    OidcProviderPage list(String projectId);

    /**
     * Pages through the OIDC providers of a project, controlling inclusion of suspended providers
     * and pagination.
     *
     * @param projectId the project identifier
     * @param includeSuspended {@code true} to include suspended providers, or {@code null} for the default
     * @param pageToken the opaque page token from a prior response, or {@code null} for the first page
     * @param pageSize the maximum number of results per page, or {@code null} for the server default
     * @return the requested page of OIDC providers
     */
    OidcProviderPage list(String projectId, Boolean includeSuspended, String pageToken, Integer pageSize);

    /**
     * Registers a new OIDC provider in a project.
     *
     * @param projectId the project identifier
     * @param request the provider registration request
     * @return the registered OIDC provider
     */
    OidcProvider create(String projectId, CreateOidcProviderRequest request);

    /**
     * Modifies specific properties of an existing OIDC provider.
     *
     * @param projectId the project identifier
     * @param idpId the identity provider identifier
     * @param request the patch request (its {@code lastRev} guards against concurrent updates)
     * @return the updated OIDC provider
     */
    OidcProvider update(String projectId, String idpId, PatchOidcProviderRequest request);

    /**
     * Permanently removes the trust relationship for an OIDC provider. This cannot be reversed.
     *
     * @param projectId the project identifier
     * @param idpId the identity provider identifier
     * @return {@code true} if the deletion request was accepted
     */
    Boolean delete(String projectId, String idpId);

    /**
     * Suspends an OIDC provider (a reversible way to turn off token exchange for its ID tokens).
     *
     * @param projectId the project identifier
     * @param idpId the identity provider identifier
     * @return {@code true} if the suspend request was accepted
     */
    Boolean suspend(String projectId, String idpId);

    /**
     * Restores (resumes) a previously suspended OIDC provider.
     *
     * @param projectId the project identifier
     * @param idpId the identity provider identifier
     * @return {@code true} if the resume request was accepted
     */
    Boolean resume(String projectId, String idpId);
}
