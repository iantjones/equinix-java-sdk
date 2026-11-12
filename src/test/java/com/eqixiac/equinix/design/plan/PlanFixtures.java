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


package com.eqixiac.equinix.design.plan;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.wizard.enums.ConnectionPurpose;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedCloudRouter;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedInterconnect;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedRoutingProtocol;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.enums.GatewayPackageCode;
import com.eqixiac.equinix.fabric.enums.RoutingProtocolType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;

import java.util.List;

/**
 * A small hand-built plan with logical keys: one Cloud Router in DC, one AWS provider connection on
 * it, its DIRECT and BGP routing protocols, and (optionally) an Interconnect. Deployment {@code demo},
 * prefix {@code FCR}, project {@link #PROJECT}.
 */
final class PlanFixtures {

    static final String PROJECT = "626329c0-1111-4222-8333-444455556666";
    static final MetroId DC = MetroId.of(MetroCode.DC);
    static final String ROUTER_KEY = "router/DC";
    static final String CONN_KEY = "connection/DC/aws";
    static final String DIRECT_KEY = CONN_KEY + "/rp/DIRECT";
    static final String BGP_KEY = CONN_KEY + "/rp/BGP";

    private PlanFixtures() {
    }

    static PlannedCloudRouter router() {
        return PlannedCloudRouter.builder().key(ROUTER_KEY).name("FCR-DC").metroId(DC)
                .packageCode(GatewayPackageCode.STANDARD).projectId(PROJECT)
                .notificationEmails(List.of("noc@example.com")).build();
    }

    static PlannedConnection connection() {
        return PlannedConnection.builder().key(CONN_KEY).name("FCR-DC-to-aws")
                .connectionType(ConnectionType.IP_VC).purpose(ConnectionPurpose.PROVIDER).bandwidthMbps(1000)
                .aSideMetro(DC).aSideRouterName("FCR-DC").zSideServiceProfileUuid("sp-aws")
                .zSideProviderLabel("AWS").zSideSellerRegion("us-east-1").zSideCloudType(CloudProviderType.AWS)
                .projectId(PROJECT).notificationEmails(List.of("noc@example.com")).build();
    }

    static List<PlannedRoutingProtocol> protocols() {
        return List.of(
                PlannedRoutingProtocol.builder().key(DIRECT_KEY).type(RoutingProtocolType.DIRECT)
                        .name("FCR-DC-to-aws-DIRECT").connectionName("FCR-DC-to-aws")
                        .equinixIfaceIpv4("10.100.0.1/30").build(),
                PlannedRoutingProtocol.builder().key(BGP_KEY).type(RoutingProtocolType.BGP)
                        .name("FCR-DC-to-aws-BGP").connectionName("FCR-DC-to-aws")
                        .customerPeerIpv4("10.100.0.2/30").equinixPeerIpv4("10.100.0.1/30")
                        .customerAsn(65100L).bfdEnabled(true).bfdInterval(300).build());
    }

    static DeploymentPlan plan(FabricGateway fabric) {
        return DeploymentPlan.builder()
                .deploymentName("demo").namePrefix("FCR").projectId(PROJECT)
                .cloudRouters(List.of(router()))
                .providerConnections(List.of(connection()))
                .backboneLinks(List.of())
                .routingProtocols(protocols())
                .interconnects(List.of(PlannedInterconnect.builder().key("interconnect/DC/core").name("core")
                        .metro(DC).build()))
                .valid(true).validationErrors(List.of())
                .fabric(fabric)
                .build();
    }
}
