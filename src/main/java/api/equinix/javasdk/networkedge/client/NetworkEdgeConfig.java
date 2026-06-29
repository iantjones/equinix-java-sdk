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
import api.equinix.javasdk.networkedge.model.VPN;

/**
 *
 * @author ianjones
 */
public interface NetworkEdgeConfig {

    MetroClient<Metro> getMetrosClient();

    AccountClient<Account> getAccountsClient();

    AgreementClient getAgreementsClient();

    DeviceClient<Device> getDevicesClient();

    DeviceTypeClient<DeviceType> getDeviceTypesClient();

    PublicKeyClient<PublicKey> getPublicKeysClient();
    
    DeviceLinkClient<DeviceLink> getDeviceLinksClient();

    ACLTemplateClient<ACLTemplate> getACLTemplateClient();

    VPNClient<VPN> getVPNClient();

    BGPPeeringClient<BGPPeering> getBGPClient();

    BackupClient<Backup> getBackupClient();

    PricingClient getPricingClient();

    FilesClient getFilesClient();

    NotificationClient getNotificationClient();
}
