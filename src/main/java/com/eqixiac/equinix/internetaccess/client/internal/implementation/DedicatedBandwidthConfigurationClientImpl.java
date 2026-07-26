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
import com.eqixiac.equinix.internetaccess.client.internal.DedicatedBandwidthConfigurationClient;
import com.eqixiac.equinix.internetaccess.enums.BillingType;
import com.eqixiac.equinix.internetaccess.enums.UseCase;
import com.eqixiac.equinix.internetaccess.model.DedicatedBandwidthConfiguration;
import com.eqixiac.equinix.internetaccess.model.json.DedicatedBandwidthConfigurationJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1
 * dedicated-bandwidth-configuration lookup
 * {@code GET /internetAccess/v1/dedicatedBandwidthConfigurations}. The
 * {@code DedicatedBandwidthConfiguration} response is read-only, so the deserialized
 * {@link DedicatedBandwidthConfigurationJson} (which implements
 * {@link DedicatedBandwidthConfiguration} directly) is returned without a wrapper.
 */
public class DedicatedBandwidthConfigurationClientImpl
        extends ResourceClientBase<DedicatedBandwidthConfiguration, DedicatedBandwidthConfigurationJson>
        implements DedicatedBandwidthConfigurationClient {

    public DedicatedBandwidthConfigurationClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "DedicatedBandwidthConfigurationsV1", DedicatedBandwidthConfigurationJson.class);
    }

    @Override
    protected DedicatedBandwidthConfiguration wrap(DedicatedBandwidthConfigurationJson json) {
        return json;
    }

    public Page<DedicatedBandwidthConfigurationJson> list(UseCase useCase, BillingType billing, Integer speed) {
        Map<String, List<String>> queryParams = new HashMap<>();
        ParameterMapper.addAdditionalValue(queryParams, "useCase", useCase.toString());
        if (billing != null) {
            ParameterMapper.addAdditionalValue(queryParams, "billing", billing.toString());
        }
        if (speed != null) {
            ParameterMapper.addAdditionalValue(queryParams, "connection.aside.accessPoint.port.physicalPort.speed", speed.toString());
        }
        return listPage("ListDedicatedBandwidthConfigurations", queryParams);
    }
}
