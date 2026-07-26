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

package com.eqixiac.equinix.internetaccess.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.http.ParameterMapper;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.internetaccess.client.implementation.InternetAccessConfigImpl;
import com.eqixiac.equinix.internetaccess.client.internal.IbxClient;
import com.eqixiac.equinix.internetaccess.enums.ConnectionType;
import com.eqixiac.equinix.internetaccess.model.Ibx;
import com.eqixiac.equinix.internetaccess.model.json.IbxJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v2 product-availability
 * lookup {@code GET /internetAccess/v2/ibxs}. The {@code Ibx} response is read-only, so the
 * deserialized {@link IbxJson} (which implements {@link Ibx} directly) is returned without a wrapper.
 */
public class IbxClientImpl extends ResourceClientBase<Ibx, IbxJson> implements IbxClient {

    public IbxClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "Ibxs", IbxJson.class);
    }

    @Override
    protected Ibx wrap(IbxJson json) {
        return json;
    }

    public Page<IbxJson> list(ConnectionType connectionType, String accessPointType, String assetType) {
        Map<String, List<String>> queryParams = new HashMap<>();
        ParameterMapper.addAdditionalValue(queryParams, "service.connection.type", connectionType.toString());
        if (accessPointType != null) {
            ParameterMapper.addAdditionalValue(queryParams, "connection.aside.accessPoint.type", accessPointType);
        }
        if (assetType != null) {
            ParameterMapper.addAdditionalValue(queryParams, "asset.type", assetType);
        }
        return listPage("ListIbxs", queryParams);
    }
}
