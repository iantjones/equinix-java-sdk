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
import api.equinix.javasdk.networkedge.client.ACLTemplates;
import api.equinix.javasdk.networkedge.client.BGPPeerings;
import api.equinix.javasdk.networkedge.client.Backups;
import api.equinix.javasdk.networkedge.client.DeviceLinks;
import api.equinix.javasdk.networkedge.client.PublicKeys;
import api.equinix.javasdk.networkedge.client.SSHUsers;
import api.equinix.javasdk.networkedge.client.Setup;
import api.equinix.javasdk.networkedge.client.VPNs;
import api.equinix.javasdk.networkedge.client.implementation.ACLTemplatesImpl;
import api.equinix.javasdk.networkedge.client.implementation.BGPPeeringsImpl;
import api.equinix.javasdk.networkedge.client.implementation.BackupsImpl;
import api.equinix.javasdk.networkedge.client.implementation.DeviceLinksImpl;
import api.equinix.javasdk.networkedge.client.implementation.DevicesImpl;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.Devices;
import api.equinix.javasdk.networkedge.client.NetworkEdgeConfig;
import api.equinix.javasdk.networkedge.client.implementation.PublicKeysImpl;
import api.equinix.javasdk.networkedge.client.implementation.SSHUsersImpl;
import api.equinix.javasdk.networkedge.client.implementation.SetupImpl;
import api.equinix.javasdk.networkedge.client.implementation.VPNsImpl;

/**
 * The primary entry point for accessing Network Edge APIs in the Java SDK.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public final class NetworkEdge extends EquinixClient implements Service {

    private Setup setup;
    private Devices devices;
    private PublicKeys publicKeys;
    private DeviceLinks deviceLinks;
    private SSHUsers sshUsers;
    private ACLTemplates aclTemplates;
    private VPNs vpns;
    private BGPPeerings bgpPeerings;
    private Backups backups;

    final private NetworkEdgeConfig networkEdgeConfig;

    /**
     * <p>Constructor for NetworkEdge.</p>
     *
     * @param equinixCredentials a {@link api.equinix.javasdk.core.auth.EquinixCredentials} object.
     */
    public NetworkEdge(EquinixCredentials equinixCredentials) {
        this(equinixCredentials, false);
    }

    /**
     * <p>Constructor for NetworkEdge.</p>
     *
     * @param equinixCredentials a {@link api.equinix.javasdk.core.auth.EquinixCredentials} object.
     * @param isSandBoxed a boolean.
     */
    public NetworkEdge(EquinixCredentials equinixCredentials, boolean isSandBoxed) {
        super(equinixCredentials, isSandBoxed);

        String paramFile = "json/apiParams_NetworkEdge.json";
        equinixClient.appendApiParams(paramFile);

        this.networkEdgeConfig = new NetworkEdgeConfigImpl(equinixClient);
    }

    /**
     * <p>setup.</p>
     *
     * @return {@link api.equinix.javasdk.networkedge.client.Setup}
     * the entry point for accessing setup related operations such as agreements and pricing.
     */
    public Setup setup() {
        if (this.setup == null) {
            this.setup = new SetupImpl(this.networkEdgeConfig.getAccountsClient(),
                    this.networkEdgeConfig.getMetrosClient(), this.networkEdgeConfig.getDNSClient(),
                    this.networkEdgeConfig.getAgreementsClient(), this.networkEdgeConfig.getPricingClient(), this);
        }
        return this.setup;
    }

    /**
     * <p>devices.</p>
     *
     * @return {@link api.equinix.javasdk.networkedge.client.Devices}
     * the entry point for accessing device related operations such as create, update, and restore.
     */
    public Devices devices() {
        if (this.devices == null) {
            this.devices = new DevicesImpl(this.networkEdgeConfig.getDevicesClient(),
                    this.networkEdgeConfig.getDeviceTypesClient(), this);
        }
        return this.devices;
    }

    /**
     * <p>publicKeys.</p>
     *
     * @return {@link api.equinix.javasdk.networkedge.client.PublicKeys}
     * the entry point for accessing public key related operations.
     */
    public PublicKeys publicKeys() {
        if (this.publicKeys == null) {
            this.publicKeys = new PublicKeysImpl(this.networkEdgeConfig.getPublicKeysClient(), this);
        }
        return this.publicKeys;
    }

    /**
     * <p>deviceLinks.</p>
     *
     * @return {@link api.equinix.javasdk.networkedge.client.DeviceLinks}
     * the entry point for accessing device link related operations.
     */
    public DeviceLinks deviceLinks() {
        if (this.deviceLinks == null) {
            this.deviceLinks = new DeviceLinksImpl(this.networkEdgeConfig.getDeviceLinksClient(), this);
        }
        return this.deviceLinks;
    }

    /**
     * <p>sshUsers.</p>
     *
     * @return {@link api.equinix.javasdk.networkedge.client.SSHUsers}
     * the entry point for accessing ssh user related operations.
     */
    public SSHUsers sshUsers() {
        if (this.sshUsers == null) {
            this.sshUsers = new SSHUsersImpl(this.networkEdgeConfig.getSSHUserClient(), this);
        }
        return this.sshUsers;
    }

    /**
     * <p>aclTemplates.</p>
     *
     * @return {@link api.equinix.javasdk.networkedge.client.ACLTemplates}
     * the entry point for accessing acl template related operations.
     */
    public ACLTemplates aclTemplates() {
        if (this.aclTemplates == null) {
            this.aclTemplates = new ACLTemplatesImpl(this.networkEdgeConfig.getACLTemplateClient(), this);
        }
        return this.aclTemplates;
    }

    /**
     * <p>vpns.</p>
     *
     * @return {@link api.equinix.javasdk.networkedge.client.VPNs}
     * the entry point for accessing vpn related operations.
     */
    public VPNs vpns() {
        if (this.vpns == null) {
            this.vpns = new VPNsImpl(this.networkEdgeConfig.getVPNClient(), this);
        }
        return this.vpns;
    }

    /**
     * <p>bgpPeerings.</p>
     *
     * @return {@link api.equinix.javasdk.networkedge.client.BGPPeerings}
     * the entry point for accessing bgp peering related operations.
     */
    public BGPPeerings bgpPeerings() {
        if (this.bgpPeerings == null) {
            this.bgpPeerings = new BGPPeeringsImpl(this.networkEdgeConfig.getBGPClient(), this);
        }
        return this.bgpPeerings;
    }

    /**
     * <p>backups.</p>
     *
     * @return {@link api.equinix.javasdk.networkedge.client.Backups}
     * the entry point for accessing backup related operations.
     */
    public Backups backups() {
        if (this.backups == null) {
            this.backups = new BackupsImpl(this.networkEdgeConfig.getBackupClient(), this);
        }
        return this.backups;
    }
}
