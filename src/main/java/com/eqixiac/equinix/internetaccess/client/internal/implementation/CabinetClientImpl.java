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
import com.eqixiac.equinix.internetaccess.client.internal.CabinetClient;
import com.eqixiac.equinix.internetaccess.model.Cabinet;
import com.eqixiac.equinix.internetaccess.model.json.CabinetJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1 product-availability
 * lookup {@code GET /internetAccess/v1/cabinets}. The {@code Cabinet} response is read-only, so
 * the deserialized {@link CabinetJson} (which implements {@link Cabinet} directly) is returned
 * without a wrapper.
 */
public class CabinetClientImpl extends ResourceClientBase<Cabinet, CabinetJson> implements CabinetClient {

    public CabinetClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "CabinetsV1", CabinetJson.class);
    }

    @Override
    protected Cabinet wrap(CabinetJson json) {
        return json;
    }

    public Page<CabinetJson> list(String cageSpaceId, String ibx, String accountNumber) {
        Map<String, List<String>> queryParams = new HashMap<>();
        if (cageSpaceId != null) {
            ParameterMapper.addAdditionalValue(queryParams, "cage.spaceId", cageSpaceId);
        }
        if (ibx != null) {
            ParameterMapper.addAdditionalValue(queryParams, "location.ibx", ibx);
        }
        if (accountNumber != null) {
            ParameterMapper.addAdditionalValue(queryParams, "account.accountNumber", accountNumber);
        }
        return listPage("ListCabinets", queryParams);
    }
}
