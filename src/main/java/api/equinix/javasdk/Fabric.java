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
import api.equinix.javasdk.customerportal.client.Invoices;
import api.equinix.javasdk.fabric.client.*;
import api.equinix.javasdk.fabric.client.implementation.*;

/**
 * <p>Fabric class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public final class Fabric extends EquinixClient implements Service {

    private Metros metros;

    private ServiceTokens serviceTokens;

    private Ports ports;

    private Connections connections;

    private Prices prices;

    private ServiceProfiles serviceProfiles;

    private FabricGateways fabricGateways;

    final private FabricConfig fabricConfig;

    /**
     * <p>Constructor for Fabric.</p>
     *
     * @param equinixCredentials a {@link api.equinix.javasdk.core.auth.EquinixCredentials} object.
     */
    public Fabric(EquinixCredentials equinixCredentials) {
        this(equinixCredentials, false);
    }

    /**
     * <p>Constructor for Fabric.</p>
     *
     * @param equinixCredentials a {@link api.equinix.javasdk.core.auth.EquinixCredentials} object.
     * @param isSandBoxed a boolean.
     */
    public Fabric(EquinixCredentials equinixCredentials, boolean isSandBoxed) {
        super(equinixCredentials, isSandBoxed);

        String paramFile = "json/apiParams_Fabric.json";
        equinixClient.appendApiParams(paramFile);

        this.fabricConfig = new FabricConfigImpl(equinixClient);
    }

    /**
     * <p>metros.</p>
     *
     * @return a {@link Metros} object.
     */
    public Metros metros() {
        if (this.metros == null) {
            this.metros = new MetrosImpl(this.fabricConfig.getMetrosClient(), this);
        }
        return metros;
    }

    /**
     * <p>serviceTokens.</p>
     *
     * @return a {@link Invoices} object.
     */
    public ServiceTokens serviceTokens() {
        if (this.serviceTokens == null) {
            this.serviceTokens = new ServiceTokensImpl(this.fabricConfig.getServiceTokensClient(), this);
        }
        return serviceTokens;
    }

    /**
     * <p>ports.</p>
     *
     * @return a {@link Ports} object.
     */
    public Ports ports() {
        if (this.ports == null) {
            this.ports = new PortsImpl(this.fabricConfig.getPortsClient(), this.fabricConfig.getPortStatisticsClient(), this);
        }
        return ports;
    }

    /**
     * <p>connections.</p>
     *
     * @return a {@link Connections} object.
     */
    public Connections connections() {
        if (this.connections == null) {
            this.connections = new ConnectionsImpl(this.fabricConfig.getConnectionsClient(), this);
        }
        return connections;
    }

    public Prices prices() {
        if (this.prices == null) {
            this.prices = new PricesImpl(this.fabricConfig.getPricingClient(), this);
        }
        return prices;
    }

    /**
     * <p>serviceProfiles.</p>
     *
     * @return a {@link ServiceProfiles} object.
     */
    public ServiceProfiles serviceProfiles() {
        if (this.serviceProfiles == null) {
            this.serviceProfiles = new ServiceProfilesImpl(this.fabricConfig.getServiceProfilesClient(), this);
        }
        return serviceProfiles;
    }

    public FabricGateways fabricGateways() {
        if (this.fabricGateways == null) {
            this.fabricGateways = new FabricGatewaysImpl(this.fabricConfig.getFabricGatewaysClient(), this);
        }
        return fabricGateways;
    }
}
