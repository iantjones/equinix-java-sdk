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

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.model.MetroRecommendation;
import com.eqixiac.equinix.design.optimizer.model.MetroScore;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.ProviderAvailability;
import com.eqixiac.equinix.design.optimizer.wizard.enums.BackboneTopology;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Locks the {@code EQ-3142539} fix: every name the Deployment Wizard generates — Cloud Routers,
 * provider connections, backbone links, and routing protocols — must be 1..23 characters (Fabric
 * rejects 0 or {@code >= 24}) and unique within a plan.
 *
 * <p>The keystone scenario mirrors the live failure: a "globalpay" prefix over four metros that each
 * carry AWS + Azure + GCP, with providers named by their <em>full display names</em>
 * ("Amazon Web Services", ...) — the exact input that produced 35-character names like
 * {@code globalpay-NY-to-amazon-web-services} and made all 12 provider connections fail the live
 * dry run. The fix substitutes a compact provider token and caps every composed name.</p>
 */
@DisplayName("DeploymentWizard — generated names fit Fabric's < 24-character limit (EQ-3142539)")
class DeploymentWizardNameGenerationTest {

    /** Fabric's contract: connection name length must be > 0 and < 24 characters. */
    private static final int MIN_LEN = 1;
    private static final int MAX_LEN = 23;

    // ── keystone: realistic 4-metro / 3-cloud plan with full display-name providers ──

    @Test
    @DisplayName("every name in a 4-metro / 3-cloud plan is 1..23 chars and unique (full display-name providers)")
    void realisticPlanNamesAllFitAndAreUnique() {
        DeploymentPlan plan = planFor("globalpay",
                fourMetroThreeCloudResult("Amazon Web Services", "Microsoft Azure", "Google Cloud Platform"));

        // The scenario shape: 4 routers, 4×3 = 12 provider connections, full-mesh 4C2 = 6 backbone
        // links, and a DIRECT+BGP pair per connection = (12 + 6) × 2 = 36 routing protocols.
        assertEquals(4, plan.getCloudRouters().size());
        assertEquals(12, plan.getProviderConnections().size());
        assertEquals(6, plan.getBackboneLinks().size());
        assertEquals(36, plan.getRoutingProtocols().size());

        List<String> names = allGeneratedNames(plan);
        assertEquals(4 + 12 + 6 + 36, names.size(), "every planned resource contributes one name");
        assertAllFitAndUnique(names);

        // The specific defect: no name still carries a full provider display name.
        assertTrue(names.stream().noneMatch(n -> n.contains("amazon") || n.contains("microsoft") || n.contains("google")),
                () -> "a full provider display name leaked into a generated name: " + names);
        // The compact token is used instead.
        assertTrue(plan.getProviderConnections().stream().anyMatch(c -> c.getName().endsWith("-to-aws")),
                () -> "expected a compact 'aws' token in a provider connection name: "
                        + plan.getProviderConnections());
    }

    @Test
    @DisplayName("the same plan with providers named by their PRODUCT names resolves to the same tokens")
    void productNamedProvidersResolveToTokens() {
        DeploymentPlan plan = planFor("globalpay",
                fourMetroThreeCloudResult("AWS Direct Connect", "Azure ExpressRoute", "Google Cloud Interconnect"));

        assertAllFitAndUnique(allGeneratedNames(plan));
        Set<String> connSuffixes = plan.getProviderConnections().stream()
                .map(c -> c.getName().substring(c.getName().lastIndexOf("-to-") + 4))
                .collect(Collectors.toSet());
        assertEquals(Set.of("aws", "azure", "gcp"), connSuffixes,
                "product names must resolve to the same compact tokens as corporate names");
    }

    // ── edge: a long, caller-supplied prefix must not push names over the limit ──

    @Test
    @DisplayName("an over-long router_name_prefix still yields names that all fit and stay unique")
    void longPrefixStillFitsAndStaysUnique() {
        String longPrefix = "acme-globalpay-production-payments-platform"; // 43 chars, far over the budget
        DeploymentPlan plan = planFor(longPrefix,
                fourMetroThreeCloudResult("Amazon Web Services", "Microsoft Azure", "Google Cloud Platform"));

        List<String> names = allGeneratedNames(plan);
        assertEquals(4 + 12 + 6 + 36, names.size());
        assertAllFitAndUnique(names);
    }

    @Test
    @DisplayName("a blank router_name_prefix fails fast with a clear error when the plan is generated")
    void blankPrefixIsRejected() {
        FabricGateway fabric = mock(FabricGateway.class);
        OptimizationResult result =
                fourMetroThreeCloudResult("Amazon Web Services", "Microsoft Azure", "Google Cloud Platform");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                DeploymentWizard.builder(fabric, result)
                        .routerNamePrefix("   ")
                        .rateCard(emptyRateCard())
                        .plan());
        assertTrue(ex.getMessage().toLowerCase().contains("prefix"),
                () -> "the error must name the offending input: " + ex.getMessage());
    }

    // ── provider-token pinning (per provider) ──

    @Test
    @DisplayName("each provider maps to its pinned compact token, via label and via CloudProviderType.shortCode()")
    void providerTokensArePinned() {
        // CloudProviderType.shortCode() is the source of truth.
        assertEquals("aws", CloudProviderType.AWS.shortCode());
        assertEquals("azure", CloudProviderType.AZURE.shortCode());
        assertEquals("gcp", CloudProviderType.GOOGLE_CLOUD.shortCode());
        assertEquals("oci", CloudProviderType.ORACLE_CLOUD.shortCode());
        assertEquals("ibm", CloudProviderType.IBM_CLOUD.shortCode());
        assertEquals("alibaba", CloudProviderType.ALIBABA_CLOUD.shortCode());

        // PlanNames.providerToken resolves real Fabric labels (corporate names) to the same tokens.
        assertEquals("aws", PlanNames.providerToken("Amazon Web Services"));
        assertEquals("azure", PlanNames.providerToken("Microsoft Azure"));
        assertEquals("gcp", PlanNames.providerToken("Google Cloud Platform"));
        assertEquals("oci", PlanNames.providerToken("Oracle Cloud Infrastructure"));
        assertEquals("ibm", PlanNames.providerToken("IBM Cloud"));
        assertEquals("alibaba", PlanNames.providerToken("Alibaba Cloud"));

        // Every pinned token is itself short enough to keep composed names within budget.
        for (String token : List.of("aws", "azure", "gcp", "oci", "ibm", "alibaba")) {
            assertTrue(token.length() <= 8 && !token.isBlank(), token);
        }

        // A label that matches no well-known provider falls back to a short, name-safe slug.
        String fallback = PlanNames.providerToken("Some Regional Carrier X");
        assertTrue(fallback.length() >= MIN_LEN && fallback.length() <= 8, fallback);
        assertTrue(fallback.matches("[a-z0-9-]+"), fallback);
    }

    // ── PlanNames unit behaviour: cap + collision disambiguation ──

    @Test
    @DisplayName("PlanNames caps to 23 and disambiguates colliding truncations deterministically")
    void planNamesCapsAndDisambiguates() {
        PlanNames a = new PlanNames();
        // Two long, distinct desired names that truncate to the same 23-char head must not collide.
        String n1 = a.unique("globalpay-frankfurt-to-amazon-alpha");
        String n2 = a.unique("globalpay-frankfurt-to-amazon-beta");
        assertTrue(n1.length() <= MAX_LEN && n2.length() <= MAX_LEN, n1 + " / " + n2);
        assertFalse(n1.equals(n2), "distinct desired names must yield distinct capped names");

        // Determinism: the same construction order reproduces the same names.
        PlanNames b = new PlanNames();
        assertEquals(n1, b.unique("globalpay-frankfurt-to-amazon-alpha"));
        assertEquals(n2, b.unique("globalpay-frankfurt-to-amazon-beta"));

        // uniqueWithSuffix keeps the suffixed name within budget even for a long base.
        PlanNames c = new PlanNames();
        String base = c.unique("globalpay-singapore-to-azure");
        String direct = c.uniqueWithSuffix(base, "DIRECT");
        String bgp = c.uniqueWithSuffix(base, "BGP");
        assertTrue(direct.length() <= MAX_LEN, direct);
        assertTrue(bgp.length() <= MAX_LEN, bgp);
        assertFalse(direct.equals(bgp));
    }

    // ── helpers ──

    private static List<String> allGeneratedNames(DeploymentPlan plan) {
        List<String> names = new ArrayList<>();
        plan.getCloudRouters().forEach(r -> names.add(r.getName()));
        plan.getProviderConnections().forEach(c -> names.add(c.getName()));
        plan.getBackboneLinks().forEach(l -> names.add(l.getName()));
        plan.getRoutingProtocols().forEach(p -> names.add(p.getName()));
        return names;
    }

    private static void assertAllFitAndUnique(List<String> names) {
        for (String name : names) {
            assertTrue(name != null && name.length() >= MIN_LEN && name.length() <= MAX_LEN,
                    () -> "name out of Fabric's 1..23 range (" + (name == null ? "null" : name.length())
                            + " chars): '" + name + "'");
        }
        Set<String> unique = new HashSet<>(names);
        assertEquals(names.size(), unique.size(), () -> {
            List<String> dupes = new ArrayList<>(names);
            unique.forEach(dupes::remove);
            return "duplicate generated names: " + dupes + " in " + names;
        });
    }

    private static DeploymentPlan planFor(String prefix, OptimizationResult result) {
        FabricGateway fabric = mock(FabricGateway.class); // bare stub: connections() is null → dry run skipped
        return DeploymentWizard.builder(fabric, result)
                .routerPackage("STANDARD")
                .routerNamePrefix(prefix)
                .providerConnectionType(ConnectionType.IP_VC)
                .backboneTopology(BackboneTopology.FULL_MESH)
                .backboneBandwidthMbps(10_000)
                .rateCard(emptyRateCard())
                .plan();
    }

    /**
     * Four metros (NY, LD, SG, FR), each carrying the same three clouds as <em>available</em> providers
     * named by the given labels. No workloads or topology are supplied, so each provider connection
     * takes the wizard's 1000 Mbps default — the plan's <em>names</em> are what this suite exercises,
     * not its bandwidth sizing.
     */
    private static OptimizationResult fourMetroThreeCloudResult(String awsLabel, String azureLabel, String gcpLabel) {
        List<MetroRecommendation> recs = new ArrayList<>();
        int rank = 1;
        for (String code : List.of("NY", "LD", "SG", "FR")) {
            recs.add(MetroRecommendation.builder()
                    .rank(rank++)
                    .metroId(MetroId.of(code))
                    .metroName(code)
                    .score(new MetroScore(90.0, List.of()))
                    .reasons(List.of("candidate"))
                    .availableProviders(Arrays.asList(
                            available(awsLabel, "sp-aws-" + code, "us-east-1"),
                            available(azureLabel, "sp-azure-" + code, "eastus"),
                            available(gcpLabel, "sp-gcp-" + code, "us-central1")))
                    .build());
        }

        return OptimizationResult.builder()
                .recommendations(recs)
                .computedAt(Instant.now())
                .computeTimeMs(7)
                .build();
    }

    private static ProviderAvailability available(String label, String uuid, String region) {
        return ProviderAvailability.builder()
                .providerLabel(label)
                .available(true)
                .sellerRegions(List.of(region))
                .serviceProfileUuid(uuid)
                .build();
    }

    /** A rate card that resolves nothing, so pricing falls to the built-in heuristic and issues no HTTP. */
    private static RateCard emptyRateCard() {
        RateCard card = mock(RateCard.class);
        lenient().when(card.connection(any(), anyInt(), any(), any())).thenReturn(Optional.empty());
        lenient().when(card.cloudRouter(anyString(), any(), any())).thenReturn(Optional.empty());
        return card;
    }
}
