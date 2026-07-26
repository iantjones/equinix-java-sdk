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

package api.equinix.javasdk.design;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.geo.SpeedOfLightLatency;
import api.equinix.javasdk.design.optimizer.model.DeploymentTopology;
import api.equinix.javasdk.design.optimizer.model.MetroRecommendation;
import api.equinix.javasdk.design.optimizer.model.MetroScore;
import api.equinix.javasdk.design.optimizer.model.OptimizationRequest;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.optimizer.model.ProviderAvailability;
import api.equinix.javasdk.design.optimizer.model.ProviderRequirement;
import api.equinix.javasdk.design.optimizer.model.WorkloadPlacement;
import api.equinix.javasdk.design.optimizer.model.WorkloadSpec;
import api.equinix.javasdk.design.optimizer.wizard.DeploymentWizard;
import api.equinix.javasdk.design.optimizer.wizard.model.DeploymentPlan;
import api.equinix.javasdk.design.optimizer.wizard.model.PlanPricing;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedCloudRouter;
import api.equinix.javasdk.design.value.ratecard.ColocationItem;
import api.equinix.javasdk.design.value.ratecard.EgressPath;
import api.equinix.javasdk.design.value.ratecard.PriceQuote;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.Term;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import api.equinix.javasdk.internetaccess.model.Ibx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Doc-contract tests: each test enforces a <em>documented behavioral promise</em> from the
 * {@code design.*} javadoc, quoting the exact line it locks. The 2026-07 defect review found that
 * several documented promises had NO test at all — tests pinned incidental implementation behavior
 * while the published contract drifted. A test in this class fails when the code stops honouring
 * what its javadoc tells users; if a promise is deliberately changed, change the javadoc AND the
 * quote in the corresponding test here in the same commit.
 */
@DisplayName("design/* — documented behavioral promises (doc contracts)")
class DocContractTest {

    private static final MetroId DC = MetroId.of(MetroCode.DC);
    private static final MetroId DA = MetroId.of(MetroCode.DA);
    private static final Currency USD = Currency.getInstance("USD");

    // ══════════════════════════════════════════════
    //  RateCard / LayeredRateCard
    // ══════════════════════════════════════════════

    @Nested
    @DisplayName("RateCard layering")
    class RateCardLayering {

        /**
         * Enforces RateCard.layered(...) javadoc: "Combines several rate cards into a single card
         * that consults each in order and returns the first non-empty quote. Earlier cards take
         * precedence" — and LayeredRateCard's: "the earliest card that can price an item wins."
         */
        @Test
        @DisplayName("layered(...) consults cards in declaration order and the first non-empty quote wins")
        void layeredConsultsInOrderAndFirstNonEmptyWins() {
            List<String> consulted = new ArrayList<>();
            RateCard first = recordingCard("first", consulted, BigDecimal.valueOf(100));
            RateCard second = recordingCard("second", consulted, BigDecimal.valueOf(200));

            Optional<PriceQuote> quote = RateCard.layered(first, second)
                    .connection(ConnectionType.IP_VC, 1000, MetroCode.DC, Term.MONTH_12);

            assertTrue(quote.isPresent());
            assertEquals(0, BigDecimal.valueOf(100).compareTo(quote.get().getMonthlyRecurring()),
                    "the FIRST card's figure must win, not the second's");
            // "consults each in order": the second card is never asked once the first has priced.
            assertEquals(List.of("first"), consulted);
        }

        /**
         * Enforces the same RateCard.layered(...) promise from the fall-through side: an earlier
         * card that "cannot price that item" (empty) must NOT block later cards — "the first card
         * that can price an item wins."
         */
        @Test
        @DisplayName("an empty earlier card falls through to the next card in the chain")
        void layeredFallsThroughEmptyCards() {
            List<String> consulted = new ArrayList<>();
            RateCard silent = recordingCard("silent", consulted, null); // prices nothing
            RateCard priced = recordingCard("priced", consulted, BigDecimal.valueOf(200));

            Optional<PriceQuote> quote = RateCard.layered(silent, priced)
                    .connection(ConnectionType.IP_VC, 1000, MetroCode.DC, Term.MONTH_12);

            assertTrue(quote.isPresent());
            assertEquals(0, BigDecimal.valueOf(200).compareTo(quote.get().getMonthlyRecurring()));
            assertEquals(List.of("silent", "priced"), consulted, "both cards consulted, in order");
        }

        /**
         * Enforces RateCard interface javadoc: "an empty result means <em>this card cannot price
         * that item</em>, which is distinct from a zero price. That distinction lets callers fall
         * back to another source rather than silently treating 'unknown' as 'free.'" A chain with
         * no capable card must produce empty — never a fabricated zero quote.
         */
        @Test
        @DisplayName("a chain that cannot price returns empty — never a fabricated zero")
        void cannotPriceIsEmptyNeverZero() {
            RateCard chain = RateCard.layered(
                    recordingCard("a", new ArrayList<>(), null),
                    recordingCard("b", new ArrayList<>(), null));

            Optional<PriceQuote> quote = chain.connection(ConnectionType.IP_VC, 1000, MetroCode.DC, Term.MONTH_12);

            assertTrue(quote.isEmpty(), "unknown price must surface as empty, not as zero dollars");
        }

        /**
         * Enforces RateCard.egress(...) javadoc: "The default returns empty so cards that do not
         * model egress need not override it" — and RateCard.colocation(...): "Cards that do not
         * model colocation return {@code Optional.empty()} (the default)."
         */
        @Test
        @DisplayName("egress() and colocation() default to empty for cards that do not model them")
        void defaultEgressAndColocationAreEmpty() {
            RateCard minimal = recordingCard("minimal", new ArrayList<>(), BigDecimal.ONE);

            assertTrue(minimal.egress(CloudProviderType.AWS, "us-east-1", EgressPath.INTERNET, Term.MONTH_12)
                    .isEmpty());
            assertTrue(minimal.colocation(ColocationItem.CABINET, MetroCode.DC, Term.MONTH_12)
                    .isEmpty());
        }

        /**
         * Enforces RateCard.source() javadoc: "Aggregating cards report
         * {@code PriceSource.COMPOSITE}."
         */
        @Test
        @DisplayName("a layered card reports PriceSource.COMPOSITE")
        void layeredReportsComposite() {
            assertEquals(PriceSource.COMPOSITE,
                    RateCard.layered(recordingCard("a", new ArrayList<>(), BigDecimal.ONE)).source());
        }
    }

    // ══════════════════════════════════════════════
    //  SpeedOfLightLatency
    // ══════════════════════════════════════════════

    @Nested
    @DisplayName("SpeedOfLightLatency")
    class SpeedOfLight {

        /**
         * Enforces SpeedOfLightLatency class javadoc: "Coordinates are optional in both source
         * APIs, so every typed overload throws {@code IllegalArgumentException} naming the
         * offending IBX/metro when coordinates are missing — fail loud rather than return a bogus
         * zero." (IBX flavour: the message must name the IBX code.)
         */
        @Test
        @DisplayName("missing IBX coordinates throw an IllegalArgumentException NAMING the IBX")
        void missingIbxCoordinatesThrowNamingTheIbx() {
            Ibx la4 = mock(Ibx.class);
            when(la4.getIbxCode()).thenReturn("LA4");
            when(la4.getGeoCoordinates()).thenReturn(null);
            Ibx sv5 = mock(Ibx.class);
            when(sv5.getIbxCode()).thenReturn("SV5");
            when(sv5.getGeoCoordinates()).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> SpeedOfLightLatency.roundTrip().millisBetween(la4, sv5));
            assertTrue(ex.getMessage().contains("LA4"),
                    () -> "the offender must be NAMED, got: " + ex.getMessage());
        }

        /**
         * Enforces the same "naming the offending IBX/metro" promise, metro flavour: a metro
         * without coordinates must throw naming the metro, never return a bogus zero.
         */
        @Test
        @DisplayName("missing metro coordinates throw an IllegalArgumentException NAMING the metro")
        void missingMetroCoordinatesThrowNamingTheMetro() {
            Metro dc = mock(Metro.class);
            when(dc.metroId()).thenReturn(DC);
            when(dc.geoCoordinates()).thenReturn(null);
            Metro da = mock(Metro.class);
            when(da.metroId()).thenReturn(DA);
            when(da.geoCoordinates()).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> SpeedOfLightLatency.roundTrip().millisBetween(dc, da));
            assertTrue(ex.getMessage().contains("DC"),
                    () -> "the offender must be NAMED, got: " + ex.getMessage());
        }

        /**
         * Enforces millisForKm(...) javadoc: "@param distanceKm the distance in kilometres
         * (negative values are treated as 0)".
         */
        @Test
        @DisplayName("a negative distance is treated as 0, not extrapolated into negative latency")
        void negativeDistanceIsTreatedAsZero() {
            assertEquals(0.0, SpeedOfLightLatency.roundTrip().millisForKm(-100.0));
        }

        /**
         * Enforces Builder.mode(...) javadoc: "Unlike the other two setters, a {@code null}
         * argument does not throw — it is silently ignored and the current mode (initially
         * {@code Mode.ROUND_TRIP}) is kept."
         */
        @Test
        @DisplayName("mode(null) is silently ignored and ROUND_TRIP is kept")
        void nullModeIsIgnored() {
            SpeedOfLightLatency calc = SpeedOfLightLatency.builder().mode(null).build();
            assertEquals(SpeedOfLightLatency.Mode.ROUND_TRIP, calc.getMode());
        }
    }

    // ══════════════════════════════════════════════
    //  DeploymentWizard
    // ══════════════════════════════════════════════

    @Nested
    @DisplayName("DeploymentWizard")
    class Wizard {

        /**
         * Enforces DeploymentWizard.Builder.reprice(...) javadoc: "returns the result as a
         * <em>new</em> {@code DeploymentPlan}: the argument is never mutated (plans are immutable
         * values)", "Only the plan's {@code pricing} is replaced; every other field is carried
         * through unchanged", and "Repricing a plan whose connections did not change yields the
         * same figures (a no-op-equivalent)."
         */
        @Test
        @DisplayName("reprice() returns a new copy, never mutates the argument, and is figure-stable when nothing changed")
        void repriceReturnsCopyNeverMutates() {
            DeploymentWizard.Builder builder = offlineWizard();
            DeploymentPlan original = builder.plan();
            PlanPricing pricingBefore = original.getPricing();

            DeploymentPlan repriced = builder.reprice(original);

            assertNotSame(original, repriced, "reprice must return a NEW plan instance");
            assertSame(pricingBefore, original.getPricing(), "the argument's pricing must be untouched");
            // Unchanged connections => the same figures.
            assertNotNull(pricingBefore.getMonthlyTotal());
            assertEquals(0, pricingBefore.getMonthlyTotal()
                            .compareTo(repriced.getPricing().getMonthlyTotal()),
                    "repricing an unchanged plan must yield the same monthly total");
            // "every other field is carried through unchanged"
            assertEquals(original.getCloudRouters(), repriced.getCloudRouters());
            assertEquals(original.getProviderConnections(), repriced.getProviderConnections());
            assertEquals(original.getBackboneLinks(), repriced.getBackboneLinks());
            assertEquals(original.getRoutingProtocols(), repriced.getRoutingProtocols());
            assertEquals(original.getValidationErrors(), repriced.getValidationErrors());
            assertEquals(original.getDeferredValidations(), repriced.getDeferredValidations());
        }

        /**
         * Enforces DeploymentWizard.Builder.notifications(...) javadoc: "<strong>Every</strong>
         * address is applied to every planned Cloud Router and connection (Fabric mandates at
         * least one recipient per Cloud Router, error {@code EQ-3040013}) — all of them are sent
         * on the wire bodies ... never just the first."
         */
        @Test
        @DisplayName("every notification address is applied to every planned Cloud Router — never just the first")
        void everyNotificationAppliedToEveryRouter() {
            DeploymentPlan plan = offlineWizard("noc@example.com", "ops@example.com").plan();

            assertFalse(plan.getCloudRouters().isEmpty());
            for (PlannedCloudRouter router : plan.getCloudRouters()) {
                assertEquals(List.of("noc@example.com", "ops@example.com"),
                        router.getNotificationEmails(),
                        () -> "router '" + router.getName() + "' must carry ALL configured addresses");
            }
        }

        /**
         * Enforces DeploymentWizard.Builder.notifications(...) javadoc:
         * "@throws IllegalArgumentException if an address is {@code null} or blank".
         */
        @Test
        @DisplayName("a blank notification address throws IllegalArgumentException")
        void blankNotificationThrows() {
            FabricGateway bare = mock(FabricGateway.class);
            DeploymentWizard.Builder builder = DeploymentWizard.builder(bare, twoMetroResult());
            assertThrows(IllegalArgumentException.class, () -> builder.notifications("  "));
            assertThrows(IllegalArgumentException.class, () -> builder.notifications((String) null));
        }

        /**
         * Enforces the wizard's name-generation javadoc (PlanNames, observed through the public
         * API): "Generation is deterministic: for a given optimization result and builder
         * configuration the same names are produced every time".
         */
        @Test
        @DisplayName("plan generation is deterministic: identical config yields identical names")
        void planGenerationIsDeterministic() {
            OptimizationResult sharedResult = twoMetroResult();
            DeploymentPlan p1 = offlineWizard(sharedResult, "noc@example.com").plan();
            DeploymentPlan p2 = offlineWizard(sharedResult, "noc@example.com").plan();

            assertEquals(namesOf(p1), namesOf(p2),
                    "two plans from the same result + configuration must generate identical names");
        }

        private List<String> namesOf(DeploymentPlan plan) {
            List<String> names = new ArrayList<>();
            plan.getCloudRouters().forEach(r -> names.add(r.getName()));
            plan.getProviderConnections().forEach(c -> names.add(c.getName()));
            plan.getBackboneLinks().forEach(l -> names.add(l.getName()));
            plan.getRoutingProtocols().forEach(p -> names.add(p.getName()));
            return names;
        }
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════

    /**
     * A minimal test card: records its consultations into {@code consulted} and quotes the given
     * monthly figure for connections and routers — or nothing at all when {@code monthly} is null.
     * Only the two abstract lookups are implemented, so the interface DEFAULTS for egress and
     * colocation are what this class exercises.
     */
    private static RateCard recordingCard(String label, List<String> consulted, BigDecimal monthly) {
        return new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps,
                                                   MetroCode metro, Term term) {
                consulted.add(label);
                return quote();
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
                consulted.add(label);
                return quote();
            }

            private Optional<PriceQuote> quote() {
                return monthly == null
                        ? Optional.empty()
                        : Optional.of(PriceQuote.of(monthly, BigDecimal.ZERO, USD, PriceSource.ESTIMATE));
            }

            @Override
            public PriceSource source() {
                return PriceSource.ESTIMATE;
            }
        };
    }

    private static DeploymentWizard.Builder offlineWizard(String... notifications) {
        return offlineWizard(twoMetroResult(),
                notifications.length == 0 ? new String[] {"noc@example.com"} : notifications);
    }

    /**
     * An offline wizard on a bare stub gateway (no HTTP): live validation layers self-report as
     * skipped, which keeps these doc-contract tests fast, deterministic and network-free.
     */
    private static DeploymentWizard.Builder offlineWizard(OptimizationResult result, String... notifications) {
        FabricGateway bare = mock(FabricGateway.class);
        return DeploymentWizard.builder(bare, result)
                .routerPackage("STANDARD")
                .routerNamePrefix("FCR")
                .providerConnectionType(ConnectionType.IP_VC)
                .notifications(notifications)
                .rateCard(fixedCard());
    }

    /** A rate card that always quotes fixed USD figures, so pricing is deterministic and offline. */
    private static RateCard fixedCard() {
        return new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps,
                                                   MetroCode metro, Term term) {
                return Optional.of(PriceQuote.of(BigDecimal.valueOf(500), BigDecimal.ZERO, USD,
                        PriceSource.ESTIMATE));
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
                return Optional.of(PriceQuote.of(BigDecimal.valueOf(300), BigDecimal.ZERO, USD,
                        PriceSource.ESTIMATE));
            }

            @Override
            public PriceSource source() {
                return PriceSource.ESTIMATE;
            }
        };
    }

    /**
     * Two metros: DC with AWS available and one AWS-dependent workload placed there, plus DA with
     * no providers — one provider connection and one backbone link (mirrors the wizard WireMock
     * suites' canonical two-metro fixture).
     */
    private static OptimizationResult twoMetroResult() {
        MetroScore score = new MetroScore(90.0, Collections.emptyList());
        WorkloadSpec ml = WorkloadSpec.builder()
                .label("ML Training").bandwidthMbps(8000)
                .dependsOnProviders(List.of(ProviderRequirement.builder().label("AWS").build()))
                .build();

        return OptimizationResult.builder()
                .request(OptimizationRequest.builder().workloads(List.of(ml)).build())
                .recommendations(List.of(
                        MetroRecommendation.builder()
                                .rank(1).metroId(DC).metroName("Ashburn").score(score).reasons(List.of("Primary"))
                                .availableProviders(List.of(ProviderAvailability.builder()
                                        .providerLabel("AWS")
                                        .available(true)
                                        .sellerRegions(List.of("us-east-1"))
                                        .serviceProfileUuid("sp-aws")
                                        .build()))
                                .build(),
                        MetroRecommendation.builder()
                                .rank(2).metroId(DA).metroName("Dallas").score(score).reasons(List.of("Secondary"))
                                .availableProviders(Collections.emptyList())
                                .build()))
                .topology(new DeploymentTopology(List.of(WorkloadPlacement.builder()
                        .workloadLabel("ML Training")
                        .assignedMetro(DC)
                        .reasoning("AWS at DC")
                        .build())))
                .computedAt(Instant.now())
                .computeTimeMs(5)
                .build();
    }
}
