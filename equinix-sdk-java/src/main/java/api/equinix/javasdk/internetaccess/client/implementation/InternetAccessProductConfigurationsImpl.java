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

package api.equinix.javasdk.internetaccess.client.implementation;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.client.InternetAccessProductConfigurations;
import api.equinix.javasdk.internetaccess.client.internal.CustomerRouteConfigurationClient;
import api.equinix.javasdk.internetaccess.client.internal.DedicatedBandwidthConfigurationClient;
import api.equinix.javasdk.internetaccess.client.internal.DedicatedPortDefaultConfigurationClient;
import api.equinix.javasdk.internetaccess.client.internal.PortConfigurationClient;
import api.equinix.javasdk.internetaccess.client.internal.RoutingConfigurationClient;
import api.equinix.javasdk.internetaccess.client.internal.VirtualBandwidthConfigurationClient;
import api.equinix.javasdk.internetaccess.client.internal.VirtualConnectionDefaultConfigurationClient;
import api.equinix.javasdk.internetaccess.enums.BillingType;
import api.equinix.javasdk.internetaccess.enums.Redundancy;
import api.equinix.javasdk.internetaccess.enums.RoutingProtocolType;
import api.equinix.javasdk.internetaccess.enums.UseCase;
import api.equinix.javasdk.internetaccess.model.CustomerRouteConfiguration;
import api.equinix.javasdk.internetaccess.model.DedicatedBandwidthConfiguration;
import api.equinix.javasdk.internetaccess.model.DedicatedPortDefaultConfiguration;
import api.equinix.javasdk.internetaccess.model.PortConfiguration;
import api.equinix.javasdk.internetaccess.model.RoutingProtocolConfiguration;
import api.equinix.javasdk.internetaccess.model.VirtualBandwidthConfiguration;
import api.equinix.javasdk.internetaccess.model.VirtualConnectionDefaultConfiguration;
import api.equinix.javasdk.internetaccess.model.json.CustomerRouteConfigurationJson;
import api.equinix.javasdk.internetaccess.model.json.DedicatedBandwidthConfigurationJson;
import api.equinix.javasdk.internetaccess.model.json.DedicatedPortDefaultConfigurationJson;
import api.equinix.javasdk.internetaccess.model.json.PortConfigurationJson;
import api.equinix.javasdk.internetaccess.model.json.RoutingProtocolConfigurationJson;
import api.equinix.javasdk.internetaccess.model.json.VirtualBandwidthConfigurationJson;
import api.equinix.javasdk.internetaccess.model.json.VirtualConnectionDefaultConfigurationJson;

public class InternetAccessProductConfigurationsImpl implements InternetAccessProductConfigurations {

    private final InternetAccess serviceManager;

    private final RoutingConfigurationClient routingConfigurationClient;

    private final DedicatedBandwidthConfigurationClient dedicatedBandwidthConfigurationClient;

    private final VirtualBandwidthConfigurationClient virtualBandwidthConfigurationClient;

    private final VirtualConnectionDefaultConfigurationClient virtualConnectionDefaultConfigurationClient;

    private final CustomerRouteConfigurationClient customerRouteConfigurationClient;

    private final DedicatedPortDefaultConfigurationClient dedicatedPortDefaultConfigurationClient;

    private final PortConfigurationClient portConfigurationClient;

    public InternetAccessProductConfigurationsImpl(RoutingConfigurationClient routingConfigurationClient,
                                                   DedicatedBandwidthConfigurationClient dedicatedBandwidthConfigurationClient,
                                                   VirtualBandwidthConfigurationClient virtualBandwidthConfigurationClient,
                                                   VirtualConnectionDefaultConfigurationClient virtualConnectionDefaultConfigurationClient,
                                                   CustomerRouteConfigurationClient customerRouteConfigurationClient,
                                                   DedicatedPortDefaultConfigurationClient dedicatedPortDefaultConfigurationClient,
                                                   PortConfigurationClient portConfigurationClient,
                                                   InternetAccess serviceManager) {
        this.routingConfigurationClient = routingConfigurationClient;
        this.dedicatedBandwidthConfigurationClient = dedicatedBandwidthConfigurationClient;
        this.virtualBandwidthConfigurationClient = virtualBandwidthConfigurationClient;
        this.virtualConnectionDefaultConfigurationClient = virtualConnectionDefaultConfigurationClient;
        this.customerRouteConfigurationClient = customerRouteConfigurationClient;
        this.dedicatedPortDefaultConfigurationClient = dedicatedPortDefaultConfigurationClient;
        this.portConfigurationClient = portConfigurationClient;
        this.serviceManager = serviceManager;
    }

    public PaginatedList<RoutingProtocolConfiguration> routingConfigurations(UseCase useCase) {
        return routingConfigurations(useCase, null);
    }

    public PaginatedList<RoutingProtocolConfiguration> routingConfigurations(UseCase useCase, Redundancy type) {
        Page<RoutingProtocolConfiguration, RoutingProtocolConfigurationJson> responsePage =
                this.routingConfigurationClient.list(useCase, type);
        return Utils.toPaginatedList(responsePage, this.routingConfigurationClient, (json, client) -> json);
    }

    public PaginatedList<DedicatedBandwidthConfiguration> dedicatedBandwidthConfigurations(UseCase useCase) {
        return dedicatedBandwidthConfigurations(useCase, null, null);
    }

    public PaginatedList<DedicatedBandwidthConfiguration> dedicatedBandwidthConfigurations(UseCase useCase, BillingType billing, Integer physicalPortSpeed) {
        Page<DedicatedBandwidthConfiguration, DedicatedBandwidthConfigurationJson> responsePage =
                this.dedicatedBandwidthConfigurationClient.list(useCase, billing, physicalPortSpeed);
        return Utils.toPaginatedList(responsePage, this.dedicatedBandwidthConfigurationClient, (json, client) -> json);
    }

    public PaginatedList<VirtualBandwidthConfiguration> virtualBandwidthConfigurations(UseCase useCase) {
        return virtualBandwidthConfigurations(useCase, null);
    }

    public PaginatedList<VirtualBandwidthConfiguration> virtualBandwidthConfigurations(UseCase useCase, BillingType billing) {
        Page<VirtualBandwidthConfiguration, VirtualBandwidthConfigurationJson> responsePage =
                this.virtualBandwidthConfigurationClient.list(useCase, billing);
        return Utils.toPaginatedList(responsePage, this.virtualBandwidthConfigurationClient, (json, client) -> json);
    }

    public PaginatedList<VirtualConnectionDefaultConfiguration> virtualConnectionDefaultConfigurations(String ibx) {
        return virtualConnectionDefaultConfigurations(ibx, null);
    }

    public PaginatedList<VirtualConnectionDefaultConfiguration> virtualConnectionDefaultConfigurations(String ibx, String metroCode) {
        Page<VirtualConnectionDefaultConfiguration, VirtualConnectionDefaultConfigurationJson> responsePage =
                this.virtualConnectionDefaultConfigurationClient.list(ibx, metroCode);
        return Utils.toPaginatedList(responsePage, this.virtualConnectionDefaultConfigurationClient, (json, client) -> json);
    }

    public PaginatedList<CustomerRouteConfiguration> customerRouteConfigurations(UseCase useCase) {
        return customerRouteConfigurations(useCase, null, null);
    }

    public PaginatedList<CustomerRouteConfiguration> customerRouteConfigurations(UseCase useCase, Redundancy type, RoutingProtocolType routingProtocolType) {
        Page<CustomerRouteConfiguration, CustomerRouteConfigurationJson> responsePage =
                this.customerRouteConfigurationClient.list(useCase, type, routingProtocolType);
        return Utils.toPaginatedList(responsePage, this.customerRouteConfigurationClient, (json, client) -> json);
    }

    public PaginatedList<DedicatedPortDefaultConfiguration> dedicatedPortDefaultConfigurations(String ibx) {
        Page<DedicatedPortDefaultConfiguration, DedicatedPortDefaultConfigurationJson> responsePage =
                this.dedicatedPortDefaultConfigurationClient.list(ibx);
        return Utils.toPaginatedList(responsePage, this.dedicatedPortDefaultConfigurationClient, (json, client) -> json);
    }

    public PaginatedList<PortConfiguration> portConfigurations(String ibx, UseCase useCase) {
        Page<PortConfiguration, PortConfigurationJson> responsePage =
                this.portConfigurationClient.list(ibx, useCase);
        return Utils.toPaginatedList(responsePage, this.portConfigurationClient, (json, client) -> json);
    }
}
