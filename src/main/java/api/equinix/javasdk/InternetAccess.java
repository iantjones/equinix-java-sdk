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
import api.equinix.javasdk.internetaccess.client.InternetAccessPorts;
import api.equinix.javasdk.internetaccess.client.RoutingConfigs;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConfigImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessServicesImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessPortsImpl;
import api.equinix.javasdk.internetaccess.client.implementation.RoutingConfigsImpl;

/**
 * The primary entry point for accessing Equinix Internet Access APIs.
 *
 * <p>Internet Access provides managed internet connectivity services through Equinix
 * data centers. This class offers typed access to internet access services, associated ports,
 * and routing configurations.</p>
 *
 * <p>All resource accessors use lazy initialization — internal clients are created on first access
 * and reused for subsequent calls.</p>
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * BasicEquinixCredentials credentials = new BasicEquinixCredentials("clientId", "clientSecret");
 * InternetAccess internetAccess = new InternetAccess(credentials);
 *
 * // List internet access services
 * PaginatedList<InternetAccessService> services = internetAccess.services().list();
 * }</pre>
 *
 * @author ianjones
 * @see api.equinix.javasdk.core.auth.BasicEquinixCredentials
 */
public final class InternetAccess extends EquinixClient implements Service {

    private InternetAccessServices services;

    private InternetAccessPorts ports;

    private RoutingConfigs routingConfigs;

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
     * Returns the client for managing Internet Access service instances.
     * Each service represents a managed internet connectivity subscription at an Equinix facility.
     *
     * @return the {@link InternetAccessServices} client for managing internet access services
     */
    public InternetAccessServices services() {
        if (this.services == null) {
            this.services = new InternetAccessServicesImpl(this.internetAccessConfig.getInternetAccessServiceClient(), this);
        }
        return services;
    }

    /**
     * Returns the client for managing Internet Access ports.
     * Ports represent the physical network interfaces used for internet access connectivity.
     *
     * @return the {@link InternetAccessPorts} client for managing internet access ports
     */
    public InternetAccessPorts ports() {
        if (this.ports == null) {
            this.ports = new InternetAccessPortsImpl(this.internetAccessConfig.getInternetAccessPortClient(), this);
        }
        return ports;
    }

    /**
     * Returns the client for managing routing configurations for Internet Access services.
     * Routing configurations define BGP sessions, prefixes, and routing policies.
     *
     * @return the {@link RoutingConfigs} client for managing routing configurations
     */
    public RoutingConfigs routingConfigs() {
        if (this.routingConfigs == null) {
            this.routingConfigs = new RoutingConfigsImpl(this.internetAccessConfig.getRoutingConfigClient(), this);
        }
        return routingConfigs;
    }
}
