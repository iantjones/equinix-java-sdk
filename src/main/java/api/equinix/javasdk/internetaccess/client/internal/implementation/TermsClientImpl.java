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
import api.equinix.javasdk.internetaccess.client.internal.TermsClient;
import api.equinix.javasdk.internetaccess.enums.ConnectivitySourceType;
import api.equinix.javasdk.internetaccess.enums.TermsProduct;
import api.equinix.javasdk.internetaccess.enums.TermsType;
import api.equinix.javasdk.internetaccess.model.TermsAndConditions;
import api.equinix.javasdk.internetaccess.model.json.TermsAndConditionsJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1 terms-and-conditions
 * lookup {@code GET /internetAccess/v1/terms}. The {@code TermsAndConditions} response is read-only,
 * so the deserialized {@link TermsAndConditionsJson} (which implements {@link TermsAndConditions}
 * directly) is returned without a wrapper.
 */
public class TermsClientImpl extends ResourceClientBase<TermsAndConditions, TermsAndConditionsJson> implements TermsClient {

    public TermsClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "TermsV1", TermsAndConditionsJson.class);
    }

    @Override
    protected TermsAndConditions wrap(TermsAndConditionsJson json) {
        return json;
    }

    public Page<TermsAndConditionsJson> list(String accountNumber, String ibx, TermsProduct product, TermsType type, String language) {
        Map<String, List<String>> queryParams = new HashMap<>();
        ParameterMapper.addAdditionalValue(queryParams, "account.accountNumber", accountNumber);
        ParameterMapper.addAdditionalValue(queryParams, "location.ibx", ibx);
        ParameterMapper.addAdditionalValue(queryParams, "connectivitySource.type", ConnectivitySourceType.COLO.toString());
        ParameterMapper.addAdditionalValue(queryParams, "product", product.toString());
        if (type != null) {
            ParameterMapper.addAdditionalValue(queryParams, "type", type.toString());
        }
        if (language != null) {
            ParameterMapper.addAdditionalValue(queryParams, "language", language);
        }
        return listPage("ListTerms", queryParams);
    }
}
