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

package api.equinix.javasdk.fabric.client;

import api.equinix.javasdk.fabric.client.internal.*;
import api.equinix.javasdk.fabric.model.*;

/**
 * <p>FabricConfig interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface FabricConfig {

    /**
     * <p>getMetrosClient.</p>
     *
     * @return a {@link MetroClient} object.
     */
    MetroClient<Metro> getMetrosClient();

    /**
     * <p>getServiceTokensClient.</p>
     *
     * @return a {@link ServiceTokenClient} object.
     */
    ServiceTokenClient<ServiceToken> getServiceTokensClient();

    /**
     * <p>getPortsClient.</p>
     *
     * @return a {@link PortClient} object.
     */
    PortClient<Port> getPortsClient();

    /**
     * <p>getPortStatisticsClient.</p>
     *
     * @return a {@link PortStatisticClient} object.
     */
    PortStatisticClient<PortStatistic> getPortStatisticsClient();

    /**
     * <p>getConnectionsClient.</p>
     *
     * @return a {@link ConnectionClient} object.
     */
    ConnectionClient<Connection> getConnectionsClient();

    PricingClient<Pricing> getPricingClient();

    /**
     * <p>getServiceProfilesClient.</p>
     *
     * @return a {@link ServiceProfileClient} object.
     */
    ServiceProfileClient<ServiceProfile> getServiceProfilesClient();

    FabricGatewayClient<FabricGateway> getFabricGatewaysClient();

    GatewayPackageClient<GatewayPackage> getGatewayPackageClient();

    CloudRouterClient<CloudRouter> getCloudRoutersClient();

    CloudRouterPackageClient<CloudRouterPackage> getCloudRouterPackagesClient();

    RoutingProtocolClient<RoutingProtocol> getRoutingProtocolsClient();

    RouteFilterClient<RouteFilter> getRouteFiltersClient();

    RouteFilterRuleClient<RouteFilterRule> getRouteFilterRulesClient();

    RouteAggregationClient<RouteAggregation> getRouteAggregationsClient();

    RouteAggregationRuleClient<RouteAggregationRule> getRouteAggregationRulesClient();

    NetworkClient<Network> getNetworksClient();

    StreamClient<Stream> getStreamsClient();

    StreamSubscriptionClient<StreamSubscription> getStreamSubscriptionsClient();

    PrecisionTimeClient<PrecisionTime> getPrecisionTimesClient();

    CloudEventClient<CloudEvent> getCloudEventsClient();

    MarketplaceSubscriptionClient<MarketplaceSubscription> getMarketplaceSubscriptionsClient();

    HealthClient<HealthStatus> getHealthClient();
}
