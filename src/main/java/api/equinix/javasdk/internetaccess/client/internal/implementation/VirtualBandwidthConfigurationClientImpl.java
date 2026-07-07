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
import api.equinix.javasdk.internetaccess.client.internal.VirtualBandwidthConfigurationClient;
import api.equinix.javasdk.internetaccess.enums.BillingType;
import api.equinix.javasdk.internetaccess.enums.UseCase;
import api.equinix.javasdk.internetaccess.model.VirtualBandwidthConfiguration;
import api.equinix.javasdk.internetaccess.model.json.VirtualBandwidthConfigurationJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1
 * virtual-bandwidth-configuration lookup
 * {@code GET /internetAccess/v1/virtualBandwidthConfigurations}. The
 * {@code VirtualBandwidthConfiguration} response is read-only, so the deserialized
 * {@link VirtualBandwidthConfigurationJson} (which implements {@link VirtualBandwidthConfiguration}
 * directly) is returned without a wrapper.
 */
public class VirtualBandwidthConfigurationClientImpl
        extends ResourceClientBase<VirtualBandwidthConfiguration, VirtualBandwidthConfigurationJson>
        implements VirtualBandwidthConfigurationClient {

    public VirtualBandwidthConfigurationClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "VirtualBandwidthConfigurationsV1", VirtualBandwidthConfigurationJson.class);
    }

    @Override
    protected VirtualBandwidthConfiguration wrap(VirtualBandwidthConfigurationJson json) {
        return json;
    }

    public Page<VirtualBandwidthConfigurationJson> list(UseCase useCase, BillingType billing) {
        Map<String, List<String>> queryParams = new HashMap<>();
        ParameterMapper.addAdditionalValue(queryParams, "useCase", useCase.toString());
        if (billing != null) {
            ParameterMapper.addAdditionalValue(queryParams, "billing", billing.toString());
        }
        return listPage("ListVirtualBandwidthConfigurations", queryParams);
    }
}
