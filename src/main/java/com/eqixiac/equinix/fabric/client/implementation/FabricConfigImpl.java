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

package com.eqixiac.equinix.fabric.client.implementation;

import com.eqixiac.equinix.core.client.Config;
import com.eqixiac.equinix.core.client.EquinixClient;
import com.eqixiac.equinix.fabric.client.internal.implementation.*;
import lombok.Getter;

/**
 *
 * @author ianjones
 */
@Getter
public class FabricConfigImpl extends Config {

    private final MetroClientImpl metrosClient;

    private final ServiceTokenClientImpl serviceTokensClient;

    private final PortClientImpl portsClient;

    private final PortStatisticClientImpl portStatisticsClient;

    private final PricingClientImpl pricingClient;

    private final ConnectionClientImpl connectionsClient;

    private final MetricClientImpl metricsClient;

    private final ServiceProfileClientImpl serviceProfilesClient;

    private final CloudRouterClientImpl cloudRoutersClient;

    private final CloudRouterPackageClientImpl cloudRouterPackagesClient;

    private final CloudRouterCommandClientImpl cloudRouterCommandsClient;

    private final RoutingProtocolClientImpl routingProtocolsClient;

    private final RouteTableEntryClientImpl connectionRoutesClient;

    private final RouteTableEntryClientImpl cloudRouterRoutesClient;

    private final RouteFilterClientImpl routeFiltersClient;

    private final EiaServiceClientImpl eiaServicesClient;

    private final RouteFilterRuleClientImpl routeFilterRulesClient;

    private final RouteAggregationClientImpl routeAggregationsClient;

    private final RouteAggregationRuleClientImpl routeAggregationRulesClient;

    private final NetworkClientImpl networksClient;

    private final StreamClientImpl streamsClient;

    private final StreamSubscriptionClientImpl streamSubscriptionsClient;

    private final PrecisionTimeClientImpl precisionTimesClient;

    private final CloudEventClientImpl cloudEventsClient;

    private final MarketplaceSubscriptionClientImpl marketplaceSubscriptionsClient;

    private final HealthClientImpl healthClient;

    private final IpBlockClientImpl ipBlocksClient;

    private final PortPackageClientImpl portPackagesClient;

    private final StreamAlertRuleClientImpl streamAlertRulesClient;

    private final StreamAssetClientImpl streamAssetsClient;

    private final AgentClientImpl agentsClient;

    private final AgentTemplateClientImpl agentTemplatesClient;

    private final CompanyProfileClientImpl companyProfilesClient;

    private final TagClientImpl tagsClient;

    public FabricConfigImpl(EquinixClient equinixClient) {
        super(equinixClient);
        this.metrosClient = new MetroClientImpl(this);
        this.serviceTokensClient = new ServiceTokenClientImpl(this);
        this.portsClient = new PortClientImpl(this);
        this.portStatisticsClient = new PortStatisticClientImpl(this);
        this.connectionsClient = new ConnectionClientImpl(this);
        this.metricsClient = new MetricClientImpl(this);
        this.pricingClient = new PricingClientImpl(this);
        this.serviceProfilesClient = new ServiceProfileClientImpl(this);
        this.cloudRoutersClient = new CloudRouterClientImpl(this);
        this.cloudRouterPackagesClient = new CloudRouterPackageClientImpl(this);
        this.cloudRouterCommandsClient = new CloudRouterCommandClientImpl(this);
        this.routingProtocolsClient = new RoutingProtocolClientImpl(this);
        this.connectionRoutesClient = new RouteTableEntryClientImpl(this, "Connections");
        this.cloudRouterRoutesClient = new RouteTableEntryClientImpl(this, "CloudRouters");
        this.routeFiltersClient = new RouteFilterClientImpl(this);
        this.eiaServicesClient = new EiaServiceClientImpl(this);
        this.routeFilterRulesClient = new RouteFilterRuleClientImpl(this);
        this.routeAggregationsClient = new RouteAggregationClientImpl(this);
        this.routeAggregationRulesClient = new RouteAggregationRuleClientImpl(this);
        this.networksClient = new NetworkClientImpl(this);
        this.streamsClient = new StreamClientImpl(this);
        this.streamSubscriptionsClient = new StreamSubscriptionClientImpl(this);
        this.precisionTimesClient = new PrecisionTimeClientImpl(this);
        this.cloudEventsClient = new CloudEventClientImpl(this);
        this.marketplaceSubscriptionsClient = new MarketplaceSubscriptionClientImpl(this);
        this.healthClient = new HealthClientImpl(this);
        this.ipBlocksClient = new IpBlockClientImpl(this);
        this.portPackagesClient = new PortPackageClientImpl(this);
        this.streamAlertRulesClient = new StreamAlertRuleClientImpl(this);
        this.streamAssetsClient = new StreamAssetClientImpl(this);
        this.agentsClient = new AgentClientImpl(this);
        this.agentTemplatesClient = new AgentTemplateClientImpl(this);
        this.companyProfilesClient = new CompanyProfileClientImpl(this);
        this.tagsClient = new TagClientImpl(this);
    }
}