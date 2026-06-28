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

package api.equinix.javasdk.sts.client.implementation;

import api.equinix.javasdk.core.client.Config;
import api.equinix.javasdk.core.client.EquinixClient;
import api.equinix.javasdk.sts.client.STSConfig;
import api.equinix.javasdk.sts.client.internal.implementation.DiscoveryClientImpl;
import api.equinix.javasdk.sts.client.internal.implementation.OidcProviderClientImpl;
import api.equinix.javasdk.sts.client.internal.implementation.TokenClientImpl;
import lombok.Getter;

@Getter
public class STSConfigImpl extends Config implements STSConfig {

    private final TokenClientImpl tokenClient;

    private final OidcProviderClientImpl oidcProviderClient;

    private final DiscoveryClientImpl discoveryClient;

    public STSConfigImpl(EquinixClient equinixClient) {
        super(equinixClient);
        this.tokenClient = new TokenClientImpl(this);
        this.oidcProviderClient = new OidcProviderClientImpl(this);
        this.discoveryClient = new DiscoveryClientImpl(this);
    }
}
