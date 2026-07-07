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
import api.equinix.javasdk.internetaccess.client.internal.PortConfigurationClient;
import api.equinix.javasdk.internetaccess.enums.UseCase;
import api.equinix.javasdk.internetaccess.model.PortConfiguration;
import api.equinix.javasdk.internetaccess.model.json.PortConfigurationJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1 port-configuration lookup
 * {@code GET /internetAccess/v1/portConfigurations}. The {@code PortConfiguration} response is
 * read-only, so the deserialized {@link PortConfigurationJson} (which implements
 * {@link PortConfiguration} directly) is returned without a wrapper.
 */
public class PortConfigurationClientImpl
        extends ResourceClientBase<PortConfiguration, PortConfigurationJson>
        implements PortConfigurationClient {

    public PortConfigurationClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "PortConfigurationsV1", PortConfigurationJson.class);
    }

    @Override
    protected PortConfiguration wrap(PortConfigurationJson json) {
        return json;
    }

    public Page<PortConfigurationJson> list(String ibx, UseCase useCase) {
        Map<String, List<String>> queryParams = new HashMap<>();
        ParameterMapper.addAdditionalValue(queryParams, "connection.aside.accessPoint.port.location.ibx", ibx);
        ParameterMapper.addAdditionalValue(queryParams, "useCase", useCase.toString());
        return listPage("ListPortConfigurations", queryParams);
    }
}
