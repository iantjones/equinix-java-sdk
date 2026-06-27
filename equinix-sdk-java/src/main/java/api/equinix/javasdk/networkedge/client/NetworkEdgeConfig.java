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

package api.equinix.javasdk.networkedge.client;

import api.equinix.javasdk.networkedge.client.internal.*;
import api.equinix.javasdk.networkedge.model.ACLTemplate;
import api.equinix.javasdk.networkedge.model.Account;
import api.equinix.javasdk.networkedge.model.BGPPeering;
import api.equinix.javasdk.networkedge.model.Backup;
import api.equinix.javasdk.networkedge.model.Device;
import api.equinix.javasdk.networkedge.model.DeviceLink;
import api.equinix.javasdk.networkedge.model.DeviceType;
import api.equinix.javasdk.networkedge.model.Metro;
import api.equinix.javasdk.networkedge.model.PublicKey;
import api.equinix.javasdk.networkedge.model.SSHUser;
import api.equinix.javasdk.networkedge.model.VPN;
import api.equinix.javasdk.networkedge.model.implementation.DNSLookup;

/**
 * <p>NetworkEdgeConfig interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface NetworkEdgeConfig {

    /**
     * <p>getMetrosClient.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.internal.MetroClient} object.
     */
    MetroClient<Metro> getMetrosClient();

    /**
     * <p>getAccountsClient.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.internal.AccountClient} object.
     */
    AccountClient<Account> getAccountsClient();

    /**
     * <p>getAgreementsClient.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.internal.AgreementClient} object.
     */
    AgreementClient getAgreementsClient();

    /**
     * <p>getDevicesClient.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.internal.DeviceClient} object.
     */
    DeviceClient<Device> getDevicesClient();

    /**
     * <p>getDeviceTypesClient.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.internal.DeviceTypeClient} object.
     */
    DeviceTypeClient<DeviceType> getDeviceTypesClient();

    /**
     * <p>getPublicKeysClient.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.internal.PublicKeyClient} object.
     */
    PublicKeyClient<PublicKey> getPublicKeysClient();
    
    /**
     * <p>getDeviceLinksClient.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.internal.DeviceLinkClient} object.
     */
    DeviceLinkClient<DeviceLink> getDeviceLinksClient();

    /**
     * <p>getSSHUserClient.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.internal.SSHUserClient} object.
     */
    SSHUserClient<SSHUser> getSSHUserClient();

    /**
     * <p>getACLTemplateClient.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.internal.ACLTemplateClient} object.
     */
    ACLTemplateClient<ACLTemplate> getACLTemplateClient();

    /**
     * <p>getVPNClient.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.internal.VPNClient} object.
     */
    VPNClient<VPN> getVPNClient();

    /**
     * <p>getDNSClient.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.internal.DNSClient} object.
     */
    DNSClient<DNSLookup> getDNSClient();

    /**
     * <p>getBGPClient.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.internal.BGPPeeringClient} object.
     */
    BGPPeeringClient<BGPPeering> getBGPClient();

    /**
     * <p>getBackupClient.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.internal.BackupClient} object.
     */
    BackupClient<Backup> getBackupClient();

    /**
     * <p>getPricingClient.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.client.internal.PricingClient} object.
     */
    PricingClient getPricingClient();
}
