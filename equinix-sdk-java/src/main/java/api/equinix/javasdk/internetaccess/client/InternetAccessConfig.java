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

package api.equinix.javasdk.internetaccess.client;

import api.equinix.javasdk.internetaccess.client.internal.AccountClient;
import api.equinix.javasdk.internetaccess.client.internal.CabinetClient;
import api.equinix.javasdk.internetaccess.client.internal.CageClient;
import api.equinix.javasdk.internetaccess.client.internal.ConnectionServiceClient;
import api.equinix.javasdk.internetaccess.client.internal.CustomerRouteConfigurationClient;
import api.equinix.javasdk.internetaccess.client.internal.DedicatedBandwidthConfigurationClient;
import api.equinix.javasdk.internetaccess.client.internal.DedicatedPortDefaultConfigurationClient;
import api.equinix.javasdk.internetaccess.client.internal.IbxClient;
import api.equinix.javasdk.internetaccess.client.internal.IbxV1Client;
import api.equinix.javasdk.internetaccess.client.internal.InternetAccessServiceClient;
import api.equinix.javasdk.internetaccess.client.internal.OperationalUnitClient;
import api.equinix.javasdk.internetaccess.client.internal.OrderClient;
import api.equinix.javasdk.internetaccess.client.internal.PatchPanelClient;
import api.equinix.javasdk.internetaccess.client.internal.PortConfigurationClient;
import api.equinix.javasdk.internetaccess.client.internal.PriceClient;
import api.equinix.javasdk.internetaccess.client.internal.PurchaseOrderClient;
import api.equinix.javasdk.internetaccess.client.internal.RoutingConfigurationClient;
import api.equinix.javasdk.internetaccess.client.internal.SignaturePolicyClient;
import api.equinix.javasdk.internetaccess.client.internal.TermsClient;
import api.equinix.javasdk.internetaccess.client.internal.VirtualBandwidthConfigurationClient;
import api.equinix.javasdk.internetaccess.client.internal.VirtualConnectionDefaultConfigurationClient;

public interface InternetAccessConfig {

    InternetAccessServiceClient getInternetAccessServiceClient();

    IbxClient getIbxClient();

    IbxV1Client getIbxV1Client();

    PriceClient getPriceClient();

    AccountClient getAccountClient();

    TermsClient getTermsClient();

    OperationalUnitClient getOperationalUnitClient();

    SignaturePolicyClient getSignaturePolicyClient();

    RoutingConfigurationClient getRoutingConfigurationClient();

    DedicatedBandwidthConfigurationClient getDedicatedBandwidthConfigurationClient();

    VirtualBandwidthConfigurationClient getVirtualBandwidthConfigurationClient();

    VirtualConnectionDefaultConfigurationClient getVirtualConnectionDefaultConfigurationClient();

    CustomerRouteConfigurationClient getCustomerRouteConfigurationClient();

    DedicatedPortDefaultConfigurationClient getDedicatedPortDefaultConfigurationClient();

    PortConfigurationClient getPortConfigurationClient();

    PurchaseOrderClient getPurchaseOrderClient();

    OrderClient getOrderClient();

    CageClient getCageClient();

    CabinetClient getCabinetClient();

    PatchPanelClient getPatchPanelClient();

    ConnectionServiceClient getConnectionServiceClient();
}
