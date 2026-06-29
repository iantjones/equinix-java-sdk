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
 *
 * @author ianjones
 */
public interface FabricConfig {

    MetroClient<Metro> getMetrosClient();

    ServiceTokenClient<ServiceToken> getServiceTokensClient();

    PortClient<Port> getPortsClient();

    PortStatisticClient<PortStatistic> getPortStatisticsClient();

    ConnectionClient<Connection> getConnectionsClient();

    MetricClient<Metric> getMetricsClient();

    PricingClient<Pricing> getPricingClient();

    ServiceProfileClient<ServiceProfile> getServiceProfilesClient();

    CloudRouterClient<CloudRouter> getCloudRoutersClient();

    CloudRouterPackageClient<CloudRouterPackage> getCloudRouterPackagesClient();

    CloudRouterCommandClient<CloudRouterCommand> getCloudRouterCommandsClient();

    RoutingProtocolClient<RoutingProtocol> getRoutingProtocolsClient();

    RouteTableEntryClient<RouteTableEntry> getConnectionRoutesClient();

    RouteTableEntryClient<RouteTableEntry> getCloudRouterRoutesClient();

    RouteFilterClient<RouteFilter> getRouteFiltersClient();

    EiaServiceClient<EiaService> getEiaServicesClient();

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

    IpBlockClient<IpBlock> getIpBlocksClient();

    PortPackageClient<PortPackage> getPortPackagesClient();

    StreamAlertRuleClient<StreamAlertRule> getStreamAlertRulesClient();

    StreamAssetClient<StreamAsset> getStreamAssetsClient();

    AgentClient<Agent> getAgentsClient();

    AgentTemplateClient<AgentTemplate> getAgentTemplatesClient();

    CompanyProfileClient<CompanyProfile> getCompanyProfilesClient();

    TagClient<Tag> getTagsClient();
}
