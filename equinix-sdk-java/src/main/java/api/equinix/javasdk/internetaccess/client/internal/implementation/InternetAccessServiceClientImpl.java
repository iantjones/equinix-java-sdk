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

import api.equinix.javasdk.core.client.ClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConfigImpl;
import api.equinix.javasdk.internetaccess.client.internal.InternetAccessServiceClient;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
import api.equinix.javasdk.internetaccess.model.json.InternetAccessServiceJson;
import api.equinix.javasdk.internetaccess.model.json.creators.ServiceRequest;

/**
 * Internal client implementation for the single Equinix Internet Access (EIA) v2 operation:
 * {@code POST /internetAccess/v2/services}. The {@code ServiceV2} response is read-only, so the
 * deserialized {@link InternetAccessServiceJson} (which implements {@link InternetAccessService}
 * directly) is returned without a wrapper.
 */
public class InternetAccessServiceClientImpl extends ClientBase implements InternetAccessServiceClient {

    public InternetAccessServiceClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "Services");
    }

    public InternetAccessService create(ServiceRequest serviceRequest) {
        EquinixRequest<InternetAccessServiceJson> equinixRequest =
                buildRequest("CreateService", RequestType.SINGLE, InternetAccessServiceJson.class);
        Utils.serializeJson(equinixRequest, serviceRequest);
        return Utils.handleSingletonResponse(invoke(equinixRequest), equinixRequest);
    }
}
