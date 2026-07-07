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
import api.equinix.javasdk.internetaccess.client.internal.RoutingConfigurationClient;
import api.equinix.javasdk.internetaccess.enums.Redundancy;
import api.equinix.javasdk.internetaccess.enums.UseCase;
import api.equinix.javasdk.internetaccess.model.RoutingProtocolConfiguration;
import api.equinix.javasdk.internetaccess.model.json.RoutingProtocolConfigurationJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1 routing-configuration
 * lookup {@code GET /internetAccess/v1/routingProtocolConfigurations}. The
 * {@code RoutingProtocolConfiguration} response is read-only, so the deserialized
 * {@link RoutingProtocolConfigurationJson} (which implements {@link RoutingProtocolConfiguration}
 * directly) is returned without a wrapper.
 */
public class RoutingConfigurationClientImpl
        extends ResourceClientBase<RoutingProtocolConfiguration, RoutingProtocolConfigurationJson>
        implements RoutingConfigurationClient {

    public RoutingConfigurationClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "RoutingConfigurationsV1", RoutingProtocolConfigurationJson.class);
    }

    @Override
    protected RoutingProtocolConfiguration wrap(RoutingProtocolConfigurationJson json) {
        return json;
    }

    public Page<RoutingProtocolConfigurationJson> list(UseCase useCase, Redundancy type) {
        Map<String, List<String>> queryParams = new HashMap<>();
        ParameterMapper.addAdditionalValue(queryParams, "useCase", useCase.toString());
        if (type != null) {
            ParameterMapper.addAdditionalValue(queryParams, "type", type.toString());
        }
        return listPage("ListRoutingConfigurations", queryParams);
    }
}
