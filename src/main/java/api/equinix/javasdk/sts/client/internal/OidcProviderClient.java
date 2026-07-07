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

package api.equinix.javasdk.sts.client.internal;

import api.equinix.javasdk.sts.model.OidcProvider;
import api.equinix.javasdk.sts.model.json.OidcProviderPage;
import api.equinix.javasdk.sts.model.json.creators.CreateOidcProviderRequest;
import api.equinix.javasdk.sts.model.json.creators.PatchOidcProviderRequest;

/**
 * Internal client for managing trusted OIDC providers within a root project
 * ({@code /v1/projects/{projectId}/oidcProviders}). Operations (by operationId):
 * {@code pageOidcProviders}, {@code createOidcProvider}, {@code patchOidcProvider},
 * {@code deleteOidcProvider}, {@code suspendOidcProvider}, {@code resumeOidcProvider}.
 */
public interface OidcProviderClient {

    OidcProviderPage list(String projectId, Boolean includeSuspended, String pageToken, Integer pageSize);

    OidcProvider create(String projectId, CreateOidcProviderRequest request);

    OidcProvider update(String projectId, String idpId, PatchOidcProviderRequest request);

    Boolean delete(String projectId, String idpId);

    Boolean suspend(String projectId, String idpId);

    Boolean resume(String projectId, String idpId);
}
