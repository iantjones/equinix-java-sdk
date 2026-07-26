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

package com.eqixiac.equinix.sts.client.internal.implementation;

import com.eqixiac.equinix.core.client.ClientBase;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.sts.client.implementation.STSConfigImpl;
import com.eqixiac.equinix.sts.client.internal.OidcProviderClient;
import com.eqixiac.equinix.core.http.request.QueryParamBuilder;
import com.eqixiac.equinix.sts.model.OidcProvider;
import com.eqixiac.equinix.sts.model.json.OidcProviderJson;
import com.eqixiac.equinix.sts.model.json.OidcProviderPage;
import com.eqixiac.equinix.sts.model.json.creators.CreateOidcProviderRequest;
import com.eqixiac.equinix.sts.model.json.creators.PatchOidcProviderRequest;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for STS OIDC providers. Provider responses are read-only and the
 * page operation uses opaque-token ({@code nextPageToken}) pagination, so the deserialized JSON
 * models are returned directly.
 */
public class OidcProviderClientImpl extends ClientBase implements OidcProviderClient {

    public OidcProviderClientImpl(STSConfigImpl configClient) {
        super(configClient, "STS", "OidcProviders");
    }

    @Override
    public OidcProviderPage list(String projectId, Boolean includeSuspended, String pageToken, Integer pageSize) {
        Map<String, List<String>> queryParams = QueryParamBuilder.builder()
                .add("includeSuspended", includeSuspended)
                .add("pageToken", pageToken)
                .add("pageSize", pageSize)
                .build();
        return getAs("PageOidcProviders", Map.of("projectId", projectId), queryParams, OidcProviderPage.class);
    }

    @Override
    public OidcProvider create(String projectId, CreateOidcProviderRequest request) {
        return postForType("CreateOidcProvider", Map.of("projectId", projectId), request,
                new TypeReference<OidcProviderJson>() {
                });
    }

    @Override
    public OidcProvider update(String projectId, String idpId, PatchOidcProviderRequest request) {
        return postForType("PatchOidcProvider", Map.of("projectId", projectId, "idpId", idpId), request,
                new TypeReference<OidcProviderJson>() {
                });
    }

    @Override
    public Boolean delete(String projectId, String idpId) {
        return booleanOp("DeleteOidcProvider", RequestType.SINGLE,
                Map.of("projectId", projectId, "idpId", idpId), null, null);
    }

    @Override
    public Boolean suspend(String projectId, String idpId) {
        return booleanOp("SuspendOidcProvider", RequestType.SINGLE,
                Map.of("projectId", projectId, "idpId", idpId), null, null);
    }

    @Override
    public Boolean resume(String projectId, String idpId) {
        return booleanOp("ResumeOidcProvider", RequestType.SINGLE,
                Map.of("projectId", projectId, "idpId", idpId), null, null);
    }
}
