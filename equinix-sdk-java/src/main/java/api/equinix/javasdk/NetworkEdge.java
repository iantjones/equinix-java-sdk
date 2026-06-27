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
 * The primary entry point for accessing the Equinix Network Edge APIs.
 *
 * <p>Network Edge provides virtual network devices deployed at Equinix data centers,
 * enabling customers to run network functions (routers, firewalls, SD-WAN) without
 * physical hardware. This class offers typed access to all Network Edge resources
 * including virtual devices, SSH users, ACL templates, VPN connections, BGP peerings,
 * device links, public keys, and backups.</p>
 *
 * <p>All resource accessors use lazy initialization — internal clients are created on first access
 * and reused for subsequent calls.</p>
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * BasicEquinixCredentials credentials = new BasicEquinixCredentials("clientId", "clientSecret");
 * NetworkEdge networkEdge = new NetworkEdge(credentials);
 *
 * // List all virtual devices
 * PaginatedList<Device> devices = networkEdge.devices().list();
 *
 * // Create a virtual device with the fluent builder
 * Device device = networkEdge.devices()
 *     .define("my-router")
 *     .withAccountNumber(accountNumber)
 *     .inMetro(MetroCode.SV)
 *     .withDeviceType("CSR1000V")
 *     .create();
 *
 * // Manage SSH users and ACL templates
 * PaginatedList<SSHUser> sshUsers = networkEdge.sshUsers().list();
 * PaginatedList<ACLTemplate> templates = networkEdge.aclTemplates().list();
 * }</pre>
 *
 * @author ianjones
 * @version $Id: $Id
 * @see api.equinix.javasdk.core.auth.BasicEquinixCredentials
 * @see api.equinix.javasdk.networkedge.client.Devices
 * @see api.equinix.javasdk.networkedge.client.SSHUsers
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
     * Creates a new Network Edge client using the provided credentials.
     * Authentication occurs automatically on the first API call.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     */
    public NetworkEdge(EquinixCredentials equinixCredentials) {
        this(equinixCredentials, false);
    }

    /**
     * Creates a new Network Edge client with optional sandbox mode.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     * @param isSandBoxed {@code true} to use the sandbox environment for testing; {@code false} for production
     */
    public NetworkEdge(EquinixCredentials equinixCredentials, boolean isSandBoxed) {
        super(equinixCredentials, isSandBoxed);

        String paramFile = "json/apiParams_NetworkEdge.json";
        equinixClient.appendApiParams(paramFile);

        this.networkEdgeConfig = new NetworkEdgeConfigImpl(equinixClient);
    }

    /**
     * Returns the client for Network Edge setup and provisioning operations.
     * Provides access to account settings, metro availability, DNS configuration,
     * license agreements, and pricing for virtual network devices.
     *
     * @return the {@link Setup} client for managing setup-related resources
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
     * Returns the client for managing virtual network devices and device types.
     * Devices are virtual appliances (routers, firewalls, SD-WAN) running at Equinix
     * data centers. Supports creation via fluent builders, lifecycle management,
     * and querying available device types.
     *
     * @return the {@link Devices} client for CRUD operations on virtual devices
     */
    public Devices devices() {
        if (this.devices == null) {
            this.devices = new DevicesImpl(this.networkEdgeConfig.getDevicesClient(),
                    this.networkEdgeConfig.getDeviceTypesClient(), this);
        }
        return this.devices;
    }

    /**
     * Returns the client for managing SSH public keys.
     * Public keys are uploaded and associated with virtual devices for
     * secure SSH access without password-based authentication.
     *
     * @return the {@link PublicKeys} client for managing SSH public keys
     */
    public PublicKeys publicKeys() {
        if (this.publicKeys == null) {
            this.publicKeys = new PublicKeysImpl(this.networkEdgeConfig.getPublicKeysClient(), this);
        }
        return this.publicKeys;
    }

    /**
     * Returns the client for managing device links between virtual network devices.
     * Device links create logical connections between devices, enabling multi-device
     * topologies and high-availability configurations.
     *
     * @return the {@link DeviceLinks} client for creating and managing device links
     */
    public DeviceLinks deviceLinks() {
        if (this.deviceLinks == null) {
            this.deviceLinks = new DeviceLinksImpl(this.networkEdgeConfig.getDeviceLinksClient(), this);
        }
        return this.deviceLinks;
    }

    /**
     * Returns the client for managing SSH user accounts on virtual devices.
     * SSH users define the credentials used to access virtual devices for
     * configuration and management via SSH.
     *
     * @return the {@link SSHUsers} client for managing SSH user accounts
     */
    public SSHUsers sshUsers() {
        if (this.sshUsers == null) {
            this.sshUsers = new SSHUsersImpl(this.networkEdgeConfig.getSSHUserClient(), this);
        }
        return this.sshUsers;
    }

    /**
     * Returns the client for managing Access Control List (ACL) templates.
     * ACL templates define network access rules that can be applied to
     * virtual devices to control inbound and outbound traffic.
     *
     * @return the {@link ACLTemplates} client for CRUD operations on ACL templates
     */
    public ACLTemplates aclTemplates() {
        if (this.aclTemplates == null) {
            this.aclTemplates = new ACLTemplatesImpl(this.networkEdgeConfig.getACLTemplateClient(), this);
        }
        return this.aclTemplates;
    }

    /**
     * Returns the client for managing VPN connections on virtual devices.
     * VPNs establish secure, encrypted tunnels between virtual devices
     * and remote network endpoints.
     *
     * @return the {@link VPNs} client for creating and managing VPN connections
     */
    public VPNs vpns() {
        if (this.vpns == null) {
            this.vpns = new VPNsImpl(this.networkEdgeConfig.getVPNClient(), this);
        }
        return this.vpns;
    }

    /**
     * Returns the client for managing BGP peering sessions on virtual devices.
     * BGP peerings enable dynamic route exchange between virtual devices
     * and neighboring routers for network reachability.
     *
     * @return the {@link BGPPeerings} client for configuring BGP peering sessions
     */
    public BGPPeerings bgpPeerings() {
        if (this.bgpPeerings == null) {
            this.bgpPeerings = new BGPPeeringsImpl(this.networkEdgeConfig.getBGPClient(), this);
        }
        return this.bgpPeerings;
    }

    /**
     * Returns the client for managing device configuration backups.
     * Backups capture the running configuration of virtual devices,
     * enabling point-in-time recovery and configuration rollback.
     *
     * @return the {@link Backups} client for managing device backups
     */
    public Backups backups() {
        if (this.backups == null) {
            this.backups = new BackupsImpl(this.networkEdgeConfig.getBackupClient(), this);
        }
        return this.backups;
    }
}
