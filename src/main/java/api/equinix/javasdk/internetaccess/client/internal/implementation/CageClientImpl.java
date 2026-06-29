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
import api.equinix.javasdk.internetaccess.client.internal.CageClient;
import api.equinix.javasdk.internetaccess.model.Cage;
import api.equinix.javasdk.internetaccess.model.json.CageJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1 product-availability
 * lookup {@code GET /internetAccess/v1/cages}. The {@code Cage} response is read-only, so the
 * deserialized {@link CageJson} (which implements {@link Cage} directly) is returned without a
 * wrapper.
 */
public class CageClientImpl extends ResourceClientBase<Cage, CageJson> implements CageClient {

    public CageClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "CagesV1", CageJson.class);
    }

    @Override
    protected Cage wrap(CageJson json) {
        return json;
    }

    public Page<Cage, CageJson> list(String ibx, String accountNumber) {
        Map<String, List<String>> queryParams = new HashMap<>();
        Utils.addAdditionalValue(queryParams, "location.ibx", ibx);
        Utils.addAdditionalValue(queryParams, "account.accountNumber", accountNumber);
        return listPage("ListCages", queryParams);
    }
}
