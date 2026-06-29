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
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConfigImpl;
import api.equinix.javasdk.internetaccess.client.internal.OperationalUnitClient;
import api.equinix.javasdk.internetaccess.model.OperationalUnit;
import api.equinix.javasdk.internetaccess.model.json.OperationalUnitJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1 operational-units lookup
 * {@code GET /internetAccess/v1/operationalUnits}. The {@code OperationalUnit} response is
 * read-only, so the deserialized {@link OperationalUnitJson} (which implements
 * {@link OperationalUnit} directly) is returned without a wrapper.
 */
public class OperationalUnitClientImpl extends ResourceClientBase<OperationalUnit, OperationalUnitJson> implements OperationalUnitClient {

    public OperationalUnitClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "OperationalUnitsV1", OperationalUnitJson.class);
    }

    @Override
    protected OperationalUnit wrap(OperationalUnitJson json) {
        return json;
    }

    public Page<OperationalUnit, OperationalUnitJson> list(String ibx) {
        Map<String, List<String>> queryParams = new HashMap<>();
        Utils.addAdditionalValue(queryParams, "location.ibx", ibx);
        return listPage("ListOperationalUnits", queryParams);
    }
}
