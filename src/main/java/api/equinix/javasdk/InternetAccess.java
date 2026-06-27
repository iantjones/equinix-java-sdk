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

package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.EquinixCredentials;
import api.equinix.javasdk.core.model.Service;
import api.equinix.javasdk.internetaccess.client.InternetAccessConfig;
import api.equinix.javasdk.internetaccess.client.InternetAccessServices;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConfigImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessServicesImpl;

/**
 * The primary entry point for accessing the Equinix Internet Access (EIA) v2 API.
 *
 * <p>Equinix Internet Access provides managed internet connectivity services through Equinix
 * data centers. EIA v2 exposes a single operation — creating a service via
 * {@code POST /internetAccess/v2/services} — where the IP blocks and routing configuration are
 * all supplied as a single nested request body.</p>
 *
 * <p>The {@link #services()} accessor uses lazy initialization — the internal client is created
 * on first access and reused for subsequent calls.</p>
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * BasicEquinixCredentials credentials = new BasicEquinixCredentials("clientId", "clientSecret");
 * InternetAccess internetAccess = new InternetAccess(credentials);
 *
 * InternetAccessService service = internetAccess.services().define()
 *     .name("WebServers")
 *     .type(ServiceTypeV2.SINGLE)
 *     .connection("9b8c5042-b553-4d5e-a2ac-c73bf6d4fd81")
 *     .routingProtocol(BgpRoutingProtocolRequest.builder()
 *         .customerAsn(16220L)
 *         .exportPolicy(ExportPolicy.FULL)
 *         .build())
 *     .create();
 * }</pre>
 *
 * @author ianjones
 * @see api.equinix.javasdk.core.auth.BasicEquinixCredentials
 */
public final class InternetAccess extends EquinixClient implements Service {

    private InternetAccessServices services;

    final private InternetAccessConfig internetAccessConfig;

    /**
     * Creates a new Internet Access client using the provided credentials.
     * Authentication occurs automatically on the first API call.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     */
    public InternetAccess(EquinixCredentials equinixCredentials) {
        this(equinixCredentials, false);
    }

    /**
     * Creates a new Internet Access client with optional sandbox mode.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     * @param isSandBoxed {@code true} to use the sandbox environment for testing; {@code false} for production
     */
    public InternetAccess(EquinixCredentials equinixCredentials, boolean isSandBoxed) {
        super(equinixCredentials, isSandBoxed);

        String paramFile = "json/apiParams_InternetAccess.json";
        equinixClient.appendApiParams(paramFile);

        this.internetAccessConfig = new InternetAccessConfigImpl(equinixClient);
    }

    /**
     * Returns the client for creating Equinix Internet Access v2 service instances.
     *
     * @return the {@link InternetAccessServices} client
     */
    public InternetAccessServices services() {
        if (this.services == null) {
            this.services = new InternetAccessServicesImpl(this.internetAccessConfig.getInternetAccessServiceClient(), this);
        }
        return services;
    }
}
