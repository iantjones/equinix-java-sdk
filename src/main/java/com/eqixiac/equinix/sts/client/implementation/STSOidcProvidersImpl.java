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

package com.eqixiac.equinix.sts.client.implementation;

import com.eqixiac.equinix.STS;
import com.eqixiac.equinix.sts.client.STSOidcProviders;
import com.eqixiac.equinix.sts.client.internal.OidcProviderClient;
import com.eqixiac.equinix.sts.model.OidcProvider;
import com.eqixiac.equinix.sts.model.json.OidcProviderPage;
import com.eqixiac.equinix.sts.model.json.creators.CreateOidcProviderRequest;
import com.eqixiac.equinix.sts.model.json.creators.PatchOidcProviderRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class STSOidcProvidersImpl implements STSOidcProviders {

    private final OidcProviderClient oidcProviderClient;

    private final STS serviceManager;

    public OidcProviderPage list(String projectId) {
        return this.oidcProviderClient.list(projectId, null, null, null);
    }

    public OidcProviderPage list(String projectId, Boolean includeSuspended, String pageToken, Integer pageSize) {
        return this.oidcProviderClient.list(projectId, includeSuspended, pageToken, pageSize);
    }

    public OidcProvider create(String projectId, CreateOidcProviderRequest request) {
        return this.oidcProviderClient.create(projectId, request);
    }

    public OidcProvider update(String projectId, String idpId, PatchOidcProviderRequest request) {
        return this.oidcProviderClient.update(projectId, idpId, request);
    }

    public Boolean delete(String projectId, String idpId) {
        return this.oidcProviderClient.delete(projectId, idpId);
    }

    public Boolean suspend(String projectId, String idpId) {
        return this.oidcProviderClient.suspend(projectId, idpId);
    }

    public Boolean resume(String projectId, String idpId) {
        return this.oidcProviderClient.resume(projectId, idpId);
    }
}
