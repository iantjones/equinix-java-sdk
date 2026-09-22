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
import com.eqixiac.equinix.design.optimizer.wizard.enums.CloudToCloudStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.enums.MulticloudLinkRole;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentOutcome;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedMulticloudInterconnect;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Doc-contract tests for the cloud-to-cloud lever of the Deployment Wizard. Each test quotes the
 * javadoc sentence it enforces. If a promise is changed on purpose, change the javadoc and the
 * quote here in the same commit.
 */
@DisplayName("design/optimizer cloud-to-cloud: documented behavioral promises (doc contracts)")
class MulticloudWizardDocContractTest {

    private static DeploymentWizard.Builder wizard() {
        return DeploymentWizard.builder(null, MulticloudWizardFixtures.awsGcpAtDc(10_000))
                .notifications("noc@example.com")
                .rateCard(MulticloudWizardFixtures.flatRateCard());
    }

    @Nested
    @DisplayName("CloudToCloudStrategy")
    class Strategy {

        /**
         * Enforces CloudToCloudStrategy.EQUINIX_ONLY javadoc: "The multicloud environment catalog
         * is not consulted, no native link is attached".
         */
        @Test
        @DisplayName("EQUINIX_ONLY does not read the catalog")
        void equinixOnlyDoesNotReadTheCatalog() {
            MulticloudEnvironmentCatalog catalog = mock(MulticloudEnvironmentCatalog.class);

            DeploymentPlan plan = wizard().multicloudEnvironments(catalog)
                    .cloudToCloudStrategy(CloudToCloudStrategy.EQUINIX_ONLY).plan();

            verifyNoInteractions(catalog);
            assertNull(plan.getMulticloudLinks());
        }

        /**
         * Enforces DeploymentWizard.Builder.cloudToCloudStrategy javadoc: "{@code COMPARE} changes
         * no Cloud Router, connection, backbone link, routing protocol or Equinix price relative to
         * {@code EQUINIX_ONLY}; it adds entries to {@code DeploymentPlan.getMulticloudLinks()}, one
         * skipped-validation note per entry, and the native-link fields of {@code PlanPricing}."
         */
        @Test
        @DisplayName("COMPARE adds links, one skipped note per link and the native pricing fields, and nothing else")
        void compareOnlyAdds() {
            DeploymentPlan equinixOnly = wizard().cloudToCloudStrategy(CloudToCloudStrategy.EQUINIX_ONLY).plan();
            DeploymentPlan compare = wizard().cloudToCloudStrategy(CloudToCloudStrategy.COMPARE).plan();

            assertEquals(equinixOnly.getCloudRouters(), compare.getCloudRouters());
            assertEquals(equinixOnly.getProviderConnections(), compare.getProviderConnections());
            assertEquals(equinixOnly.getBackboneLinks(), compare.getBackboneLinks());
            assertEquals(equinixOnly.getRoutingProtocols(), compare.getRoutingProtocols());
            assertEquals(equinixOnly.getPricing().getMonthlyTotal(), compare.getPricing().getMonthlyTotal());
            assertEquals(1, compare.getMulticloudLinks().size());
            assertEquals(equinixOnly.getSkippedValidations().size() + compare.getMulticloudLinks().size(),
                    compare.getSkippedValidations().size());
            assertEquals(1, compare.getPricing().getPerMulticloudLinkCost().size());
        }

        /**
         * Enforces MulticloudEnvironmentStatus.GA javadoc: "Only this status permits the Deployment
         * Wizard to omit Equinix connections in favour of the native link."
         */
        @Test
        @DisplayName("an UNVERIFIED environment replaces nothing under NATIVE_WHEN_AVAILABLE")
        void onlyGaReplaces() {
            MulticloudEnvironmentCatalog unverified = MulticloudEnvironmentCatalog.of(List.of(
                    MulticloudEnvironment.of(CloudProviderType.AWS, "us-east-1", CloudProviderType.GOOGLE_CLOUD,
                            "us-east4", MulticloudEnvironmentStatus.UNVERIFIED, BandwidthTier.of(10_000), null, null)));

            DeploymentPlan plan = wizard().multicloudEnvironments(unverified)
                    .cloudToCloudStrategy(CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE).plan();

            assertEquals(2, plan.getProviderConnections().size());
            assertEquals(MulticloudLinkRole.ALTERNATIVE, plan.getMulticloudLinks().get(0).getRole());
        }

        /**
         * Enforces CloudToCloudStrategy class javadoc, replacement rule: "Otherwise it is planned
         * exactly as under {@code EQUINIX_ONLY}, at the same bandwidth."
         */
        @Test
        @DisplayName("a connection the rule keeps equals the EQUINIX_ONLY connection")
        void keptConnectionEqualsEquinixOnly() {
            DeploymentWizard.Builder withSite = DeploymentWizard.builder(null, MulticloudWizardFixtures.twoCloudResult(
                            CloudProviderType.AWS, "AWS", "us-east-1", CloudProviderType.GOOGLE_CLOUD, "GCP", "us-east4",
                            10_000, List.of(MulticloudWizardFixtures.site("HQ", MulticloudWizardFixtures.DC)),
                            Collections.emptyList(), Collections.emptyList()))
                    .notifications("noc@example.com").rateCard(MulticloudWizardFixtures.flatRateCard());

            assertEquals(withSite.cloudToCloudStrategy(CloudToCloudStrategy.EQUINIX_ONLY).plan().getProviderConnections(),
                    withSite.cloudToCloudStrategy(CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE).plan().getProviderConnections());
        }
    }

    @Nested
    @DisplayName("PlannedMulticloudInterconnect and DeploymentPlan")
    class PlanModel {

        /**
         * Enforces PlannedMulticloudInterconnect javadoc: "It has no Cloud Router A-side, no routing
         * protocols, no /30 subnet, no redundancy group and no Fabric name constraint."
         */
        @Test
        @DisplayName("no routing protocol, subnet or planned connection refers to a native link")
        void noRoutingProtocolForALink() {
            DeploymentPlan plan = wizard().plan();
            String linkName = plan.getMulticloudLinks().get(0).getName();

            assertTrue(plan.getRoutingProtocols().stream().noneMatch(rp -> linkName.equals(rp.getConnectionName())));
            assertTrue(plan.getProviderConnections().stream().noneMatch(c -> linkName.equals(c.getName())));
            assertEquals(plan.getProviderConnections().size() * 2, plan.getRoutingProtocols().size());
            assertTrue(plan.getRequiredInputs().stream().noneMatch(r -> linkName.equals(r.getConnectionName())));
        }

        /**
         * Enforces DeploymentPlan.totalResourceCount javadoc: "Native multicloud links are excluded
         * because execution never creates them".
         */
        @Test
        @DisplayName("totalResourceCount() is the same with and without the links")
        void resourceCountExcludesLinks() {
            DeploymentPlan compare = wizard().plan();

            assertEquals(wizard().cloudToCloudStrategy(CloudToCloudStrategy.EQUINIX_ONLY).plan().totalResourceCount(),
                    compare.totalResourceCount());
            assertEquals(compare.totalResourceCount(),
                    compare.toBuilder().multicloudLinks(null).build().totalResourceCount());
        }

        /**
         * Enforces PlanPricing javadoc: "no native-link amount is part of {@code monthlyTotal},
         * {@code setupTotal}, {@code monthlyByCurrency} or any category figure. All of them are
         * {@code null} on a plan without native links."
         */
        @Test
        @DisplayName("native amounts are outside every Equinix figure, and null without links")
        void nativeAmountsAreSeparate() {
            DeploymentPlan compare = wizard().plan();
            DeploymentPlan equinixOnly = wizard().cloudToCloudStrategy(CloudToCloudStrategy.EQUINIX_ONLY).plan();

            assertEquals(equinixOnly.getPricing().getMonthlyTotal(), compare.getPricing().getMonthlyTotal());
            assertEquals(equinixOnly.getPricing().getSetupTotal(), compare.getPricing().getSetupTotal());
            assertEquals(equinixOnly.getPricing().getMonthlyByCurrency(), compare.getPricing().getMonthlyByCurrency());
            assertEquals(equinixOnly.getPricing().getRouterMonthlyCost(), compare.getPricing().getRouterMonthlyCost());
            assertEquals(equinixOnly.getPricing().getProviderConnectionMonthlyCost(),
                    compare.getPricing().getProviderConnectionMonthlyCost());
            assertEquals(equinixOnly.getPricing().getBackboneMonthlyCost(), compare.getPricing().getBackboneMonthlyCost());
            assertNull(equinixOnly.getPricing().getNativeAlternativeMonthlyCost());
            assertNull(equinixOnly.getPricing().getNativeReplacementMonthlyCost());
            assertNull(equinixOnly.getPricing().getPerMulticloudLinkCost());
        }

        /**
         * Enforces DeploymentOutcome javadoc: "An informational entry is not an error: it does not
         * affect {@code isFullySuccessful()} and is not counted in {@code errors}."
         */
        @Test
        @DisplayName("executing a plan that holds only a native link succeeds with no error and one informational entry")
        void informationalEntryIsNotAnError() {
            PlannedMulticloudInterconnect link = wizard().plan().getMulticloudLinks().get(0);
            // No Cloud Router, connection or protocol: execute() has nothing to send, so no gateway is needed.
            DeploymentPlan linkOnly = DeploymentPlan.builder().valid(true).multicloudLinks(List.of(link)).build();

            DeploymentOutcome outcome = linkOnly.execute();

            assertTrue(outcome.isFullySuccessful());
            assertTrue(outcome.getErrors().isEmpty());
            assertTrue(outcome.getResources().isEmpty());
            assertEquals(1, outcome.getInformational().size());
            assertFalse(outcome.getInformational().get(0).isRecoverable());
            assertTrue(linkOnly.rollback(outcome).isEmpty(), "rollback has nothing to delete for a native link");
        }

        /**
         * Enforces DeploymentWizard.Builder.reprice javadoc: "A link's environment, role, bandwidth
         * and reasoning are not re-planned."
         */
        @Test
        @DisplayName("reprice() under a different strategy keeps each link's role and environment")
        void repriceDoesNotReplan() {
            DeploymentPlan plan = wizard().plan();

            DeploymentPlan repriced = wizard().cloudToCloudStrategy(CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE)
                    .reprice(plan);

            assertEquals(MulticloudLinkRole.ALTERNATIVE, repriced.getMulticloudLinks().get(0).getRole());
            assertEquals(plan.getProviderConnections(), repriced.getProviderConnections());
            assertEquals(plan.getMulticloudLinks().get(0).getEnvironment(),
                    repriced.getMulticloudLinks().get(0).getEnvironment());
            assertEquals(plan.getMulticloudLinks().get(0).getRequestedMbps(),
                    repriced.getMulticloudLinks().get(0).getRequestedMbps());
        }
    }

    @Nested
    @DisplayName("MulticloudEnvironmentCatalog")
    class Catalog {

        /**
         * Enforces MulticloudEnvironmentCatalog javadoc: "{@link MulticloudEnvironmentCatalog#find}
         * and {@link MulticloudEnvironmentCatalog#regionsFor} treat the two clouds as an unordered
         * pair."
         */
        @Test
        @DisplayName("find() and regionsFor() give the same answer for either argument order")
        void unorderedPair() {
            MulticloudEnvironmentCatalog catalog = MulticloudEnvironmentCatalog.standard();

            assertEquals(catalog.find(CloudProviderType.AWS, "us-east-1", CloudProviderType.ORACLE_CLOUD, "us-ashburn-1"),
                    catalog.find(CloudProviderType.ORACLE_CLOUD, "us-ashburn-1", CloudProviderType.AWS, "us-east-1"));
            assertEquals(catalog.regionsFor(CloudProviderType.AWS, CloudProviderType.AZURE),
                    catalog.regionsFor(CloudProviderType.AZURE, CloudProviderType.AWS));
        }

        /**
         * Enforces MulticloudEnvironmentCatalog javadoc: "At most one entry exists per region pair:
         * adding an entry for a pair already present replaces it." and with(...) javadoc: "This
         * catalog is unchanged."
         */
        @Test
        @DisplayName("with() replaces the entry for the same region pair and leaves the receiver unchanged")
        void onePerRegionPair() {
            MulticloudEnvironmentCatalog standard = MulticloudEnvironmentCatalog.standard();
            int before = standard.environments().size();

            MulticloudEnvironmentCatalog updated = standard.with(MulticloudEnvironment.of(
                    CloudProviderType.GOOGLE_CLOUD, "US-EAST4", CloudProviderType.AWS, "us-east-1",
                    MulticloudEnvironmentStatus.UNVERIFIED, null, null, null));

            assertEquals(before, updated.environments().size());
            assertEquals(MulticloudEnvironmentStatus.UNVERIFIED, updated.find(CloudProviderType.AWS, "us-east-1",
                    CloudProviderType.GOOGLE_CLOUD, "us-east4").orElseThrow().getStatus());
            assertEquals(before, standard.environments().size());
            assertEquals(MulticloudEnvironmentStatus.GA, standard.find(CloudProviderType.AWS, "us-east-1",
                    CloudProviderType.GOOGLE_CLOUD, "us-east4").orElseThrow().getStatus());
        }
    }
}
