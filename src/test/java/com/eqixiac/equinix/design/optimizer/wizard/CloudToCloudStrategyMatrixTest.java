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

import com.eqixiac.equinix.core.model.multicloud.BandwidthTier;
import com.eqixiac.equinix.design.optimizer.enums.MulticloudEnvironmentStatus;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironment;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironmentCatalog;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.wizard.enums.CloudToCloudStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.enums.MulticloudLinkRole;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedMulticloudInterconnect;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The four {@link CloudToCloudStrategy} values against a cloud pair the bundled catalog lists
 * (AWS {@code us-east-1} and Google Cloud {@code us-east4}, GA) and one it does not (AWS
 * {@code us-east-2} and Google Cloud {@code us-central1}). Each fixture is one metro, one workload
 * depending on both clouds at 10000 Mbps, and no user site, so the replacement rule has no reason
 * to keep a connection.
 */
@DisplayName("DeploymentWizard cloudToCloudStrategy: strategy x environment matrix")
class CloudToCloudStrategyMatrixTest {

    private static OptimizationResult environmentPresent() {
        return MulticloudWizardFixtures.awsGcpAtDc(10_000);
    }

    private static OptimizationResult environmentAbsent() {
        return MulticloudWizardFixtures.twoCloudResult(CloudProviderType.AWS, "AWS", "us-east-2",
                CloudProviderType.GOOGLE_CLOUD, "GCP", "us-central1", 10_000);
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

    @ParameterizedTest(name = "{0}, environment present: {1} link(s) with role {2}, {3} provider connection(s), valid={4}")
    @CsvSource({
            "EQUINIX_ONLY,          0, ,            2, true",
            "COMPARE,               1, ALTERNATIVE, 2, true",
            "NATIVE_WHEN_AVAILABLE, 1, REPLACEMENT, 0, true",
            "NATIVE_ONLY,           1, REPLACEMENT, 0, true"})
    void environmentPresentMatrix(CloudToCloudStrategy strategy, int links, MulticloudLinkRole role,
                                  int connections, boolean valid) {
        DeploymentPlan plan = plan(environmentPresent(), strategy);

        assertEquals(links, plan.multicloudLinksOrEmpty().size());
        assertEquals(connections, plan.getProviderConnections().size());
        assertEquals(connections * 2, plan.getRoutingProtocols().size(),
                "one DIRECT+BGP pair per remaining provider connection, none for a native link");
        assertEquals(valid, plan.isValid(), () -> String.valueOf(plan.getValidationErrors()));
        if (links == 1) {
            PlannedMulticloudInterconnect link = plan.getMulticloudLinks().get(0);
            assertEquals(role, link.getRole());
            assertEquals(strategy, link.getStrategy());
            assertEquals(MulticloudEnvironmentStatus.GA, link.environmentStatus());
            assertEquals(CloudProviderType.AWS, link.getProviderA());
            assertEquals("us-east-1", link.getRegionA());
            assertEquals(CloudProviderType.GOOGLE_CLOUD, link.getProviderZ());
            assertEquals("us-east4", link.getRegionZ());
            assertEquals(10_000, link.getRequestedMbps());
            assertEquals(10_000, link.getCoveringTierMbps());
            assertEquals(List.of("Replication"), link.getWorkloadLabels());
        } else {
            assertNull(plan.getMulticloudLinks(), "a plan without links carries null, as before the list existed");
        }
    }

    @ParameterizedTest(name = "{0}, environment absent: {1} link(s), 2 provider connections kept, valid={2}")
    @CsvSource({
            "EQUINIX_ONLY,          0, true",
            "COMPARE,               0, true",
            "NATIVE_WHEN_AVAILABLE, 0, true",
            "NATIVE_ONLY,           1, false"})
    void environmentAbsentMatrix(CloudToCloudStrategy strategy, int links, boolean valid) {
        DeploymentPlan plan = plan(environmentAbsent(), strategy);

        assertEquals(links, plan.multicloudLinksOrEmpty().size());
        assertEquals(List.of("FCR-DC-to-aws", "FCR-DC-to-gcp"), connectionNames(plan),
                "with no environment the Equinix path is planned under every strategy");
        assertEquals(4, plan.getRoutingProtocols().size());
        assertEquals(valid, plan.isValid(), () -> String.valueOf(plan.getValidationErrors()));
    }

    @Test
    @DisplayName("NATIVE_ONLY without an environment: one UNAVAILABLE entry, a Layer-1 error naming the pair, and execute() refuses the plan")
    void nativeOnlyWithoutEnvironmentIsAnError() {
        DeploymentPlan plan = plan(environmentAbsent(), CloudToCloudStrategy.NATIVE_ONLY);

        PlannedMulticloudInterconnect entry = plan.getMulticloudLinks().get(0);
        assertEquals(MulticloudLinkRole.UNAVAILABLE, entry.getRole());
        assertNull(entry.getEnvironment());
        assertNull(entry.getPricing());
        assertEquals("us-east-2", entry.getRegionA());
        assertEquals("us-central1", entry.getRegionZ());

        assertEquals(1, plan.getValidationErrors().size(), () -> String.valueOf(plan.getValidationErrors()));
        String error = plan.getValidationErrors().get(0);
        assertTrue(error.startsWith("NATIVE_ONLY: no usable native multicloud environment for "
                + "AWS us-east-2 <-> GOOGLE_CLOUD us-central1"), error);
        assertTrue(error.contains("the catalog has no environment for the region pair"), error);
        assertTrue(plan.getSkippedValidations().stream().noneMatch(s -> s.contains("Native multicloud link")),
                "the error is not also reported as skipped");

        assertThrows(IllegalStateException.class, plan::execute);
    }

    @ParameterizedTest(name = "PREVIEW environment under {0}: link kept as {1}, both Equinix connections stay, valid={2}")
    @CsvSource({
            "COMPARE,               ALTERNATIVE, true",
            "NATIVE_WHEN_AVAILABLE, ALTERNATIVE, true",
            "NATIVE_ONLY,           UNAVAILABLE, false"})
    void previewEnvironmentNeverReplaces(CloudToCloudStrategy strategy, MulticloudLinkRole role, boolean valid) {
        OptimizationResult awsAzure = MulticloudWizardFixtures.twoCloudResult(CloudProviderType.AWS, "AWS", "us-east-1",
                CloudProviderType.AZURE, "AZURE", "eastus", 1000);

        DeploymentPlan plan = plan(awsAzure, strategy);

        PlannedMulticloudInterconnect link = plan.getMulticloudLinks().get(0);
        assertEquals(MulticloudEnvironmentStatus.PREVIEW, link.environmentStatus());
        assertEquals(role, link.getRole());
        assertEquals(List.of("FCR-DC-to-aws", "FCR-DC-to-azure"), connectionNames(plan));
        assertEquals(valid, plan.isValid(), () -> String.valueOf(plan.getValidationErrors()));
        if (!valid) {
            assertTrue(plan.getValidationErrors().get(0).contains("is not GA"), plan.getValidationErrors().get(0));
        } else {
            assertTrue(link.getRecommendation().startsWith("The environment is PREVIEW"), link.getRecommendation());
        }
    }

    @ParameterizedTest(name = "no covering size under {0}: link {1}, connections kept, valid={2}")
    @CsvSource({
            "NATIVE_WHEN_AVAILABLE, ALTERNATIVE, true",
            "NATIVE_ONLY,           UNAVAILABLE, false"})
    void bandwidthAboveEveryListedSizeNeverReplaces(CloudToCloudStrategy strategy, MulticloudLinkRole role,
                                                   boolean valid) {
        DeploymentPlan plan = plan(MulticloudWizardFixtures.awsGcpAtDc(200_000), strategy);

        PlannedMulticloudInterconnect link = plan.getMulticloudLinks().get(0);
        assertEquals(role, link.getRole());
        assertNull(link.getCoveringTierMbps());
        assertTrue(link.coveringTier().isEmpty());
        assertEquals(2, plan.getProviderConnections().size());
        assertEquals(valid, plan.isValid(), () -> String.valueOf(plan.getValidationErrors()));
        assertNull(link.getPricing().getNativeMonthly(), "no size to order, so no native price");
        if (!valid) {
            assertTrue(plan.getValidationErrors().get(0).contains("lists no size covering 200000 Mbps"),
                    plan.getValidationErrors().get(0));
        }
    }

    @Test
    @DisplayName("the default strategy is COMPARE")
    void defaultIsCompare() {
        DeploymentPlan byDefault = DeploymentWizard.builder(null, environmentPresent())
                .notifications("noc@example.com")
                .rateCard(MulticloudWizardFixtures.flatRateCard())
                .plan();

        assertEquals(plan(environmentPresent(), CloudToCloudStrategy.COMPARE).getMulticloudLinks(),
                byDefault.getMulticloudLinks());
        assertEquals(CloudToCloudStrategy.COMPARE, byDefault.getMulticloudLinks().get(0).getStrategy());
    }

    @Test
    @DisplayName("COMPARE plans the same Equinix resources and Equinix prices as EQUINIX_ONLY")
    void compareLeavesEquinixSideUntouched() {
        DeploymentPlan equinixOnly = plan(environmentPresent(), CloudToCloudStrategy.EQUINIX_ONLY);
        DeploymentPlan compare = plan(environmentPresent(), CloudToCloudStrategy.COMPARE);

        assertEquals(equinixOnly.getCloudRouters(), compare.getCloudRouters());
        assertEquals(equinixOnly.getProviderConnections(), compare.getProviderConnections());
        assertEquals(equinixOnly.getBackboneLinks(), compare.getBackboneLinks());
        assertEquals(equinixOnly.getRoutingProtocols(), compare.getRoutingProtocols());
        assertEquals(equinixOnly.getRequiredInputs(), compare.getRequiredInputs());
        assertEquals(equinixOnly.getDeferredValidations(), compare.getDeferredValidations());
        assertEquals(equinixOnly.getValidationErrors(), compare.getValidationErrors());
        assertEquals(equinixOnly.totalResourceCount(), compare.totalResourceCount());
        assertEquals(equinixOnly.getPricing().getMonthlyTotal(), compare.getPricing().getMonthlyTotal());
        assertEquals(equinixOnly.getPricing().getSetupTotal(), compare.getPricing().getSetupTotal());
        assertEquals(equinixOnly.getPricing().getPerConnectionCost(), compare.getPricing().getPerConnectionCost());
        // The only added validation entry is the skipped note for the link, appended last.
        assertEquals(equinixOnly.getSkippedValidations(),
                compare.getSkippedValidations().subList(0, equinixOnly.getSkippedValidations().size()));
        assertEquals(equinixOnly.getSkippedValidations().size() + 1, compare.getSkippedValidations().size());
    }

    @ParameterizedTest(name = "a null strategy is rejected ({0} is accepted)")
    @EnumSource(CloudToCloudStrategy.class)
    void nullStrategyRejected(CloudToCloudStrategy accepted) {
        DeploymentWizard.Builder builder = DeploymentWizard.builder(null, environmentPresent());
        assertNotNull(builder.cloudToCloudStrategy(accepted));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> builder.cloudToCloudStrategy(null));
        assertTrue(e.getMessage().contains("EQUINIX_ONLY"), e.getMessage());
    }

    // ── Caller-supplied catalogs: the bundled one goes stale ──

    @Test
    @DisplayName("a caller-extended catalog turns the absent pair into a planned alternative")
    void extendedCatalogIsHonoured() {
        MulticloudEnvironmentCatalog extended = MulticloudEnvironmentCatalog.standard().with(
                MulticloudEnvironment.of(CloudProviderType.AWS, "us-east-2", CloudProviderType.GOOGLE_CLOUD, "us-central1",
                        MulticloudEnvironmentStatus.GA, BandwidthTier.of(10_000), "https://example.com/new-pair",
                        "2026-12-01"));

        DeploymentPlan plan = DeploymentWizard.builder(null, environmentAbsent())
                .notifications("noc@example.com")
                .rateCard(MulticloudWizardFixtures.flatRateCard())
                .multicloudEnvironments(extended)
                .plan();

        PlannedMulticloudInterconnect link = plan.getMulticloudLinks().get(0);
        assertEquals(MulticloudLinkRole.ALTERNATIVE, link.getRole());
        assertEquals("2026-12-01", link.getEnvironment().getAsOf());
        assertEquals(List.of("https://example.com/new-pair"), link.getEnvironment().getSourceUrls());
    }

    @Test
    @DisplayName("an empty catalog attaches nothing; a null catalog is rejected; the request's catalog is the fallback")
    void emptyNullAndRequestCatalog() {
        DeploymentPlan none = DeploymentWizard.builder(null, environmentPresent())
                .notifications("noc@example.com")
                .rateCard(MulticloudWizardFixtures.flatRateCard())
                .multicloudEnvironments(MulticloudEnvironmentCatalog.empty())
                .plan();
        assertTrue(none.multicloudLinksOrEmpty().isEmpty());

        assertThrows(IllegalArgumentException.class,
                () -> DeploymentWizard.builder(null, environmentPresent()).multicloudEnvironments(null));

        OptimizationResult base = environmentPresent();
        OptimizationResult withRequestCatalog = OptimizationResult.builder()
                .request(com.eqixiac.equinix.design.optimizer.model.OptimizationRequest.builder()
                        .sites(base.getRequest().getSites())
                        .providers(base.getRequest().getProviders())
                        .workloads(base.getRequest().getWorkloads())
                        .multicloudEnvironments(MulticloudEnvironmentCatalog.empty())
                        .build())
                .recommendations(base.getRecommendations())
                .topology(base.getTopology())
                .computedAt(base.getComputedAt())
                .computeTimeMs(base.getComputeTimeMs())
                .build();
        DeploymentPlan fromRequest = DeploymentWizard.builder(null, withRequestCatalog)
                .notifications("noc@example.com")
                .rateCard(MulticloudWizardFixtures.flatRateCard())
                .plan();
        assertTrue(fromRequest.multicloudLinksOrEmpty().isEmpty(), "the request's empty catalog was used");
        assertFalse(plan(base, CloudToCloudStrategy.COMPARE).multicloudLinksOrEmpty().isEmpty());
    }

    @Test
    @DisplayName("a single-cloud workload implies no flow: no link under any strategy")
    void singleCloudWorkloadsHaveNoFlow() {
        for (CloudToCloudStrategy strategy : CloudToCloudStrategy.values()) {
            DeploymentPlan plan = plan(MulticloudWizardFixtures.threeMetroSingleCloudResult(), strategy);
            assertNull(plan.getMulticloudLinks(), strategy.name());
            assertTrue(plan.isValid(), strategy.name());
        }
    }

    @Test
    @DisplayName("a preferred seller region on the dependency matches when the metro advertises a different region first")
    void preferredSellerRegionIsACandidate() {
        OptimizationResult base = MulticloudWizardFixtures.twoCloudResult(CloudProviderType.AWS, "AWS", "us-east-2",
                CloudProviderType.GOOGLE_CLOUD, "GCP", "us-east4", 10_000);
        // Replace the workload with one that states us-east-1 as its preferred AWS region.
        OptimizationResult result = OptimizationResult.builder()
                .request(com.eqixiac.equinix.design.optimizer.model.OptimizationRequest.builder()
                        .workloads(List.of(com.eqixiac.equinix.design.optimizer.model.WorkloadSpec.builder()
                                .label("Replication").bandwidthMbps(10_000)
                                .dependsOnProviders(List.of(
                                        MulticloudWizardFixtures.cloud(CloudProviderType.AWS, "AWS", "us-east-1"),
                                        MulticloudWizardFixtures.cloud(CloudProviderType.GOOGLE_CLOUD, "GCP")))
                                .build()))
                        .build())
                .recommendations(base.getRecommendations())
                .topology(base.getTopology())
                .computedAt(base.getComputedAt())
                .computeTimeMs(base.getComputeTimeMs())
                .build();

        PlannedMulticloudInterconnect link = plan(result, CloudToCloudStrategy.COMPARE).getMulticloudLinks().get(0);

        assertEquals("us-east-1", link.getRegionA(), "the planned region us-east-2 has no entry; the preferred region does");
        assertEquals("us-east4", link.getRegionZ());
    }
}
