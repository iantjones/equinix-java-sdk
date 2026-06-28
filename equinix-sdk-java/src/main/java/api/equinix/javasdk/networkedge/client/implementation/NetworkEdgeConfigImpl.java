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

package api.equinix.javasdk.networkedge.client.implementation;

import api.equinix.javasdk.core.client.Config;
import api.equinix.javasdk.core.client.EquinixClient;
import api.equinix.javasdk.networkedge.client.NetworkEdgeConfig;
import api.equinix.javasdk.networkedge.client.internal.ACLTemplateClient;
import api.equinix.javasdk.networkedge.client.internal.BGPPeeringClient;
import api.equinix.javasdk.networkedge.client.internal.VPNClient;
import api.equinix.javasdk.networkedge.client.internal.implementation.*;
import api.equinix.javasdk.networkedge.model.ACLTemplate;
import api.equinix.javasdk.networkedge.model.BGPPeering;
import api.equinix.javasdk.networkedge.model.VPN;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * <p>NetworkEdgeConfigImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class NetworkEdgeConfigImpl extends Config implements NetworkEdgeConfig {

    private final MetroClientImpl metrosClient;

    private final AccountClientImpl accountsClient;

    private final AgreementClientImpl agreementsClient;

    private final DeviceClientImpl devicesClient;

    private final DeviceTypeClientImpl deviceTypesClient;

    private final PublicKeyClientImpl publicKeysClient;
    
    private final DeviceLinkClientImpl deviceLinksClient;

    private final ACLTemplateClientImpl aclTemplateClient;

    private final VPNClientImpl vpnClient;

    private final BGPPeeringClientImpl bgpClient;

    private final BackupClientImpl backupClient;

    private final PricingClientImpl pricingClient;

    private final FilesClientImpl filesClient;

    /**
     * <p>Constructor for NetworkEdgeConfigImpl.</p>
     *
     * @param equinixClient a {@link api.equinix.javasdk.core.client.EquinixClient} object.
     */
    public NetworkEdgeConfigImpl(EquinixClient equinixClient) {
        super(equinixClient);
        this.metrosClient = new MetroClientImpl(this);
        this.accountsClient = new AccountClientImpl(this);
        this.agreementsClient = new AgreementClientImpl(this);
        this.devicesClient = new DeviceClientImpl(this);
        this.deviceTypesClient = new DeviceTypeClientImpl(this);
        this.publicKeysClient = new PublicKeyClientImpl(this);
        this.deviceLinksClient = new DeviceLinkClientImpl(this);
        this.aclTemplateClient = new ACLTemplateClientImpl(this);
        this.vpnClient = new VPNClientImpl(this);
        this.bgpClient = new BGPPeeringClientImpl(this);
        this.backupClient = new BackupClientImpl(this);
        this.pricingClient = new PricingClientImpl(this);
        this.filesClient = new FilesClientImpl(this);
    }

    /** {@inheritDoc} */
    @Override
    @JsonProperty("aCLTemplateClient")
    public ACLTemplateClient<ACLTemplate> getACLTemplateClient() {
        return aclTemplateClient;
    }

    /** {@inheritDoc} */
    @Override
    @JsonProperty("vPNClient")
    public VPNClient<VPN> getVPNClient() {
        return vpnClient;
    }

    /** {@inheritDoc} */
    @Override
    @JsonProperty("bGPPeeringClient")
    public BGPPeeringClient<BGPPeering> getBGPClient() {
        return bgpClient;
    }
}
