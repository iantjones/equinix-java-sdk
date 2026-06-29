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

package api.equinix.javasdk.customerportal.client.internal.implementation;

import api.equinix.javasdk.core.client.ClientBase;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.SmartHandsClient;
import api.equinix.javasdk.customerportal.model.SmartHandResponse;
import api.equinix.javasdk.customerportal.model.SmartHandType;
import api.equinix.javasdk.customerportal.model.SmartHandsLocation;
import api.equinix.javasdk.customerportal.model.json.SmartHandResponseJson;
import api.equinix.javasdk.customerportal.model.json.SmartHandsLocationsResponseJson;
import api.equinix.javasdk.customerportal.model.json.SmartHandsTypesResponseJson;
import api.equinix.javasdk.customerportal.model.json.creators.SmartHandsRequestJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmartHandsClientImpl extends ClientBase implements SmartHandsClient {

    public SmartHandsClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "SmartHands");
    }

    public SmartHandResponse create(String serviceEndpoint, SmartHandsRequestJson requestJson) {
        return postAs(serviceEndpoint, requestJson, SmartHandResponseJson.class);
    }

    public List<? extends SmartHandsLocation> listLocations(Boolean detail, String ibxs, String cages) {
        Map<String, List<String>> queryParams = new HashMap<>();
        if (detail != null) {
            queryParams.put("detail", List.of(String.valueOf(detail)));
        }
        if (ibxs != null) {
            queryParams.put("ibxs", List.of(ibxs));
        }
        if (cages != null) {
            queryParams.put("cages", List.of(cages));
        }
        SmartHandsLocationsResponseJson response = getAs("ListSmartHandsLocations", null,
                queryParams.isEmpty() ? null : queryParams, SmartHandsLocationsResponseJson.class);
        return response.getLocations();
    }

    public List<? extends SmartHandType> listTypes() {
        SmartHandsTypesResponseJson response = getAs("ListSmartHandsTypes", SmartHandsTypesResponseJson.class);
        return response.getSmarthands();
    }
}
