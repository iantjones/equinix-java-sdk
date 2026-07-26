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

package com.eqixiac.equinix.sts.client.internal.implementation;

import com.eqixiac.equinix.core.client.ClientBase;
import com.eqixiac.equinix.sts.client.implementation.STSConfigImpl;
import com.eqixiac.equinix.sts.client.internal.DiscoveryClient;
import com.eqixiac.equinix.sts.model.Jwks;
import com.eqixiac.equinix.sts.model.OpenIdConfiguration;
import com.eqixiac.equinix.sts.model.json.JwksJson;
import com.eqixiac.equinix.sts.model.json.OpenIdConfigurationJson;

/**
 * Internal client implementation for the STS unauthenticated discovery endpoints. The responses
 * are read-only, so the deserialized JSON models (which implement their interfaces directly) are
 * returned without a wrapper.
 */
public class DiscoveryClientImpl extends ClientBase implements DiscoveryClient {

    public DiscoveryClientImpl(STSConfigImpl configClient) {
        super(configClient, "STS", "Discovery");
    }

    @Override
    public Jwks getJwks() {
        return getAs("GetJwks", null, null, JwksJson.class);
    }

    @Override
    public OpenIdConfiguration getOpenIdConfiguration() {
        return getAs("GetOpenIdConfiguration", null, null, OpenIdConfigurationJson.class);
    }
}
