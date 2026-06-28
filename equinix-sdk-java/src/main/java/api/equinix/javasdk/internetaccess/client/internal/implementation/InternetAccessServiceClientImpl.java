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

package api.equinix.javasdk.internetaccess.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConfigImpl;
import api.equinix.javasdk.internetaccess.client.internal.InternetAccessServiceClient;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
import api.equinix.javasdk.internetaccess.model.json.InternetAccessServiceJson;
import api.equinix.javasdk.internetaccess.model.json.creators.ChangeOperationUpdate;
import api.equinix.javasdk.internetaccess.model.json.creators.ServiceRequest;
import api.equinix.javasdk.internetaccess.model.json.creators.ServiceSearchRequest;

import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v2 service lifecycle.
 * Standard get/update/delete/search + paging come from {@link ResourceClientBase}; the
 * {@code ServiceV2} responses are read-only, so the deserialized {@link InternetAccessServiceJson}
 * (which implements {@link InternetAccessService} directly) is returned without a wrapper.
 */
public class InternetAccessServiceClientImpl
        extends ResourceClientBase<InternetAccessService, InternetAccessServiceJson>
        implements InternetAccessServiceClient {

    public InternetAccessServiceClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "Services", InternetAccessServiceJson.class);
    }

    @Override
    protected InternetAccessService wrap(InternetAccessServiceJson json) {
        return json;
    }

    public InternetAccessService create(ServiceRequest serviceRequest) {
        EquinixRequest<InternetAccessServiceJson> equinixRequest =
                buildRequest("CreateService", RequestType.SINGLE, InternetAccessServiceJson.class);
        Utils.serializeJson(equinixRequest, serviceRequest);
        return Utils.handleSingletonResponse(invoke(equinixRequest), equinixRequest);
    }

    public InternetAccessServiceJson getByUuid(String serviceId) {
        return getOne("GetService", serviceId);
    }

    public InternetAccessServiceJson update(String serviceId, List<ChangeOperationUpdate> operations, boolean dryRun) {
        EquinixRequest<InternetAccessServiceJson> request =
                buildRequestWithPathParams("UpdateService", RequestType.SINGLE, Map.of("uuid", serviceId), InternetAccessServiceJson.class);
        if (dryRun) {
            request.addSingleQueryParameter("dryRun", "true");
        }
        Utils.serializeJson(request, operations);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    public Boolean delete(String serviceId, boolean dryRun) {
        Map<String, List<String>> queryParams = dryRun ? Map.of("dryRun", List.of("true")) : null;
        return booleanOp("DeleteService", RequestType.SINGLE, Map.of("uuid", serviceId), queryParams, null);
    }

    public Page<InternetAccessService, InternetAccessServiceJson> search(ServiceSearchRequest searchRequest) {
        return searchPage("SearchServices", searchRequest);
    }
}
