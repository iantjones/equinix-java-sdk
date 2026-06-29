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

import api.equinix.javasdk.core.http.response.PaginatedList;
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

/**
 * Client interface for the Equinix Internet Access (EIA) v1 product / attribute configuration
 * lookups — the read-only endpoints under {@code /internetAccess/v1} that return the allowed and
 * default configuration values (routing, bandwidth, connection, port) for ordering an EIA service.
 */
public interface InternetAccessProductConfigurations {

    /**
     * Returns the allowed routing-protocol configurations for the given use case
     * ({@code GET /internetAccess/v1/routingProtocolConfigurations}).
     *
     * @param useCase the intended use case
     * @return a paginated list of allowed routing-protocol configurations
     */
    PaginatedList<RoutingProtocolConfiguration> routingConfigurations(UseCase useCase);

    /**
     * Returns the allowed routing-protocol configurations for the given use case and redundancy
     * ({@code GET /internetAccess/v1/routingProtocolConfigurations}).
     *
     * @param useCase the intended use case
     * @param type the redundancy configuration, or {@code null} for any
     * @return a paginated list of allowed routing-protocol configurations
     */
    PaginatedList<RoutingProtocolConfiguration> routingConfigurations(UseCase useCase, Redundancy type);

    /**
     * Returns the allowed dedicated-bandwidth configurations for the given use case
     * ({@code GET /internetAccess/v1/dedicatedBandwidthConfigurations}).
     *
     * @param useCase the intended use case
     * @return a paginated list of allowed dedicated-bandwidth configurations
     */
    PaginatedList<DedicatedBandwidthConfiguration> dedicatedBandwidthConfigurations(UseCase useCase);

    /**
     * Returns the allowed dedicated-bandwidth configurations for the given use case, billing model
     * and physical-port speed
     * ({@code GET /internetAccess/v1/dedicatedBandwidthConfigurations}).
     *
     * @param useCase the intended use case
     * @param billing the billing model, or {@code null} for any
     * @param physicalPortSpeed the physical-port speed in Mbps, or {@code null} for any
     * @return a paginated list of allowed dedicated-bandwidth configurations
     */
    PaginatedList<DedicatedBandwidthConfiguration> dedicatedBandwidthConfigurations(UseCase useCase, BillingType billing, Integer physicalPortSpeed);

    /**
     * Returns the allowed virtual-bandwidth configurations for the given use case
     * ({@code GET /internetAccess/v1/virtualBandwidthConfigurations}).
     *
     * @param useCase the intended use case
     * @return a paginated list of allowed virtual-bandwidth configurations
     */
    PaginatedList<VirtualBandwidthConfiguration> virtualBandwidthConfigurations(UseCase useCase);

    /**
     * Returns the allowed virtual-bandwidth configurations for the given use case and billing model
     * ({@code GET /internetAccess/v1/virtualBandwidthConfigurations}).
     *
     * @param useCase the intended use case
     * @param billing the billing model, or {@code null} for any
     * @return a paginated list of allowed virtual-bandwidth configurations
     */
    PaginatedList<VirtualBandwidthConfiguration> virtualBandwidthConfigurations(UseCase useCase, BillingType billing);

    /**
     * Returns the default virtual-connection configurations available in the given IBX
     * ({@code GET /internetAccess/v1/virtualConnectionDefaultConfigurations}).
     *
     * @param ibx the IBX data-center code
     * @return a paginated list of default virtual-connection configurations
     */
    PaginatedList<VirtualConnectionDefaultConfiguration> virtualConnectionDefaultConfigurations(String ibx);

    /**
     * Returns the default virtual-connection configurations available in the given IBX and metro
     * ({@code GET /internetAccess/v1/virtualConnectionDefaultConfigurations}).
     *
     * @param ibx the IBX data-center code
     * @param metroCode the metro code, or {@code null} for any
     * @return a paginated list of default virtual-connection configurations
     */
    PaginatedList<VirtualConnectionDefaultConfiguration> virtualConnectionDefaultConfigurations(String ibx, String metroCode);

    /**
     * Returns the allowed customer-route configurations for the given use case
     * ({@code GET /internetAccess/v1/customerRouteConfigurations}).
     *
     * @param useCase the intended use case
     * @return a paginated list of allowed customer-route configurations
     */
    PaginatedList<CustomerRouteConfiguration> customerRouteConfigurations(UseCase useCase);

    /**
     * Returns the allowed customer-route configurations for the given use case, redundancy and
     * routing-protocol type ({@code GET /internetAccess/v1/customerRouteConfigurations}).
     *
     * @param useCase the intended use case
     * @param type the redundancy configuration, or {@code null} for any
     * @param routingProtocolType the routing-protocol type, or {@code null} for any
     * @return a paginated list of allowed customer-route configurations
     */
    PaginatedList<CustomerRouteConfiguration> customerRouteConfigurations(UseCase useCase, Redundancy type, RoutingProtocolType routingProtocolType);

    /**
     * Returns the default dedicated-port configurations available in the given IBX
     * ({@code GET /internetAccess/v1/dedicatedPortDefaultConfigurations}).
     *
     * @param ibx the IBX data-center code
     * @return a paginated list of default dedicated-port configurations
     */
    PaginatedList<DedicatedPortDefaultConfiguration> dedicatedPortDefaultConfigurations(String ibx);

    /**
     * Returns the allowed port configurations for the given IBX and use case
     * ({@code GET /internetAccess/v1/portConfigurations}).
     *
     * @param ibx the IBX data-center code
     * @param useCase the intended use case
     * @return a paginated list of allowed port configurations
     */
    PaginatedList<PortConfiguration> portConfigurations(String ibx, UseCase useCase);
}
