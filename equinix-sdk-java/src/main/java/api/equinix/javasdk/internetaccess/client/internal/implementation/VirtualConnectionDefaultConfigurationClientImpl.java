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
import api.equinix.javasdk.internetaccess.client.internal.VirtualConnectionDefaultConfigurationClient;
import api.equinix.javasdk.internetaccess.model.VirtualConnectionDefaultConfiguration;
import api.equinix.javasdk.internetaccess.model.json.VirtualConnectionDefaultConfigurationJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1 virtual-connection
 * default-configuration lookup
 * {@code GET /internetAccess/v1/virtualConnectionDefaultConfigurations}. The
 * {@code VirtualConnectionDefaultConfiguration} response is read-only, so the deserialized
 * {@link VirtualConnectionDefaultConfigurationJson} (which implements
 * {@link VirtualConnectionDefaultConfiguration} directly) is returned without a wrapper.
 */
public class VirtualConnectionDefaultConfigurationClientImpl
        extends ResourceClientBase<VirtualConnectionDefaultConfiguration, VirtualConnectionDefaultConfigurationJson>
        implements VirtualConnectionDefaultConfigurationClient {

    public VirtualConnectionDefaultConfigurationClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "VirtualConnectionDefaultConfigurationsV1", VirtualConnectionDefaultConfigurationJson.class);
    }

    @Override
    protected VirtualConnectionDefaultConfiguration wrap(VirtualConnectionDefaultConfigurationJson json) {
        return json;
    }

    public Page<VirtualConnectionDefaultConfiguration, VirtualConnectionDefaultConfigurationJson> list(String ibx, String metroCode) {
        Map<String, List<String>> queryParams = new HashMap<>();
        Utils.addAdditionalValue(queryParams, "connection.aside.accessPoint.location.ibx", ibx);
        if (metroCode != null) {
            Utils.addAdditionalValue(queryParams, "connection.aside.accessPoint.location.metroCode", metroCode);
        }
        return listPage("ListVirtualConnectionDefaultConfigurations", queryParams);
    }
}
