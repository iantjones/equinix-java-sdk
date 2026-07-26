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

package api.equinix.javasdk.mcp.server;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.optimizer.wizard.DeploymentWizard;
import api.equinix.javasdk.design.optimizer.wizard.enums.ConnectionPurpose;
import api.equinix.javasdk.design.optimizer.wizard.model.DeploymentPlan;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedConnection;
import api.equinix.javasdk.design.optimizer.wizard.model.ProfileCandidate;
import api.equinix.javasdk.design.optimizer.wizard.model.ProfileSelection;
import api.equinix.javasdk.design.value.ratecard.PriceQuote;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.Term;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProfileSelection in the MCP plan output — serialization + profile-choice elicitation")
class ProfileSelectionMcpTest {

    private final ServerContext ctx = ServerContext.builder().environment(Map.of()).build();

    /** A connection whose 3000 Mbps requirement was rounded up to a 5000 Mbps tier, with a genuine choice. */
    private static DeploymentPlan roundedUpChoicePlan() {
        ProfileCandidate def = ProfileCandidate.builder()
                .serviceProfileUuid("sp-default")
                .sellerRegions(List.of("us-east-1"))
                .coveringTierMbps(5000)
                .supportedBandwidths(List.of(1000, 5000, 10000))
                .allowCustomBandwidth(false)
                .vcBandwidthMax(10000)
                .build();
        ProfileCandidate alt = ProfileCandidate.builder()
                .serviceProfileUuid("sp-alt")
                .sellerRegions(List.of("us-west-1"))
                .coveringTierMbps(5000)
                .supportedBandwidths(List.of(5000))
                .allowCustomBandwidth(true)
                .build();
        ProfileSelection selection = ProfileSelection.builder()
                .requestedMbps(3000)
                .selectedProfileUuid("sp-default")
                .selectedSellerRegion("us-east-1")
                .selectedTierMbps(5000)
                .roundedUp(true)
                .alternatives(List.of(def, alt))
                .reasoning("3000 Mbps rounded up to the 5000 Mbps tier")
                .build();
        PlannedConnection connection = PlannedConnection.builder()
                .name("aws-conn")
                .purpose(ConnectionPurpose.PROVIDER)
                .bandwidthMbps(5000)
                .profileSelection(selection)
                .aSideMetro(MetroId.of("DC"))
                .aSideRouterName("FCR-DC")
                .zSideProviderLabel("AWS")
                .zSideSellerRegion("us-east-1")
                .zSideServiceProfileUuid("sp-default")
                .build();
        return DeploymentPlan.builder()
                .providerConnections(List.of(connection))
                .valid(true)
                .build();
    }

    private static JsonNode profileSelection(ObjectNode payload) {
        JsonNode connections = payload.get("provider_connections");
        assertNotNull(connections);
        assertEquals(1, connections.size());
        JsonNode ps = connections.get(0).get("profile_selection");
        assertNotNull(ps, "the connection carries a profile_selection block: " + payload.toPrettyString());
        return ps;
    }

    @Test
    @DisplayName("the profile_selection block surfaces the round-up and every covering alternative")
    void serializesRoundUpAndAlternatives() {
        ObjectNode payload = DesignToolFactory.planPayload(roundedUpChoicePlan(), "plan-x", ctx, null);
        JsonNode ps = profileSelection(payload);

        assertEquals(3000, ps.get("requested_mbps").asInt());
        assertEquals(5000, ps.get("selected_tier_mbps").asInt());
        assertTrue(ps.get("rounded_up").asBoolean(), "the round-up is surfaced, never silent");
        assertEquals(2000, ps.get("rounded_up_by_mbps").asInt());
        assertEquals("sp-default", ps.get("selected_profile_uuid").asText());
        assertEquals("us-east-1", ps.get("seller_region").asText());
        assertTrue(ps.get("has_choice").asBoolean(), "two covering profiles is a genuine choice");

        JsonNode alternatives = ps.get("alternatives");
        assertEquals(2, alternatives.size(), "every covering candidate is listed, default first");
        assertEquals("sp-default", alternatives.get(0).get("service_profile_uuid").asText());
        assertEquals(5000, alternatives.get(0).get("covering_tier_mbps").asInt());
        assertEquals(10000, alternatives.get(0).get("vc_bandwidth_max_mbps").asInt());
        assertEquals("us-east-1", alternatives.get(0).get("seller_regions").get(0).asText());
        assertEquals(List.of(1000, 5000, 10000), List.of(
                alternatives.get(0).get("supported_bandwidths").get(0).asInt(),
                alternatives.get(0).get("supported_bandwidths").get(1).asInt(),
                alternatives.get(0).get("supported_bandwidths").get(2).asInt()));
        assertEquals("sp-alt", alternatives.get(1).get("service_profile_uuid").asText());
        assertTrue(alternatives.get(1).get("allow_custom_bandwidth").asBoolean());
    }

    @Test
    @DisplayName("with no elicitation, a hasChoice() connection keeps the already-valid default profile")
    void keepsDefaultWhenUnsupported() {
        DeploymentPlan plan = roundedUpChoicePlan();
        DesignToolFactory.ProfileChoices choices =
                DesignToolFactory.applyProfileChoices(plan, ctx, StubExchanges.unsupported());

        PlannedConnection connection = choices.plan().getProviderConnections().get(0);
        assertEquals("sp-default", connection.getZSideServiceProfileUuid(), "the default profile is kept");
        assertEquals("kept_default", choices.records().get("aws-conn").status());

        ObjectNode payload = DesignToolFactory.planPayload(choices.plan(), "plan-x", ctx, choices);
        JsonNode choice = profileSelection(payload).get("choice");
        assertEquals("kept_default", choice.get("status").asText());
        assertEquals("sp-default", choice.get("selected_profile_uuid").asText());
    }

    @Test
    @DisplayName("with elicitation, a hasChoice() connection is prompted and the user's pick is applied")
    void appliesElicitedPick() {
        DeploymentPlan plan = roundedUpChoicePlan();
        DesignToolFactory.ProfileChoices choices =
                DesignToolFactory.applyProfileChoices(plan, ctx, StubExchanges.accepts("sp-alt"));

        PlannedConnection connection = choices.plan().getProviderConnections().get(0);
        assertEquals("sp-alt", connection.getZSideServiceProfileUuid(),
                "the connection is rebuilt onto the user-selected profile");
        assertEquals("us-west-1", connection.getZSideSellerRegion());
        assertEquals("sp-alt", connection.getProfileSelection().getSelectedProfileUuid());
        assertEquals("sp-alt", connection.getProfileSelection().getAlternatives().get(0).getServiceProfileUuid(),
                "the pick is reordered to the front of the alternatives");

        DesignToolFactory.ProfileChoiceRecord record = choices.records().get("aws-conn");
        assertEquals("picked", record.status());
        assertEquals("sp-alt", record.selectedProfileUuid());

        ObjectNode payload = DesignToolFactory.planPayload(choices.plan(), "plan-x", ctx, choices);
        JsonNode ps = profileSelection(payload);
        assertEquals("sp-alt", ps.get("selected_profile_uuid").asText());
        assertEquals("picked", ps.get("choice").get("status").asText());
    }

    @Test
    @DisplayName("a connection with no ProfileSelection (a backbone link) serializes no profile_selection block")
    void noBlockWhenNoSelection() {
        PlannedConnection backbone = PlannedConnection.builder()
                .name("backbone")
                .purpose(ConnectionPurpose.BACKBONE)
                .bandwidthMbps(10000)
                .aSideMetro(MetroId.of("DC"))
                .aSideRouterName("FCR-DC")
                .zSideRouterName("FCR-DA")
                .build();
        DeploymentPlan plan = DeploymentPlan.builder()
                .providerConnections(List.of(backbone))
                .valid(true)
                .build();

        ObjectNode payload = DesignToolFactory.planPayload(plan, "plan-x", ctx, null);
        assertFalse(payload.get("provider_connections").get(0).has("profile_selection"),
                "a null selection produces no block");
    }

    // ── reprice after an elicited tier change ───────────────────────────────

    /**
     * A connection with a genuine profile choice whose two covering profiles bill at DIFFERENT tiers:
     * the default (tightest fit) at 5000 Mbps, the alternative at 10000 Mbps. Picking the alternative
     * raises the connection's billable tier — the case whose pricing must be recomputed. The Cloud
     * Router and backbone lists are empty (never null), exactly as an engine-built plan carries them.
     */
    private static DeploymentPlan tierChoicePlan() {
        ProfileCandidate small = ProfileCandidate.builder()
                .serviceProfileUuid("sp-small")
                .sellerRegions(List.of("us-east-1"))
                .coveringTierMbps(5000)
                .supportedBandwidths(List.of(5000))
                .allowCustomBandwidth(false)
                .vcBandwidthMax(5000)
                .build();
        ProfileCandidate big = ProfileCandidate.builder()
                .serviceProfileUuid("sp-big")
                .sellerRegions(List.of("us-west-1"))
                .coveringTierMbps(10000)
                .supportedBandwidths(List.of(10000))
                .allowCustomBandwidth(false)
                .vcBandwidthMax(10000)
                .build();
        ProfileSelection selection = ProfileSelection.builder()
                .requestedMbps(3000)
                .selectedProfileUuid("sp-small")
                .selectedSellerRegion("us-east-1")
                .selectedTierMbps(5000)
                .roundedUp(true)
                .alternatives(List.of(small, big))
                .reasoning("two covering profiles; tightest (5000 Mbps) is the default")
                .build();
        PlannedConnection connection = PlannedConnection.builder()
                .name("aws-conn")
                .connectionType(ConnectionType.IP_VC)
                .purpose(ConnectionPurpose.PROVIDER)
                .bandwidthMbps(5000)
                .profileSelection(selection)
                .aSideMetro(MetroId.of("DC"))
                .aSideRouterName("FCR-DC")
                .zSideProviderLabel("AWS")
                .zSideSellerRegion("us-east-1")
                .zSideServiceProfileUuid("sp-small")
                .build();
        return DeploymentPlan.builder()
                .cloudRouters(List.of())
                .providerConnections(List.of(connection))
                .backboneLinks(List.of())
                .valid(true)
                .build();
    }

    /** A rate card whose monthly connection price is exactly the billable bandwidth in Mbps (USD). */
    private static RateCard bandwidthPricedCard() {
        return new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
                return Optional.of(PriceQuote.of(BigDecimal.valueOf(bandwidthMbps), BigDecimal.valueOf(100),
                        Currency.getInstance("USD"), PriceSource.CUSTOM));
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public PriceSource source() {
                return PriceSource.CUSTOM;
            }
        };
    }

    /**
     * A wizard builder configured with the bandwidth-priced rate card. The card is set, so repricing
     * never touches the (null) Fabric gateway or optimization result — it just prices the plan's current
     * connections against this card, the same way the MCP's wizard prices the plan it built.
     */
    private static DeploymentWizard.Builder repricingWizard() {
        return DeploymentWizard.builder(null, null).rateCard(bandwidthPricedCard());
    }

    @Test
    @DisplayName("after an elicited profile choice raises a connection's tier, reprice() recomputes pricing to the new tier")
    void repricesAfterElicitedTierChange() {
        DeploymentWizard.Builder wizard = repricingWizard();

        // Baseline: the plan priced at the wizard's default (5000 Mbps) tier.
        DeploymentPlan priced = wizard.reprice(tierChoicePlan());
        assertEquals(0, BigDecimal.valueOf(5000).compareTo(priced.getPricing().getProviderConnectionMonthlyCost()),
                "the default tier prices the provider connection at 5000");
        assertEquals(0, BigDecimal.valueOf(5000).compareTo(priced.getPricing().getMonthlyTotal()));

        // The user elicits a pick that moves the connection onto the higher (10000 Mbps) tier.
        DesignToolFactory.ProfileChoices choices =
                DesignToolFactory.applyProfileChoices(priced, ctx, StubExchanges.accepts("sp-big"));
        assertTrue(choices.changed(), "a pick onto a different profile is a change");
        assertEquals(10000, choices.plan().getProviderConnections().get(0).getBandwidthMbps(),
                "the connection is rebuilt at the chosen profile's tier");

        // The rebuilt plan still carries the pricing computed from the pre-swap tier — this is the defect
        // reprice() exists to correct: pricing is stale until it is recomputed.
        assertEquals(0, BigDecimal.valueOf(5000).compareTo(
                        choices.plan().getPricing().getProviderConnectionMonthlyCost()),
                "the rebuilt plan carries the pre-swap pricing until repriced");

        // The fix: reprice from the now-current connections.
        DeploymentPlan repriced = wizard.reprice(choices.plan());
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(repriced.getPricing().getProviderConnectionMonthlyCost()),
                "reprice reflects the NEW 10000 Mbps tier, not the old 5000");
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(repriced.getPricing().getMonthlyTotal()),
                "the monthly total tracks the new tier");
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(repriced.getPricing().getPerConnectionCost().get("aws-conn")),
                "the per-connection figure tracks the new tier");

        // Only pricing is replaced — the connections (and validity) are carried through untouched.
        assertEquals(choices.plan().getProviderConnections(), repriced.getProviderConnections());
        assertTrue(repriced.isValid());
    }

    @Test
    @DisplayName("repricing an unchanged plan yields identical pricing (a no-op-equivalent)")
    void repriceUnchangedIsIdentical() {
        DeploymentWizard.Builder wizard = repricingWizard();
        DeploymentPlan priced = wizard.reprice(tierChoicePlan());

        // No elicitation support → the default is kept, nothing changes.
        DesignToolFactory.ProfileChoices choices =
                DesignToolFactory.applyProfileChoices(priced, ctx, StubExchanges.unsupported());
        assertFalse(choices.changed(), "keeping the default is not a change");

        // Repricing the unchanged plan produces the same figures it already had.
        DeploymentPlan repriced = wizard.reprice(choices.plan());
        assertEquals(0, priced.getPricing().getProviderConnectionMonthlyCost()
                        .compareTo(repriced.getPricing().getProviderConnectionMonthlyCost()),
                "an unchanged plan reprices to the same provider-connection cost");
        assertEquals(0, priced.getPricing().getMonthlyTotal()
                .compareTo(repriced.getPricing().getMonthlyTotal()), "the monthly total is unchanged");
        assertEquals(0, priced.getPricing().getSetupTotal()
                .compareTo(repriced.getPricing().getSetupTotal()), "the setup total is unchanged");
        assertEquals(priced.getPricing().getCurrency(), repriced.getPricing().getCurrency());
    }
}
