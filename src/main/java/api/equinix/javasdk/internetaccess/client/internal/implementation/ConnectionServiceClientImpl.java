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
import api.equinix.javasdk.core.http.ParameterMapper;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConfigImpl;
import api.equinix.javasdk.internetaccess.client.internal.ConnectionServiceClient;
import api.equinix.javasdk.internetaccess.model.ConnectionService;
import api.equinix.javasdk.internetaccess.model.json.ConnectionServiceJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1 product-availability
 * lookup {@code GET /internetAccess/v1/connectionServices}. The {@code ConnectionService}
 * response is read-only, so the deserialized {@link ConnectionServiceJson} (which implements
 * {@link ConnectionService} directly) is returned without a wrapper.
 */
public class ConnectionServiceClientImpl extends ResourceClientBase<ConnectionService, ConnectionServiceJson>
        implements ConnectionServiceClient {

    public ConnectionServiceClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "ConnectionServicesV1", ConnectionServiceJson.class);
    }

    @Override
    protected ConnectionService wrap(ConnectionServiceJson json) {
        return json;
    }

    public Page<ConnectionServiceJson> list(String ibx) {
        Map<String, List<String>> queryParams = new HashMap<>();
        ParameterMapper.addAdditionalValue(queryParams, "mediaTypes.connectorTypes.locations.ibx", ibx);
        return listPage("ListConnectionServices", queryParams);
    }
}
