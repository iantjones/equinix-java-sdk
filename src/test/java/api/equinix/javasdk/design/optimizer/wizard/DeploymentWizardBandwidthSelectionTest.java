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

package api.equinix.javasdk.design.optimizer.wizard;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.optimizer.model.MetroRecommendation;
import api.equinix.javasdk.design.optimizer.model.MetroScore;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.optimizer.model.ProviderAvailability;
import api.equinix.javasdk.design.optimizer.model.ServiceProfileOption;
import api.equinix.javasdk.design.optimizer.wizard.enums.BandwidthStrategy;
import api.equinix.javasdk.design.optimizer.wizard.model.DeploymentPlan;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedConnection;
import api.equinix.javasdk.design.optimizer.wizard.model.ProfileSelection;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.fabric.client.ServiceProfiles;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.AccessPointTypeConfig;
import api.equinix.javasdk.fabric.model.implementation.ServiceProfileMetro;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Bandwidth-aware service-profile selection in the Deployment Wizard, reproduced from the live
 * GlobalPay failure of 2026-07-24: the optimizer picked an AWS hosted-connection profile (allowed
 * tiers &le;500&nbsp;Mbps) for a connection sized well above it, because profile selection was fixed
 * before any connection bandwidth existed. The wizard must instead choose, among a provider's
 * candidate profiles for a metro, one whose allowed tiers cover the computed bandwidth — a dedicated
 * profile when the speed exceeds the hosted maximum — and, when NONE covers it, record a precise,
 * actionable error rather than emit an unbuildable connection.
 *
 * <p>Each provider entry carries realistic multi-profile stubs: an AWS hosted profile
 * ({@code [50..500]}) and an AWS dedicated profile ({@code [1000, 10000]}).</p>
 */
@DisplayName("DeploymentWizard — bandwidth-aware service-profile selection")
class DeploymentWizardBandwidthSelectionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final MetroId DC = MetroId.of(MetroCode.DC);

    private static final String HOSTED = "sp-aws-hosted";
    private static final String DEDICATED = "sp-aws-dedicated";

    // ══════════════════════════════════════════════
    //  Selection by bandwidth
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("a low-bandwidth connection selects the hosted profile (its tier covers the speed)")
    void lowBandwidthSelectsHostedProfile() {
        DeploymentPlan plan = planWithPinnedBandwidth(awsHostedAndDedicated(), 200);

        PlannedConnection aws = onlyProviderConnection(plan);
        assertEquals(HOSTED, aws.getZSideServiceProfileUuid(),
                "200 Mbps is a hosted tier, so the hosted profile must be chosen");
        assertEquals("us-east-1", aws.getZSideSellerRegion(),
                "the seller region must come from the chosen (hosted) profile");
        assertTrue(plan.getValidationErrors().isEmpty(), () -> "no error expected: " + plan.getValidationErrors());
    }

    @Test
    @DisplayName("a high-bandwidth connection selects the DEDICATED profile, never the <=500 hosted one")
    void highBandwidthSelectsDedicatedProfile() {
        // The exact live regression: 10000 Mbps must NOT land on the hosted profile capped at 500.
        DeploymentPlan plan = planWithPinnedBandwidth(awsHostedAndDedicated(), 10000);

        PlannedConnection aws = onlyProviderConnection(plan);
        assertEquals(DEDICATED, aws.getZSideServiceProfileUuid(),
                "10000 Mbps exceeds the hosted maximum, so the dedicated profile must be chosen");
        assertEquals("us-east-1-dx", aws.getZSideSellerRegion(),
                "uuid and seller region must come from the SAME (dedicated) profile, never be spliced");
        assertEquals(10000, aws.getBandwidthMbps());
        assertTrue(plan.getValidationErrors().isEmpty(), () -> "no error expected: " + plan.getValidationErrors());
    }

    @Test
    @DisplayName("a non-exact bandwidth ROUNDS UP to the smallest satisfying tier (3000 → 5000), never an error")
    void nonExactBandwidthRoundsUpToSmallestCoveringTier() {
        // The owner's explicit fix: 3000 Mbps is on no tier of [1000, 5000, 10000], so it must round UP
        // to 5000 (the smallest tier that satisfies it), not error.
        DeploymentPlan plan = planWithPinnedBandwidth(awsTiered(List.of(1000, 5000, 10000)), 3000);

        PlannedConnection aws = onlyProviderConnection(plan);
        assertEquals(5000, aws.getBandwidthMbps(),
                "3000 Mbps must be stamped at the smallest satisfying tier, 5000 Mbps");
        assertTrue(plan.getValidationErrors().isEmpty(),
                () -> "round-up is not an error: " + plan.getValidationErrors());

        // The round-up is RECORDED, never silent — requested, billed tier, and the delta.
        assertNotNull(aws.getProfileSelection(), "the profile selection must be exposed on the connection");
        assertEquals(3000, aws.getProfileSelection().getRequestedMbps());
        assertEquals(5000, aws.getProfileSelection().getSelectedTierMbps());
        assertTrue(aws.getProfileSelection().isRoundedUp());
        assertEquals(2000, aws.getProfileSelection().roundedUpByMbps());
        assertTrue(aws.getProfileSelection().getReasoning().contains("rounded up"),
                aws.getProfileSelection().getReasoning());
        // And it is surfaced in the plan render so it can never be a silent upsell.
        assertTrue(plan.toMarkdown().contains("BANDWIDTH ROUNDED UP"), "round-up must appear in the report");
    }

    @Test
    @DisplayName("a low bandwidth still selects the smallest covering (hosted) profile, exactly not rounded")
    void lowBandwidthPrefersSmallestCoveringProfileExactly() {
        DeploymentPlan plan = planWithPinnedBandwidth(awsHostedAndDedicated(), 200);

        PlannedConnection aws = onlyProviderConnection(plan);
        assertEquals(HOSTED, aws.getZSideServiceProfileUuid(), "200 Mbps is a hosted tier");
        assertEquals(200, aws.getBandwidthMbps());
        assertNotNull(aws.getProfileSelection());
        assertFalse(aws.getProfileSelection().isRoundedUp(), "200 is an exact hosted tier — no round-up");
    }

    @Test
    @DisplayName("a bandwidth above EVERY tier yields an actionable over-capacity error with a split suggestion")
    void bandwidthAboveEveryTierYieldsActionableErrorWithSplitSuggestion() {
        // 20000 Mbps exceeds the hosted ceiling (500) AND the dedicated maximum (10000): no round-up can
        // satisfy it, so THIS is the genuine over-capacity error (the only remaining error case).
        DeploymentPlan plan = planWithPinnedBandwidth(awsHostedAndDedicated(), 20000);

        assertTrue(plan.getProviderConnections().isEmpty(),
                "an unbuildable connection must NOT be emitted: " + plan.getProviderConnections());
        assertFalse(plan.isValid(), "a bandwidth above every tier makes the plan invalid");

        String error = plan.getValidationErrors().stream()
                .filter(e -> e.contains("20000") && e.contains("exceeds every available service profile"))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "expected an over-capacity error: " + plan.getValidationErrors()));
        assertTrue(error.contains("AWS") && error.contains("DC"),
                "the error names the provider and metro: " + error);
        assertTrue(error.contains("largest bandwidth any available service profile can carry is 10000 Mbps"),
                "the error states the real maximum available: " + error);
        assertTrue(error.contains("split the workload across multiple connections"),
                "the error suggests splitting across connections: " + error);
    }

    @Test
    @DisplayName("multiple profiles covering the same bandwidth expose the alternatives with a valid default")
    void multipleCoveringProfilesExposeAlternativesWithValidDefault() {
        // Two dedicated profiles, both listing 10000, in different seller regions: a genuine choice. The
        // wizard picks a deterministic default AND exposes both so an interactive layer can elicit.
        ProviderAvailability aws = ProviderAvailability.builder()
                .providerLabel("AWS")
                .available(true)
                .sellerRegions(List.of("us-east-1-dx"))
                .serviceProfileUuid("sp-aws-dx-a")
                .profileOptions(List.of(
                        ServiceProfileOption.builder()
                                .serviceProfileUuid("sp-aws-dx-a")
                                .sellerRegions(List.of("us-east-1-dx"))
                                .supportedBandwidths(List.of(1000, 10000))
                                .allowCustomBandwidth(false)
                                .build(),
                        ServiceProfileOption.builder()
                                .serviceProfileUuid("sp-aws-dx-b")
                                .sellerRegions(List.of("us-east-1-dx2"))
                                .supportedBandwidths(List.of(1000, 10000))
                                .allowCustomBandwidth(false)
                                .build()))
                .build();

        DeploymentPlan plan = planWithPinnedBandwidth(aws, 10000);

        PlannedConnection conn = onlyProviderConnection(plan);
        ProfileSelection sel = conn.getProfileSelection();
        assertNotNull(sel, "the profile selection must be exposed");
        assertTrue(sel.hasChoice(), "two covering profiles are a genuine decision point");
        assertEquals(2, sel.getAlternatives().size(), "both covering candidates are exposed");
        assertEquals("sp-aws-dx-a", sel.getSelectedProfileUuid(),
                "the deterministic default (lowest uuid) is chosen so a non-eliciting caller gets a valid plan");
        assertEquals("sp-aws-dx-a", conn.getZSideServiceProfileUuid());
        assertEquals(10000, conn.getBandwidthMbps());
        assertTrue(plan.getValidationErrors().isEmpty(), () -> "valid: " + plan.getValidationErrors());
    }

    @Test
    @DisplayName("an availability entry with no profileOptions falls back to the pre-selected default uuid")
    void noProfileOptionsFallsBackToDefault() {
        // Hand-built entries that predate bandwidth-aware selection carry no capability data; the wizard
        // must keep working exactly as before, pinning the default uuid and its first seller region.
        ProviderAvailability legacy = ProviderAvailability.builder()
                .providerLabel("AWS")
                .available(true)
                .sellerRegions(List.of("us-east-1"))
                .serviceProfileUuid("sp-aws-legacy")
                .build();

        DeploymentPlan plan = planWithPinnedBandwidth(legacy, 3000);

        PlannedConnection aws = onlyProviderConnection(plan);
        assertEquals("sp-aws-legacy", aws.getZSideServiceProfileUuid());
        assertEquals("us-east-1", aws.getZSideSellerRegion());
        assertEquals(3000, aws.getBandwidthMbps(), "no capability data means no bandwidth-aware rejection");
    }

    // ══════════════════════════════════════════════
    //  End-to-end: the Layer-1 tier check now PASSES for a correctly-selected profile
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("the Layer-1 catalog tier check PASSES because the wizard now pins the covering profile")
    void correctlySelectedProfilePassesLayer1TierCheck() throws Exception {
        // A real Service Profile catalog: the dedicated profile lists [1000, 10000]; the hosted one
        // [50..500]. With the fix the wizard pins the dedicated uuid for a 10000 Mbps connection, so
        // PlanValidator.checkProfile re-fetches THAT profile and finds 10000 among its tiers — no
        // "not an allowed tier" error. Under the old behaviour the hosted uuid would have been pinned
        // and this same Layer-1 check would have failed exactly as it did live.
        FabricGateway fabric = catalogGateway(
                Map.of(
                        HOSTED, profile(
                                List.of(spMetro("DC", "us-east-1", 500)),
                                List.of(apConfig(List.of(50, 100, 200, 300, 400, 500), false))),
                        DEDICATED, profile(
                                List.of(spMetro("DC", "us-east-1-dx", 10000)),
                                List.of(apConfig(List.of(1000, 10000), false)))));

        DeploymentPlan plan = DeploymentWizard.builder(fabric, resultWith(awsHostedAndDedicated()))
                .routerPackage("STANDARD")
                .routerNamePrefix("FCR")
                .providerConnectionType(ConnectionType.IP_VC)
                .customBandwidthMap(Map.of("DC-AWS", 10000))
                .customerAsn(65100L)
                .notifications("noc@example.com")
                .rateCard(emptyRateCard())
                .plan();

        PlannedConnection aws = onlyProviderConnection(plan);
        assertEquals(DEDICATED, aws.getZSideServiceProfileUuid(), "the covering dedicated profile is pinned");

        assertTrue(plan.getValidationErrors().stream().noneMatch(e -> e.contains("not an allowed tier")),
                "the Layer-1 tier check must pass for the correctly-selected profile: " + plan.getValidationErrors());
        assertTrue(plan.isValid(), () -> "a bandwidth-correct plan is valid: " + plan.getValidationErrors());
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════

    /** An AWS entry carrying two candidate profiles: hosted ([50..500]) and dedicated ([1000, 10000]). */
    private static ProviderAvailability awsHostedAndDedicated() {
        return ProviderAvailability.builder()
                .providerLabel("AWS")
                .available(true)
                // The default winner (region-preferred) is the hosted profile — exactly the shape that
                // shipped the live bug: hosted wins the metro, then a too-large connection is pinned to it.
                .sellerRegions(List.of("us-east-1"))
                .serviceProfileUuid(HOSTED)
                .profileOptions(List.of(
                        ServiceProfileOption.builder()
                                .serviceProfileUuid(HOSTED)
                                .sellerRegions(List.of("us-east-1"))
                                .supportedBandwidths(List.of(50, 100, 200, 300, 400, 500))
                                .allowCustomBandwidth(false)
                                .build(),
                        ServiceProfileOption.builder()
                                .serviceProfileUuid(DEDICATED)
                                .sellerRegions(List.of("us-east-1-dx"))
                                .supportedBandwidths(List.of(1000, 10000))
                                .allowCustomBandwidth(false)
                                .build()))
                .build();
    }

    /** An AWS entry carrying a single dedicated profile that publishes the given discrete tiers. */
    private static ProviderAvailability awsTiered(List<Integer> tiers) {
        return ProviderAvailability.builder()
                .providerLabel("AWS")
                .available(true)
                .sellerRegions(List.of("us-east-1-dx"))
                .serviceProfileUuid(DEDICATED)
                .profileOptions(List.of(
                        ServiceProfileOption.builder()
                                .serviceProfileUuid(DEDICATED)
                                .sellerRegions(List.of("us-east-1-dx"))
                                .supportedBandwidths(tiers)
                                .allowCustomBandwidth(false)
                                .build()))
                .build();
    }

    /** Plans against a single DC metro carrying {@code aws}, pinning the DC-AWS connection to {@code mbps}. */
    private static DeploymentPlan planWithPinnedBandwidth(ProviderAvailability aws, int mbps) {
        FabricGateway fabric = mock(FabricGateway.class);
        return DeploymentWizard.builder(fabric, resultWith(aws))
                .routerPackage("STANDARD")
                .routerNamePrefix("FCR")
                .providerConnectionType(ConnectionType.IP_VC)
                .bandwidthStrategy(BandwidthStrategy.AGGREGATED)
                .customBandwidthMap(Map.of("DC-AWS", mbps))
                .customerAsn(65100L)
                .notifications("noc@example.com")
                .rateCard(emptyRateCard())
                .plan();
    }

    private static OptimizationResult resultWith(ProviderAvailability aws) {
        MetroScore score = new MetroScore(90.0, Collections.emptyList());
        MetroRecommendation dc = MetroRecommendation.builder()
                .rank(1).metroId(DC).metroName("Ashburn").score(score).reasons(List.of("Primary"))
                .availableProviders(List.of(aws))
                .build();
        return OptimizationResult.builder()
                .recommendations(List.of(dc))
                .computedAt(Instant.now())
                .computeTimeMs(1)
                .build();
    }

    private static PlannedConnection onlyProviderConnection(DeploymentPlan plan) {
        List<PlannedConnection> conns = plan.getProviderConnections();
        assertEquals(1, conns.size(), () -> "expected exactly one provider connection: " + conns);
        return conns.get(0);
    }

    private static RateCard emptyRateCard() {
        RateCard card = mock(RateCard.class);
        lenient().when(card.connection(any(), anyInt(), any(), any())).thenReturn(Optional.empty());
        lenient().when(card.cloudRouter(anyString(), any(), any())).thenReturn(Optional.empty());
        return card;
    }

    private static FabricGateway catalogGateway(Map<String, ServiceProfile> byUuid) {
        ServiceProfiles sp = mock(ServiceProfiles.class);
        byUuid.forEach((uuid, profile) -> lenient().when(sp.getByUuid(uuid)).thenReturn(profile));
        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.serviceProfiles()).thenReturn(sp);
        return fabric;
    }

    private static ServiceProfile profile(List<ServiceProfileMetro> metros, List<AccessPointTypeConfig> configs) {
        ServiceProfile profile = mock(ServiceProfile.class);
        lenient().when(profile.metros()).thenReturn(metros);
        lenient().when(profile.getAccessPointTypeConfigs()).thenReturn(configs);
        return profile;
    }

    private static ServiceProfileMetro spMetro(String code, String sellerRegion, Integer vcBandwidthMax)
            throws Exception {
        StringBuilder json = new StringBuilder("{\"code\":\"").append(code)
                .append("\",\"sellerRegions\":{\"").append(sellerRegion).append("\":\"").append(sellerRegion)
                .append("\"}");
        if (vcBandwidthMax != null) {
            json.append(",\"vcBandwidthMax\":").append(vcBandwidthMax);
        }
        json.append("}");
        return MAPPER.readValue(json.toString(), ServiceProfileMetro.class);
    }

    private static AccessPointTypeConfig apConfig(List<Integer> supportedBandwidths, boolean allowCustom)
            throws Exception {
        StringBuilder json = new StringBuilder("{\"allowCustomBandwidth\":").append(allowCustom);
        if (supportedBandwidths != null) {
            json.append(",\"supportedBandwidths\":[");
            for (int i = 0; i < supportedBandwidths.size(); i++) {
                if (i > 0) json.append(",");
                json.append(supportedBandwidths.get(i));
            }
            json.append("]");
        }
        json.append("}");
        return MAPPER.readValue(json.toString(), AccessPointTypeConfig.class);
    }
}
