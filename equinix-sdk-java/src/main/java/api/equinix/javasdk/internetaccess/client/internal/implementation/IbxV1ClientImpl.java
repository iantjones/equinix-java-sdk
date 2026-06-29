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
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConfigImpl;
import api.equinix.javasdk.internetaccess.client.internal.IbxV1Client;
import api.equinix.javasdk.internetaccess.enums.ConnectionType;
import api.equinix.javasdk.internetaccess.model.Ibx;
import api.equinix.javasdk.internetaccess.model.json.IbxJson;

import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1 single-IBX get
 * {@code GET /internetAccess/v1/ibxs/{ibx}}. The {@code Ibx} response is read-only, so the
 * deserialized {@link IbxJson} (which implements {@link Ibx} directly) is returned without a
 * wrapper.
 */
public class IbxV1ClientImpl extends ResourceClientBase<Ibx, IbxJson> implements IbxV1Client {

    public IbxV1ClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "IbxsV1", IbxJson.class);
    }

    @Override
    protected Ibx wrap(IbxJson json) {
        return json;
    }

    public Ibx getByCode(String ibx, ConnectionType connectionType, String accessPointType) {
        Map<String, String> pathParams = Map.of("ibx", ibx);
        EquinixRequest<IbxJson> request = buildRequestWithPathParams("GetIbx",
                api.equinix.javasdk.core.enums.RequestType.SINGLE, pathParams, IbxJson.class);
        if (connectionType != null) {
            request.addSingleQueryParameter("service.connection.type", connectionType.toString());
        }
        if (accessPointType != null) {
            request.addSingleQueryParameter("connection.aside.accessPoint.type", accessPointType);
        }
        return api.equinix.javasdk.core.http.Utils.handleSingletonResponse(invoke(request), request);
    }
}
