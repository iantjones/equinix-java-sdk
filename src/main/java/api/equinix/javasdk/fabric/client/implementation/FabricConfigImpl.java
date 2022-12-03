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

package api.equinix.javasdk.fabric.client.implementation;

import api.equinix.javasdk.core.client.Config;
import api.equinix.javasdk.core.client.EquinixClient;
import api.equinix.javasdk.fabric.client.FabricConfig;
import api.equinix.javasdk.fabric.client.internal.implementation.*;
import lombok.Getter;

/**
 * <p>FabricConfigImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class FabricConfigImpl extends Config implements FabricConfig {

    private final MetroClientImpl metrosClient;

    private final ServiceTokenClientImpl serviceTokensClient;

    private final PortClientImpl portsClient;

    private final PortStatisticClientImpl portStatisticsClient;

    private final PricingClientImpl pricingClient;

    private final ConnectionClientImpl connectionsClient;

    private final ServiceProfileClientImpl serviceProfilesClient;

    private final FabricGatewayClientImpl fabricGatewaysClient;

    /**
     * <p>Constructor for FabricConfigImpl.</p>
     *
     * @param equinixClient a {@link api.equinix.javasdk.core.client.EquinixClient} object.
     */
    public FabricConfigImpl(EquinixClient equinixClient) {
        super(equinixClient);
        this.metrosClient = new MetroClientImpl(this);
        this.serviceTokensClient = new ServiceTokenClientImpl(this);
        this.portsClient = new PortClientImpl(this);
        this.portStatisticsClient = new PortStatisticClientImpl(this);
        this.connectionsClient = new ConnectionClientImpl(this);
        this.pricingClient = new PricingClientImpl(this);
        this.serviceProfilesClient = new ServiceProfileClientImpl(this);
        this.fabricGatewaysClient = new FabricGatewayClientImpl(this);
    }
}