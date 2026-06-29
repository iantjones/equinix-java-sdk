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
import api.equinix.javasdk.internetaccess.client.internal.CustomerRouteConfigurationClient;
import api.equinix.javasdk.internetaccess.enums.Redundancy;
import api.equinix.javasdk.internetaccess.enums.RoutingProtocolType;
import api.equinix.javasdk.internetaccess.enums.UseCase;
import api.equinix.javasdk.internetaccess.model.CustomerRouteConfiguration;
import api.equinix.javasdk.internetaccess.model.json.CustomerRouteConfigurationJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1
 * customer-route-configuration lookup {@code GET /internetAccess/v1/customerRouteConfigurations}.
 * The {@code CustomerRouteConfiguration} response is read-only, so the deserialized
 * {@link CustomerRouteConfigurationJson} (which implements {@link CustomerRouteConfiguration}
 * directly) is returned without a wrapper.
 */
public class CustomerRouteConfigurationClientImpl
        extends ResourceClientBase<CustomerRouteConfiguration, CustomerRouteConfigurationJson>
        implements CustomerRouteConfigurationClient {

    public CustomerRouteConfigurationClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "CustomerRouteConfigurationsV1", CustomerRouteConfigurationJson.class);
    }

    @Override
    protected CustomerRouteConfiguration wrap(CustomerRouteConfigurationJson json) {
        return json;
    }

    public Page<CustomerRouteConfiguration, CustomerRouteConfigurationJson> list(UseCase useCase, Redundancy type, RoutingProtocolType routingProtocolType) {
        Map<String, List<String>> queryParams = new HashMap<>();
        Utils.addAdditionalValue(queryParams, "useCase", useCase.toString());
        if (type != null) {
            Utils.addAdditionalValue(queryParams, "type", type.toString());
        }
        if (routingProtocolType != null) {
            Utils.addAdditionalValue(queryParams, "routingProtocol.type", routingProtocolType.toString());
        }
        return listPage("ListCustomerRouteConfigurations", queryParams);
    }
}
