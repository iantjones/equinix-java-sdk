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
import api.equinix.javasdk.internetaccess.client.internal.DedicatedPortDefaultConfigurationClient;
import api.equinix.javasdk.internetaccess.model.DedicatedPortDefaultConfiguration;
import api.equinix.javasdk.internetaccess.model.json.DedicatedPortDefaultConfigurationJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1 dedicated-port
 * default-configuration lookup {@code GET /internetAccess/v1/dedicatedPortDefaultConfigurations}.
 * The {@code DedicatedPortDefaultConfiguration} response is read-only, so the deserialized
 * {@link DedicatedPortDefaultConfigurationJson} (which implements
 * {@link DedicatedPortDefaultConfiguration} directly) is returned without a wrapper.
 */
public class DedicatedPortDefaultConfigurationClientImpl
        extends ResourceClientBase<DedicatedPortDefaultConfiguration, DedicatedPortDefaultConfigurationJson>
        implements DedicatedPortDefaultConfigurationClient {

    public DedicatedPortDefaultConfigurationClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "DedicatedPortDefaultConfigurationsV1", DedicatedPortDefaultConfigurationJson.class);
    }

    @Override
    protected DedicatedPortDefaultConfiguration wrap(DedicatedPortDefaultConfigurationJson json) {
        return json;
    }

    public Page<DedicatedPortDefaultConfiguration, DedicatedPortDefaultConfigurationJson> list(String ibx) {
        Map<String, List<String>> queryParams = new HashMap<>();
        Utils.addAdditionalValue(queryParams, "connection.aside.accessPoint.location.ibx", ibx);
        return listPage("ListDedicatedPortDefaultConfigurations", queryParams);
    }
}
