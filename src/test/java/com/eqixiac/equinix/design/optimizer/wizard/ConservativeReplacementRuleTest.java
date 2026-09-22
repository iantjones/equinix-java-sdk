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

package com.eqixiac.equinix.design.optimizer.wizard;

import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.ProviderRequirement;
import com.eqixiac.equinix.design.optimizer.model.UserSite;
import com.eqixiac.equinix.design.optimizer.model.WorkloadSpec;
import com.eqixiac.equinix.design.optimizer.wizard.enums.BandwidthStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.enums.CloudToCloudStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.enums.MulticloudLinkRole;
import com.eqixiac.equinix.design.optimizer.wizard.model.ConnectionInputRequirement;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedMulticloudInterconnect;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedRoutingProtocol;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.eqixiac.equinix.design.optimizer.wizard.MulticloudWizardFixtures.DC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The replacement rule of {@code NATIVE_WHEN_AVAILABLE}: a Cloud Router to cloud connection is
 * omitted only when nothing but fully covered cloud-to-cloud workloads uses it. Every fixture is
 * AWS {@code us-east-1} and Google Cloud {@code us-east4} at DC (a GA catalog pair) with a
 * "Replication" workload depending on both, so without a further consumer both connections are
 * omitted ({@code CloudToCloudStrategyMatrixTest}). Each test adds one consumer.
 */
@DisplayName("NATIVE_WHEN_AVAILABLE: conservative replacement rule")
class ConservativeReplacementRuleTest {

    private static OptimizationResult awsGcp(List<UserSite> sites, List<ProviderRequirement> requestProviders,
                                             List<WorkloadSpec> extraWorkloads) {
        return MulticloudWizardFixtures.twoCloudResult(CloudProviderType.AWS, "AWS", "us-east-1",
                CloudProviderType.GOOGLE_CLOUD, "GCP", "us-east4", 10_000, sites, requestProviders, extraWorkloads);
    }

    private static DeploymentPlan plan(OptimizationResult result, CloudToCloudStrategy strategy) {
        return DeploymentWizard.builder(null, result)
                .notifications("noc@example.com")
                .rateCard(MulticloudWizardFixtures.flatRateCard())
                .cloudToCloudStrategy(strategy)
                .plan();
    }

    private static List<String> connectionNames(DeploymentPlan plan) {
        return plan.getProviderConnections().stream().map(PlannedConnection::getName).collect(Collectors.toList());
    }

    private static String reasoning(PlannedMulticloudInterconnect link) {
        return String.join("\n", link.getReasoning());
    }

    @Test
    @DisplayName("a user site keeps both connections: the link stays an ALTERNATIVE and says why")
    void userSiteKeepsBothConnections() {
        OptimizationResult result = awsGcp(List.of(MulticloudWizardFixtures.site("HQ", DC)),
                Collections.emptyList(), Collections.emptyList());

        DeploymentPlan plan = plan(result, CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE);

        assertEquals(List.of("FCR-DC-to-aws", "FCR-DC-to-gcp"), connectionNames(plan));
        PlannedMulticloudInterconnect link = plan.getMulticloudLinks().get(0);
        assertEquals(MulticloudLinkRole.ALTERNATIVE, link.getRole());
        assertTrue(link.getReplacedConnectionNames().isEmpty());
        assertEquals(List.of("FCR-DC-to-aws", "FCR-DC-to-gcp"), link.getEquinixConnectionNames());
        assertTrue(reasoning(link).contains("Kept FCR-DC-to-aws: the request declares 1 user site(s)"), reasoning(link));
        assertTrue(reasoning(link).contains("Kept FCR-DC-to-gcp: the request declares 1 user site(s)"), reasoning(link));
    }

    @Test
    @DisplayName("a kept connection is unchanged: same plan as EQUINIX_ONLY on the Equinix side")
    void keptConnectionsAreNotResized() {
        OptimizationResult result = awsGcp(List.of(MulticloudWizardFixtures.site("HQ", DC)),
                Collections.emptyList(), Collections.emptyList());

        DeploymentPlan kept = plan(result, CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE);
        DeploymentPlan equinixOnly = plan(result, CloudToCloudStrategy.EQUINIX_ONLY);

        assertEquals(equinixOnly.getProviderConnections(), kept.getProviderConnections());
        assertEquals(equinixOnly.getRoutingProtocols(), kept.getRoutingProtocols());
        assertEquals(equinixOnly.getPricing().getMonthlyTotal(), kept.getPricing().getMonthlyTotal());
    }

    @Test
    @DisplayName("a request-level provider requirement keeps that cloud's connection; the other cloud's is omitted")
    void requestLevelRequirementKeepsItsCloud() {
        OptimizationResult result = awsGcp(Collections.emptyList(),
                List.of(ProviderRequirement.builder().cloudProvider(CloudProviderType.AWS).label("AWS").required(true).build()),
                Collections.emptyList());

        DeploymentPlan plan = plan(result, CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE);

        assertEquals(List.of("FCR-DC-to-aws"), connectionNames(plan));
        PlannedMulticloudInterconnect link = plan.getMulticloudLinks().get(0);
        assertEquals(MulticloudLinkRole.REPLACEMENT, link.getRole());
        assertEquals(List.of("FCR-DC-to-gcp"), link.getReplacedConnectionNames());
        assertEquals(List.of("FCR-DC-to-aws"), link.getEquinixConnectionNames());
        assertTrue(reasoning(link).contains("Kept FCR-DC-to-aws: AWS is a request-level provider requirement"),
                reasoning(link));
    }

    @Test
    @DisplayName("another workload that depends on AWS alone keeps the AWS connection at its full shared bandwidth")
    void otherWorkloadKeepsSharedConnection() {
        WorkloadSpec analytics = WorkloadSpec.builder().label("Analytics").bandwidthMbps(2000)
                .dependsOnProviders(List.of(MulticloudWizardFixtures.cloud(CloudProviderType.AWS, "AWS"))).build();
        OptimizationResult result = awsGcp(Collections.emptyList(), Collections.emptyList(), List.of(analytics));

        DeploymentPlan plan = plan(result, CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE);

        assertEquals(List.of("FCR-DC-to-aws"), connectionNames(plan));
        PlannedConnection aws = plan.getProviderConnections().get(0);
        assertEquals(12_000, aws.getBandwidthMbps(), "kept whole: Replication 10000 + Analytics 2000, not resized");
        PlannedMulticloudInterconnect link = plan.getMulticloudLinks().get(0);
        assertEquals(MulticloudLinkRole.REPLACEMENT, link.getRole());
        assertEquals(List.of("FCR-DC-to-gcp"), link.getReplacedConnectionNames());
        assertTrue(reasoning(link).contains("Kept FCR-DC-to-aws: workload 'Analytics' uses it and depends on AWS only"),
                reasoning(link));
        // The native link is sized for the cloud-to-cloud flow only.
        assertEquals(10_000, link.getRequestedMbps());
    }

    @Test
    @DisplayName("a workload that also depends on a third-party service profile keeps both connections")
    void thirdPartyDependencyKeepsConnections() {
        WorkloadSpec mixed = WorkloadSpec.builder().label("Market Data").bandwidthMbps(1000)
                .dependsOnProviders(List.of(
                        MulticloudWizardFixtures.cloud(CloudProviderType.AWS, "AWS"),
                        MulticloudWizardFixtures.cloud(CloudProviderType.GOOGLE_CLOUD, "GCP"),
                        ProviderRequirement.builder().serviceProfileName("Acme Market Feed").build()))
                .build();
        OptimizationResult result = awsGcp(Collections.emptyList(), Collections.emptyList(), List.of(mixed));

        DeploymentPlan plan = plan(result, CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE);

        assertEquals(List.of("FCR-DC-to-aws", "FCR-DC-to-gcp"), connectionNames(plan));
        PlannedMulticloudInterconnect link = plan.getMulticloudLinks().get(0);
        assertEquals(MulticloudLinkRole.ALTERNATIVE, link.getRole());
        assertEquals(List.of("Replication", "Market Data"), link.getWorkloadLabels());
        assertEquals(11_000, link.getRequestedMbps(), "both workloads imply the AWS <-> Google Cloud flow");
        assertTrue(reasoning(link).contains("workload 'Market Data' uses it and also depends on a provider that is "
                + "not a well-known cloud"), reasoning(link));
    }

    @Test
    @DisplayName("a three-cloud workload with one uncatalogued pair keeps the connections that pair needs")
    void threeCloudWorkloadWithUncoveredPair() {
        // AWS us-east-1 <-> OCI us-ashburn-1 and AWS <-> Google Cloud are GA; Google Cloud <-> OCI is not catalogued.
        WorkloadSpec tri = WorkloadSpec.builder().label("Ledger").bandwidthMbps(1000)
                .dependsOnProviders(List.of(
                        MulticloudWizardFixtures.cloud(CloudProviderType.AWS, "AWS", "us-east-1"),
                        MulticloudWizardFixtures.cloud(CloudProviderType.GOOGLE_CLOUD, "GCP", "us-east4"),
                        MulticloudWizardFixtures.cloud(CloudProviderType.ORACLE_CLOUD, "OCI", "us-ashburn-1")))
                .build();
        OptimizationResult result = awsGcp(Collections.emptyList(), Collections.emptyList(), List.of(tri));

        DeploymentPlan plan = plan(result, CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE);

        // AWS: every flow of every AWS workload (AWS-GCP, AWS-OCI) is GA, so its connection is omitted.
        // Google Cloud: Ledger's Google Cloud <-> OCI flow has no environment, so its connection stays.
        assertEquals(List.of("FCR-DC-to-gcp"), connectionNames(plan));
        Map<String, PlannedMulticloudInterconnect> links = plan.getMulticloudLinks().stream()
                .collect(Collectors.toMap(PlannedMulticloudInterconnect::getName, l -> l));
        assertEquals(List.of("aws-gcp-DC", "aws-oci-DC"), List.copyOf(new java.util.TreeSet<>(links.keySet())),
                "the uncatalogued Google Cloud <-> OCI flow attaches no link under NATIVE_WHEN_AVAILABLE");
        assertEquals(MulticloudLinkRole.REPLACEMENT, links.get("aws-gcp-DC").getRole());
        assertEquals(List.of("FCR-DC-to-aws"), links.get("aws-gcp-DC").getReplacedConnectionNames());
        assertTrue(reasoning(links.get("aws-gcp-DC")).contains("Kept FCR-DC-to-gcp: workload 'Ledger' uses it and its "
                + "flow GOOGLE_CLOUD <-> ORACLE_CLOUD has no GA catalog environment"), reasoning(links.get("aws-gcp-DC")));
    }

    @Test
    @DisplayName("a workload placed at a metro without the cloud reaches it through this metro, so the connection is kept")
    void workloadAtAnotherMetroKeepsConnection() {
        OptimizationResult base = MulticloudWizardFixtures.awsGcpAtDc(10_000);
        WorkloadSpec remote = WorkloadSpec.builder().label("Remote").bandwidthMbps(500)
                .dependsOnProviders(List.of(MulticloudWizardFixtures.cloud(CloudProviderType.AWS, "AWS"))).build();
        List<WorkloadSpec> workloads = new java.util.ArrayList<>(base.getRequest().getWorkloads());
        workloads.add(remote);
        List<com.eqixiac.equinix.design.optimizer.model.WorkloadPlacement> placements =
                new java.util.ArrayList<>(base.getTopology().getPlacements());
        placements.add(com.eqixiac.equinix.design.optimizer.model.WorkloadPlacement.builder()
                .workloadLabel("Remote").assignedMetro(MulticloudWizardFixtures.DA).reasoning("placed at DA").build());
        List<com.eqixiac.equinix.design.optimizer.model.MetroRecommendation> metros =
                new java.util.ArrayList<>(base.getRecommendations());
        metros.add(com.eqixiac.equinix.design.optimizer.model.MetroRecommendation.builder()
                .rank(2).metroId(MulticloudWizardFixtures.DA).metroName("Dallas")
                .score(new com.eqixiac.equinix.design.optimizer.model.MetroScore(80.0, Collections.emptyList()))
                .reasons(List.of("Secondary")).availableProviders(Collections.emptyList()).build());
        OptimizationResult result = OptimizationResult.builder()
                .request(com.eqixiac.equinix.design.optimizer.model.OptimizationRequest.builder().workloads(workloads).build())
                .recommendations(metros)
                .topology(new com.eqixiac.equinix.design.optimizer.model.DeploymentTopology(placements))
                .computedAt(base.getComputedAt()).computeTimeMs(1).build();

        DeploymentPlan plan = plan(result, CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE);

        assertEquals(List.of("FCR-DC-to-aws"), connectionNames(plan), "DA has no AWS connection of its own");
        PlannedMulticloudInterconnect link = plan.getMulticloudLinks().get(0);
        assertEquals(List.of("FCR-DC-to-gcp"), link.getReplacedConnectionNames());
        assertTrue(reasoning(link).contains("Kept FCR-DC-to-aws: workload 'Remote' uses it and depends on AWS only"),
                reasoning(link));
        assertTrue(link.getPricing().getNotes().stream().anyMatch(n -> n.startsWith("Cloud Router FCR-DC is not in "
                + "the Equinix path cost: backbone link FCR-DC-to-DA also uses it")),
                () -> String.valueOf(link.getPricing().getNotes()));
    }

    @Test
    @DisplayName("a connection sized by the custom bandwidth map is kept")
    void customBandwidthKeepsConnection() {
        DeploymentPlan plan = DeploymentWizard.builder(null, MulticloudWizardFixtures.awsGcpAtDc(10_000))
                .notifications("noc@example.com")
                .rateCard(MulticloudWizardFixtures.flatRateCard())
                .customBandwidthMap(Map.of("DC-AWS", 5000))
                .cloudToCloudStrategy(CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE)
                .plan();

        assertEquals(List.of("FCR-DC-to-aws"), connectionNames(plan));
        assertEquals(5000, plan.getProviderConnections().get(0).getBandwidthMbps());
        assertTrue(reasoning(plan.getMulticloudLinks().get(0))
                .contains("Kept FCR-DC-to-aws: its bandwidth was set explicitly by the custom bandwidth map"));
    }

    @Test
    @DisplayName("AGGREGATED sizing puts every workload at the metro on every connection, so an uncovered one keeps them all")
    void aggregatedSizingCountsEveryWorkloadAtTheMetro() {
        WorkloadSpec analytics = WorkloadSpec.builder().label("Analytics").bandwidthMbps(2000)
                .dependsOnProviders(List.of(MulticloudWizardFixtures.cloud(CloudProviderType.AWS, "AWS"))).build();
        OptimizationResult result = awsGcp(Collections.emptyList(), Collections.emptyList(), List.of(analytics));

        DeploymentPlan plan = DeploymentWizard.builder(null, result)
                .notifications("noc@example.com")
                .rateCard(MulticloudWizardFixtures.flatRateCard())
                .bandwidthStrategy(BandwidthStrategy.AGGREGATED)
                .cloudToCloudStrategy(CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE)
                .plan();

        assertEquals(List.of("FCR-DC-to-aws", "FCR-DC-to-gcp"), connectionNames(plan));
        assertEquals(MulticloudLinkRole.ALTERNATIVE, plan.getMulticloudLinks().get(0).getRole());
    }

    @Test
    @DisplayName("an omitted connection has no routing protocol, no /30 subnet and no required input; the rest are allocated in sequence")
    void omittedConnectionLeavesNoTrace() {
        OptimizationResult result = awsGcp(Collections.emptyList(),
                List.of(ProviderRequirement.builder().cloudProvider(CloudProviderType.AWS).label("AWS").build()),
                Collections.emptyList());

        DeploymentPlan plan = plan(result, CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE);

        List<String> protocolParents = plan.getRoutingProtocols().stream()
                .map(PlannedRoutingProtocol::getConnectionName).distinct().collect(Collectors.toList());
        assertEquals(List.of("FCR-DC-to-aws"), protocolParents);
        assertEquals(2, plan.getRoutingProtocols().size());
        assertTrue(plan.getRoutingProtocols().stream()
                .noneMatch(rp -> rp.getName().contains("gcp") || rp.getConnectionName().contains("aws-gcp")));
        assertEquals("10.100.0.1/30", plan.getRoutingProtocols().get(0).getEquinixIfaceIpv4());
        assertEquals(List.of("FCR-DC-to-aws"), plan.getRequiredInputs().stream()
                .map(ConnectionInputRequirement::getConnectionName).collect(Collectors.toList()));
        // 1 Cloud Router + 1 connection + 2 routing protocols. The native link is not counted.
        assertEquals(4, plan.totalResourceCount());
        assertEquals(1, plan.getMulticloudLinks().size());
    }

    @Test
    @DisplayName("when both connections are omitted the link says the Cloud Router has no connection left")
    void strandedRouterIsCalledOut() {
        DeploymentPlan plan = plan(MulticloudWizardFixtures.awsGcpAtDc(10_000), CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE);

        assertTrue(plan.getProviderConnections().isEmpty());
        assertEquals(1, plan.getCloudRouters().size(), "the wizard still plans one Cloud Router per recommended metro");
        assertTrue(reasoning(plan.getMulticloudLinks().get(0))
                .contains("The Cloud Router at DC is still planned and priced"), reasoning(plan.getMulticloudLinks().get(0)));
    }
}
