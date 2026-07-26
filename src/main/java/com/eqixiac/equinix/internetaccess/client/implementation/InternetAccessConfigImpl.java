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

package com.eqixiac.equinix.internetaccess.client.implementation;

import com.eqixiac.equinix.core.client.Config;
import com.eqixiac.equinix.core.client.EquinixClient;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.AccountClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.CabinetClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.CageClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.ConnectionServiceClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.CustomerRouteConfigurationClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.DedicatedBandwidthConfigurationClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.DedicatedPortDefaultConfigurationClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.IbxClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.IbxV1ClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.InternetAccessServiceClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.OperationalUnitClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.OrderClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.PatchPanelClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.PortConfigurationClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.PriceClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.PurchaseOrderClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.RoutingConfigurationClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.SignaturePolicyClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.TermsClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.VirtualBandwidthConfigurationClientImpl;
import com.eqixiac.equinix.internetaccess.client.internal.implementation.VirtualConnectionDefaultConfigurationClientImpl;
import lombok.Getter;

@Getter
public class InternetAccessConfigImpl extends Config {

    private final InternetAccessServiceClientImpl internetAccessServiceClient;

    private final IbxClientImpl ibxClient;

    private final IbxV1ClientImpl ibxV1Client;

    private final PriceClientImpl priceClient;

    private final AccountClientImpl accountClient;

    private final TermsClientImpl termsClient;

    private final OperationalUnitClientImpl operationalUnitClient;

    private final SignaturePolicyClientImpl signaturePolicyClient;

    private final RoutingConfigurationClientImpl routingConfigurationClient;

    private final DedicatedBandwidthConfigurationClientImpl dedicatedBandwidthConfigurationClient;

    private final VirtualBandwidthConfigurationClientImpl virtualBandwidthConfigurationClient;

    private final VirtualConnectionDefaultConfigurationClientImpl virtualConnectionDefaultConfigurationClient;

    private final CustomerRouteConfigurationClientImpl customerRouteConfigurationClient;

    private final DedicatedPortDefaultConfigurationClientImpl dedicatedPortDefaultConfigurationClient;

    private final PortConfigurationClientImpl portConfigurationClient;

    private final PurchaseOrderClientImpl purchaseOrderClient;

    private final OrderClientImpl orderClient;

    private final CageClientImpl cageClient;

    private final CabinetClientImpl cabinetClient;

    private final PatchPanelClientImpl patchPanelClient;

    private final ConnectionServiceClientImpl connectionServiceClient;

    public InternetAccessConfigImpl(EquinixClient equinixClient) {
        super(equinixClient);
        this.internetAccessServiceClient = new InternetAccessServiceClientImpl(this);
        this.ibxClient = new IbxClientImpl(this);
        this.ibxV1Client = new IbxV1ClientImpl(this);
        this.priceClient = new PriceClientImpl(this);
        this.accountClient = new AccountClientImpl(this);
        this.termsClient = new TermsClientImpl(this);
        this.operationalUnitClient = new OperationalUnitClientImpl(this);
        this.signaturePolicyClient = new SignaturePolicyClientImpl(this);
        this.routingConfigurationClient = new RoutingConfigurationClientImpl(this);
        this.dedicatedBandwidthConfigurationClient = new DedicatedBandwidthConfigurationClientImpl(this);
        this.virtualBandwidthConfigurationClient = new VirtualBandwidthConfigurationClientImpl(this);
        this.virtualConnectionDefaultConfigurationClient = new VirtualConnectionDefaultConfigurationClientImpl(this);
        this.customerRouteConfigurationClient = new CustomerRouteConfigurationClientImpl(this);
        this.dedicatedPortDefaultConfigurationClient = new DedicatedPortDefaultConfigurationClientImpl(this);
        this.portConfigurationClient = new PortConfigurationClientImpl(this);
        this.purchaseOrderClient = new PurchaseOrderClientImpl(this);
        this.orderClient = new OrderClientImpl(this);
        this.cageClient = new CageClientImpl(this);
        this.cabinetClient = new CabinetClientImpl(this);
        this.patchPanelClient = new PatchPanelClientImpl(this);
        this.connectionServiceClient = new ConnectionServiceClientImpl(this);
    }
}
