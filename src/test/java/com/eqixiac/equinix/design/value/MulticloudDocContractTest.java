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

package com.eqixiac.equinix.design.value;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.design.value.ratecard.CustomRateCard;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkRequest;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.design.value.savings.DataUnit;
import com.eqixiac.equinix.design.value.savings.MulticloudPathComparison;
import com.eqixiac.equinix.design.value.savings.SavingsCalculator;
import com.eqixiac.equinix.design.value.savings.SavingsEstimate;
import com.eqixiac.equinix.design.value.tco.CostBreakdown;
import com.eqixiac.equinix.design.value.tco.DeploymentArchetype;
import com.eqixiac.equinix.design.value.tco.TcoCalculator;
import com.eqixiac.equinix.design.value.tco.TcoComparison;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Doc-contract tests for the native multicloud path in the value layer. Each test quotes the
 * javadoc sentence it enforces. If a promise is changed on purpose, change the javadoc and the
 * quote here in the same commit.
 */
@DisplayName("design/value multicloud — documented behavioral promises (doc contracts)")
class MulticloudDocContractTest {

    private static final ReferenceRateCard REFERENCE = ReferenceRateCard.standard();

    private static MulticloudLinkRequest.MulticloudLinkRequestBuilder awsToGoogle(int mbps) {
        return MulticloudLinkRequest.builder()
                .providerA(CloudProviderType.AWS).regionA("us-east-1")
                .providerZ(CloudProviderType.GOOGLE_CLOUD).regionZ("us-east4")
                .bandwidthMbps(mbps);
    }

    private static TcoCalculator.Builder tco() {
        return TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(REFERENCE);
    }

    private static SavingsCalculator.Builder savings() {
        return SavingsCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(REFERENCE);
    }

    @Nested
    @DisplayName("RateCard.multicloudLink and the quote types")
    class RateCardSeam {

        /**
         * Enforces RateCard.multicloudLink javadoc: "A present result may still have one or both
         * sides unpriced: a side is priced only from a figure the card holds, and is otherwise
         * left empty with a reason" — and MulticloudLinkQuote's: "A side with no verifiable price
         * is absent ({@link java.util.Optional#empty()}), never zero".
         */
        @Test
        @DisplayName("an unverifiable side is empty with a reason, never a zero price")
        void unverifiableSideIsEmptyWithAReason() {
            MulticloudLinkQuote quote = REFERENCE.multicloudLink(awsToGoogle(1_000).build()).orElseThrow();

            assertTrue(quote.getSideA().isEmpty());
            assertNotNull(quote.getSideAUnpricedReason());
            assertFalse(quote.getSideAUnpricedReason().isBlank());
            assertTrue(quote.getSideZ().isPresent());
            assertTrue(quote.getSideZ().get().getMonthlyRecurring().signum() > 0);
        }

        /**
         * Enforces MulticloudLinkQuote javadoc: "{@code combinedMonthly()} is present only when
         * both sides are priced and share one known currency."
         */
        @Test
        @DisplayName("combinedMonthly() is present only for two priced sides in one currency")
        void combinedMonthlyNeedsTwoPricedSidesInOneCurrency() {
            assertTrue(REFERENCE.multicloudLink(awsToGoogle(10_000).build()).orElseThrow()
                    .combinedMonthly().isPresent());
            assertTrue(REFERENCE.multicloudLink(awsToGoogle(1_000).build()).orElseThrow()
                    .combinedMonthly().isEmpty(), "one side unpriced");

            CustomRateCard eurGoogle = CustomRateCard.builder().currency("EUR")
                    .multicloudLinkRate(CloudProviderType.GOOGLE_CLOUD, 10_000, new BigDecimal("12000")).build();
            assertTrue(RateCard.layered(eurGoogle, REFERENCE).multicloudLink(awsToGoogle(10_000).build())
                    .orElseThrow().combinedMonthly().isEmpty(), "USD and EUR sides");
        }

        /**
         * Enforces LayeredRateCard javadoc: "the earliest card that prices a side supplies it,
         * and a side no card prices stays empty with every card's reason."
         */
        @Test
        @DisplayName("layered(...) resolves each side independently, earliest card first")
        void layeredResolvesEachSideIndependently() {
            CustomRateCard awsOnly = CustomRateCard.builder()
                    .multicloudLinkRate(CloudProviderType.AWS, 10_000, new BigDecimal("7000")).build();

            MulticloudLinkQuote quote = RateCard.layered(awsOnly, REFERENCE)
                    .multicloudLink(awsToGoogle(10_000).build()).orElseThrow();

            assertEquals(0, new BigDecimal("7000").compareTo(quote.getSideA().orElseThrow().getMonthlyRecurring()),
                    "the custom card is earlier and prices the AWS side");
            assertEquals(0, new BigDecimal("13870.00").compareTo(quote.getSideZ().orElseThrow().getMonthlyRecurring()),
                    "the reference card fills the side the custom card left empty");
        }

        /**
         * Enforces CustomRateCard javadoc: "The card does not model the AWS free tier: declare a
         * zero rate for the covered size to express it."
         */
        @Test
        @DisplayName("CustomRateCard ignores useAwsFreeTier; a declared zero rate expresses it")
        void customCardIgnoresTheFreeTierFlag() {
            CustomRateCard paid = CustomRateCard.builder()
                    .multicloudLinkRate(CloudProviderType.AWS, 500, new BigDecimal("400")).build();
            PriceQuote aws = paid.multicloudLink(awsToGoogle(500).useAwsFreeTier(true).build())
                    .orElseThrow().getSideA().orElseThrow();
            assertEquals(0, new BigDecimal("400").compareTo(aws.getMonthlyRecurring()));

            CustomRateCard free = CustomRateCard.builder()
                    .multicloudLinkRate(CloudProviderType.AWS, 500, BigDecimal.ZERO).build();
            assertEquals(0, BigDecimal.ZERO.compareTo(free.multicloudLink(awsToGoogle(500).build())
                    .orElseThrow().getSideA().orElseThrow().getMonthlyRecurring()));
        }
    }

    @Nested
    @DisplayName("ReferenceRateCard native-link data")
    class ReferenceData {

        /**
         * Enforces ReferenceRateCard javadoc: "The lookup is an exact match on bandwidth ... It
         * does not round up, interpolate or extrapolate, unlike {@code connection(...)}".
         */
        @Test
        @DisplayName("multicloudLink matches sizes exactly where connection(...) rounds up and extrapolates")
        void nativeLinkLookupDoesNotRoundUpUnlikeConnection() {
            // connection(...): 3 Gbps rounds up to the 10 Gbps row; 50 Gbps extrapolates.
            assertEquals(0, new BigDecimal("350").compareTo(REFERENCE.connection(
                    ConnectionType.EVPL_VC, 3_000, null, Term.MONTH_12).orElseThrow().getMonthlyRecurring()));
            assertTrue(REFERENCE.connection(ConnectionType.EVPL_VC, 50_000, null, Term.MONTH_12)
                    .orElseThrow().getNote().contains("EXTRAPOLATED"));

            // multicloudLink(...): neither.
            assertTrue(REFERENCE.multicloudLink(awsToGoogle(3_000).build()).orElseThrow().getSideZ().isEmpty());
            assertTrue(REFERENCE.multicloudLink(awsToGoogle(50_000).build()).orElseThrow().getSideZ().isEmpty());
        }

        /**
         * Enforces ReferenceRateCard javadoc: "The AWS free tier ... is applied only when the
         * request sets {@code useAwsFreeTier}; it is never a default." — and
         * MulticloudLinkRequest's: "The flag is never inferred".
         */
        @Test
        @DisplayName("the AWS free tier is applied only on an explicit opt-in")
        void freeTierIsOptIn() {
            assertTrue(REFERENCE.multicloudLink(awsToGoogle(500).build()).orElseThrow().getSideA().isEmpty());
            assertEquals(0, BigDecimal.ZERO.compareTo(REFERENCE.multicloudLink(
                    awsToGoogle(500).useAwsFreeTier(true).build()).orElseThrow()
                    .getSideA().orElseThrow().getMonthlyRecurring()));
        }

        /**
         * Enforces EgressPath.MULTICLOUD_INTERCONNECT javadoc: "For any other provider the
         * reference card returns empty (unknown), not zero."
         */
        @Test
        @DisplayName("an unverified per-GB rate on the native link is empty, not zero")
        void unknownNativePerGbRateIsEmpty() {
            for (CloudProviderType provider : CloudProviderType.values()) {
                boolean published = provider == CloudProviderType.AWS || provider == CloudProviderType.GOOGLE_CLOUD;
                assertEquals(published, REFERENCE.egress(provider, null, EgressPath.MULTICLOUD_INTERCONNECT, null)
                        .isPresent(), String.valueOf(provider));
            }
        }
    }

    @Nested
    @DisplayName("TCO")
    class Tco {

        /**
         * Enforces DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT javadoc: "the breakdown
         * carries an explicit zero-cost data-transfer line; that line appears only when both
         * sides are priced."
         */
        @Test
        @DisplayName("the zero-cost data-transfer line appears only when both sides are priced")
        void zeroDataTransferLineOnlyWhenBothSidesPriced() {
            String line = "Data transfer over the native link (none charged)";
            CostBreakdown priced = tco().toCloud(CloudProviderType.GOOGLE_CLOUD).compare()
                    .breakdown(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT).orElseThrow();
            CostBreakdown partial = tco().toCloud(CloudProviderType.GOOGLE_CLOUD).bandwidthMbps(1_000).compare()
                    .breakdown(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT).orElseThrow();

            assertTrue(priced.getLineItems().containsKey(line));
            assertFalse(partial.getLineItems().containsKey(line));
        }

        /**
         * Enforces DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT javadoc: "a partially
         * priced archetype is never recommended."
         */
        @Test
        @DisplayName("a partially priced native archetype is never recommended")
        void partialNativeArchetypeIsNeverRecommended() {
            // Only the native archetype and the (far costlier) internet baseline compete; the
            // native partial total (2,555) is the lowest number on the page.
            TcoComparison tco = tco().toCloud(CloudProviderType.GOOGLE_CLOUD).bandwidthMbps(1_000)
                    .archetypes(DeploymentArchetype.PUBLIC_CLOUD_INTERNET,
                            DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT)
                    .compare();

            assertEquals(DeploymentArchetype.PUBLIC_CLOUD_INTERNET, tco.getRecommended());
        }

        /**
         * Enforces DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT javadoc: "Requires a peer
         * cloud: requesting this archetype without {@code TcoCalculator.Builder.toCloud(...)}
         * fails at {@code compare()}."
         */
        @Test
        @DisplayName("requesting the native archetype without toCloud(...) fails at compare()")
        void nativeArchetypeWithoutPeerFailsAtCompare() {
            TcoCalculator.Builder b = tco().archetypes(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT);
            assertThrows(IllegalArgumentException.class, b::compare);
        }

        /**
         * Enforces TcoCalculator.Builder javadoc: "Constraints that span two levers are checked
         * at {@code compare()}, because levers may be set in any order".
         */
        @Test
        @DisplayName("cloud-to-cloud levers may be set in any order")
        void leversMayBeSetInAnyOrder() {
            TcoComparison tco = TcoCalculator.builder(null)
                    .useAwsFreeTier(false).pathTier(1).toRegion("us-east4")
                    .reverseEgress(10, DataUnit.TERABYTE)
                    .archetypes(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT)
                    .toCloud(CloudProviderType.GOOGLE_CLOUD)
                    .fromCloud(CloudProviderType.AWS)
                    .bandwidthMbps(10_000).rateCard(REFERENCE)
                    .compare();

            assertTrue(tco.breakdown(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT).orElseThrow().isPriced());
        }

        /**
         * Enforces TcoCalculator javadoc: "Without {@code toCloud(...)} the comparison, its
         * defaults and its output are the single-cloud ones described above." — and
         * CostBreakdown.getProvenance's: "{@code null} for a single-cloud comparison, whose
         * breakdowns are unchanged."
         */
        @Test
        @DisplayName("without toCloud(...) the comparison is the single-cloud one")
        void withoutPeerTheComparisonIsSingleCloud() {
            TcoComparison tco = tco().compare();

            assertEquals(3, tco.getBreakdowns().size());
            assertTrue(tco.breakdown(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT).isEmpty());
            assertNull(tco.getTrafficNote());
            tco.getBreakdowns().forEach(b -> assertNull(b.getProvenance()));
            assertEquals(0, new BigDecimal("4292.50").compareTo(
                    tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow().getMonthlyTotal()));
        }
    }

    @Nested
    @DisplayName("Savings")
    class Savings {

        /**
         * Enforces MulticloudPathComparison javadoc: "A path total is {@code null} when any of its
         * inputs is unpriced ... A {@code null} total is not a zero cost."
         */
        @Test
        @DisplayName("an unpriced path total is null, and is not ranked as the cheapest")
        void unpricedPathTotalIsNullNotZero() {
            MulticloudPathComparison c = savings().toCloud(CloudProviderType.GOOGLE_CLOUD).bandwidthMbps(1_000)
                    .calculate().getMulticloudComparison();

            assertNull(c.getNativeMonthlyCost());
            assertNotEquals(EgressPath.MULTICLOUD_INTERCONNECT, c.getLowestCostPath());
        }

        /**
         * Enforces MulticloudPathComparison javadoc: "It is a property of the prices, not of the
         * declared volumes."
         */
        @Test
        @DisplayName("the break-even rate does not move with the declared volume")
        void breakEvenIsIndependentOfVolume() {
            assertEquals(
                    savings().toCloud(CloudProviderType.GOOGLE_CLOUD).egress(1, DataUnit.TERABYTE)
                            .calculate().breakEvenSustainedMbps(),
                    savings().toCloud(CloudProviderType.GOOGLE_CLOUD).egress(900, DataUnit.TERABYTE)
                            .reverseEgress(3, DataUnit.TERABYTE).calculate().breakEvenSustainedMbps());
        }

        /**
         * Enforces SavingsEstimate javadoc: "The fields above it are unaffected by the peer cloud
         * ... and {@code isComplete()} does not consider the cloud-to-cloud section."
         */
        @Test
        @DisplayName("the single-cloud fields and isComplete() ignore the peer cloud")
        void singleCloudFieldsIgnoreThePeer() {
            SavingsEstimate single = savings().calculate();
            // 1 Gbps: the native path is unpriced, yet the single-cloud estimate is complete.
            SavingsEstimate single1g = savings().bandwidthMbps(1_000).calculate();
            SavingsEstimate peer1g = savings().bandwidthMbps(1_000).toCloud(CloudProviderType.GOOGLE_CLOUD).calculate();

            assertTrue(single.isComplete());
            assertEquals(single1g.isComplete(), peer1g.isComplete());
            assertEquals(single1g.getNetMonthlySavings(), peer1g.getNetMonthlySavings());
            assertEquals(single1g.getInternetEgressMonthlyCost(), peer1g.getInternetEgressMonthlyCost());
            assertNull(peer1g.getMulticloudComparison().getNativeMonthlyCost());
        }
    }
}
