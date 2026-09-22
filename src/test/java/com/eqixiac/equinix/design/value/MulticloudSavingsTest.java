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
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.savings.DataUnit;
import com.eqixiac.equinix.design.value.savings.MulticloudPathComparison;
import com.eqixiac.equinix.design.value.savings.SavingsCalculator;
import com.eqixiac.equinix.design.value.savings.SavingsEstimate;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The cloud-to-cloud section of the {@link SavingsCalculator}: three path totals on the same
 * two-way traffic, and the break-even sustained rate between the flat-fee native link and the
 * per-GB Equinix path.
 */
class MulticloudSavingsTest {

    private static SavingsCalculator.Builder awsToGoogle() {
        return SavingsCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .toCloud(CloudProviderType.GOOGLE_CLOUD).toRegion("us-east4")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(ReferenceRateCard.standard());
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertNotNull(actual, "expected " + expected + " but the figure is null");
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    // ── The break-even formula ──

    /**
     * Worked example from cited list prices (all USD, retrieved 2026-09-21 unless noted).
     * <ul>
     *   <li>Native 10 Gbps: AWS Interconnect - multicloud tier 1 at 12.33/h
     *       (aws.amazon.com/interconnect/multicloud/pricing) plus Google Partner Cross-Cloud
     *       Interconnect North America at 19.00/h
     *       (cloud.google.com/network-connectivity/docs/interconnect/pricing):
     *       31.33 x 730 h = 22,870.90 per month, no per-GB charge on either side.</li>
     *   <li>Per-GB colocation path: AWS Direct Connect 10G dedicated port at 2.25/h
     *       (aws.amazon.com/directconnect/pricing, bundled 2026-06) plus Google Cross-Cloud
     *       Interconnect 10 Gbps at 5.60/h (the pricing example on the Google page above):
     *       7.85 x 730 h = 5,730.50 per month fixed.</li>
     *   <li>Per-GB: AWS Direct Connect data transfer out 0.02/GB; Google Cloud Interconnect
     *       0.02/GiB.</li>
     * </ul>
     * Assumptions: symmetric traffic; a 730-hour month; 1 GB = 10^9 bytes; Google's per-GiB rate
     * taken as 0.02 per GB (the second assertion removes that approximation).
     */
    @Test
    void breakEvenReproducesTheWorkedExampleFromCitedListPrices() {
        BigDecimal nativeFixed = new BigDecimal("12.33").add(new BigDecimal("19.00")).multiply(BigDecimal.valueOf(730));
        BigDecimal colocationFixed = new BigDecimal("2.25").add(new BigDecimal("5.60")).multiply(BigDecimal.valueOf(730));
        assertMoney("22870.90", nativeFixed);
        assertMoney("5730.50", colocationFixed);

        // g* = 17,140.40 / 0.04 = 428,510 GB each way
        //    = 428,510 x 8000 Mb / 2,628,000 s = 1,304.44 Mbps each way = 2,608.9 Mbps in total.
        assertEquals(new BigDecimal("2608.9"), MulticloudPathComparison.breakEvenSustainedMbps(
                nativeFixed, colocationFixed, new BigDecimal("0.04"), BigDecimal.ZERO).orElseThrow());

        // With Google's 0.02/GiB converted to 0.0186/GB (as bundled): 17,140.40 / 0.0386 GB each way.
        assertEquals(new BigDecimal("2703.5"), MulticloudPathComparison.breakEvenSustainedMbps(
                nativeFixed, colocationFixed, new BigDecimal("0.0386"), BigDecimal.ZERO).orElseThrow());
    }

    @Test
    void breakEvenUnitConstantsAreTheDocumentedOnes() {
        assertEquals(730L * 3600L, MulticloudPathComparison.SECONDS_PER_MONTH);
        assertEquals(8_000, MulticloudPathComparison.MEGABITS_PER_GB);
    }

    @Test
    void breakEvenIsEmptyWhenAnInputIsMissingOrNoCrossoverExists() {
        BigDecimal n = new BigDecimal("22870.90");
        BigDecimal e = new BigDecimal("5730.50");
        BigDecimal perGb = new BigDecimal("0.04");

        assertTrue(MulticloudPathComparison.breakEvenSustainedMbps(null, e, perGb, BigDecimal.ZERO).isEmpty());
        assertTrue(MulticloudPathComparison.breakEvenSustainedMbps(n, null, perGb, BigDecimal.ZERO).isEmpty());
        assertTrue(MulticloudPathComparison.breakEvenSustainedMbps(n, e, null, BigDecimal.ZERO).isEmpty());
        assertTrue(MulticloudPathComparison.breakEvenSustainedMbps(n, e, perGb, null).isEmpty());
        assertTrue(MulticloudPathComparison.breakEvenSustainedMbps(e, n, perGb, BigDecimal.ZERO).isEmpty(),
                "native fixed fee below the per-GB path's fixed cost: native is cheaper at every volume");
        assertTrue(MulticloudPathComparison.breakEvenSustainedMbps(n, n, perGb, BigDecimal.ZERO).isEmpty());
        assertTrue(MulticloudPathComparison.breakEvenSustainedMbps(n, e, perGb, perGb).isEmpty(),
                "equal per-GB rates: the per-GB path is cheaper at every volume");
    }

    // ── Estimate ──

    @Test
    void peerCloudAddsTheNativePathAsAThirdOptionOnTheSameTwoWayTraffic() {
        MulticloudPathComparison c = awsToGoogle().calculate().getMulticloudComparison();

        assertNotNull(c);
        assertEquals(CloudProviderType.AWS, c.getProviderA());
        assertEquals(CloudProviderType.GOOGLE_CLOUD, c.getProviderZ());
        assertMoney("100000", c.getForwardEgressGb());
        assertMoney("100000", c.getReverseEgressGb());
        assertTrue(c.isReverseEgressAssumedSymmetric());

        assertMoney("20180.00", c.getInternetMonthlyCost());        // 9,000 + 11,180
        assertMoney("4018.50", c.getEquinixFixedMonthlyCost());     // 2 x 350 + 1,642.50 + 1,676
        assertMoney("3860.00", c.getEquinixEgressMonthlyCost());    // 2,000 + 1,860
        assertMoney("7878.50", c.getEquinixMonthlyCost());
        assertMoney("0", c.getEquinixSetupCost());
        assertMoney("22870.90", c.getNativeFixedMonthlyCost());     // (12.33 + 19.00) x 730
        assertMoney("0", c.getNativeDataTransferMonthlyCost());
        assertMoney("22870.90", c.getNativeMonthlyCost());
        assertEquals(EgressPath.PRIVATE, c.getLowestCostPath());
        assertEquals("USD", c.getCurrency());
        assertTrue(c.getNativeLinkQuote().isFullyPriced());
    }

    @Test
    void breakEvenIsComputedFromTheQuotesActuallyResolved() {
        SavingsEstimate estimate = awsToGoogle().calculate();

        // (22,870.90 - 4,018.50) / (0.02 + 0.0186) = 488,404.145 GB each way
        //   x 8000 / 2,628,000 = 1,486.77 Mbps each way -> 2,973.5 Mbps in total.
        assertEquals(new BigDecimal("2973.5"), estimate.breakEvenSustainedMbps().orElseThrow());
        assertEquals(new BigDecimal("2973.5"), estimate.getMulticloudComparison().getBreakEvenSustainedMbps());
        assertEquals(MulticloudPathComparison.breakEvenSustainedMbps(new BigDecimal("22870.90"),
                        new BigDecimal("4018.50"), new BigDecimal("0.0386"), BigDecimal.ZERO),
                estimate.breakEvenSustainedMbps());
        assertTrue(estimate.getMulticloudComparison().getNotes().stream()
                .anyMatch(n -> n.startsWith("Break-even = 2 x ((native fixed USD 22870.90 - Equinix fixed USD 4018.50)")),
                estimate.getMulticloudComparison().getNotes().toString());
    }

    @Test
    void breakEvenDoesNotDependOnTheDeclaredVolumes() {
        Optional<BigDecimal> small = awsToGoogle().egress(1, DataUnit.GIGABYTE).calculate().breakEvenSustainedMbps();
        Optional<BigDecimal> large = awsToGoogle().egress(5, DataUnit.PETABYTE).calculate().breakEvenSustainedMbps();

        assertEquals(new BigDecimal("2973.5"), small.orElseThrow());
        assertEquals(small, large);
    }

    @Test
    void lowestCostPathFollowsTheDeclaredVolumes() {
        assertEquals(EgressPath.PRIVATE, awsToGoogle().calculate().getMulticloudComparison().getLowestCostPath());
        // 1 PB each way: Equinix = 4,018.50 + 38,600 = 42,618.50 > native 22,870.90.
        MulticloudPathComparison high = awsToGoogle().egress(1, DataUnit.PETABYTE).calculate()
                .getMulticloudComparison();
        assertMoney("42618.50", high.getEquinixMonthlyCost());
        assertEquals(EgressPath.MULTICLOUD_INTERCONNECT, high.getLowestCostPath());
    }

    @Test
    void aCloudRouterRaisesTheEquinixFixedCostAndLowersTheBreakEven() {
        SavingsEstimate withRouter = awsToGoogle().includeCloudRouter("STANDARD").calculate();

        assertMoney("5218.50", withRouter.getMulticloudComparison().getEquinixFixedMonthlyCost()); // + 1,200
        // (22,870.90 - 5,218.50) / 0.0386 x 16000 / 2,628,000 = 2,784.27
        assertEquals(new BigDecimal("2784.3"), withRouter.breakEvenSustainedMbps().orElseThrow());
        assertFalse(withRouter.getMulticloudComparison().getNotes().stream().anyMatch(n -> n.contains("no A-side")));

        assertTrue(awsToGoogle().calculate().getMulticloudComparison().getNotes().stream()
                        .anyMatch(n -> n.contains("no A-side is priced") && n.contains("break-even rate is overstated")),
                "without a router the estimate states that the Equinix fixed cost has no A-side");
    }

    @Test
    void breakEvenIsEmptyAndTheNativeTotalNullWhenASideIsUnpriced() {
        SavingsEstimate oneGig = awsToGoogle().bandwidthMbps(1_000).calculate();
        MulticloudPathComparison c = oneGig.getMulticloudComparison();

        assertTrue(oneGig.breakEvenSustainedMbps().isEmpty(), "AWS publishes no 1 Gbps rate");
        assertNull(c.getNativeMonthlyCost(), "unpriced is null, not zero");
        assertNull(c.getNativeFixedMonthlyCost());
        assertNotNull(c.getEquinixMonthlyCost());
        assertEquals(EgressPath.PRIVATE, c.getLowestCostPath(), "ranked among the paths that priced");
        assertTrue(c.getNotes().stream().anyMatch(n -> n.contains("The AWS side is unpriced")), c.getNotes().toString());
        assertTrue(c.getNotes().stream().anyMatch(n -> n.startsWith("Break-even not computed")),
                c.getNotes().toString());
        assertFalse(c.getNativeLinkQuote().isFullyPriced());
    }

    @Test
    void breakEvenAboveTheLinkCapacityIsReportedWithANote() {
        // A 1 Gbps link carries at most 2,000 Mbps summed over both directions.
        RateCard card = RateCard.layered(
                CustomRateCard.builder()
                        .multicloudLinkHourlyRate(CloudProviderType.AWS, 1_000, new BigDecimal("1.37")).build(),
                ReferenceRateCard.standard());
        MulticloudPathComparison c = awsToGoogle().bandwidthMbps(1_000).rateCard(card).calculate()
                .getMulticloudComparison();

        // Native = (1.37 + 3.50) x 730 = 3,555.10; Equinix fixed = 2 x 150 + 219 + 1,676 = 2,195.00.
        assertMoney("3555.10", c.getNativeFixedMonthlyCost());
        assertMoney("2195.00", c.getEquinixFixedMonthlyCost());
        assertEquals(new BigDecimal("214.5"), c.getBreakEvenSustainedMbps());
        assertFalse(c.getNotes().stream().anyMatch(n -> n.contains("exceeds the link's capacity")));

        // The bundled 10 Gbps figures: 2,973.5 Mbps is within the 20,000 Mbps capacity.
        assertFalse(awsToGoogle().calculate().getMulticloudComparison().getNotes().stream()
                .anyMatch(n -> n.contains("exceeds the link's capacity")));

        // A costly negotiated native link pushes the break-even past the capacity of a 1 Gbps link.
        RateCard costly = RateCard.layered(
                CustomRateCard.builder()
                        .multicloudLinkRate(CloudProviderType.AWS, 1_000, new BigDecimal("20000")).build(),
                ReferenceRateCard.standard());
        MulticloudPathComparison over = awsToGoogle().bandwidthMbps(1_000).rateCard(costly).calculate()
                .getMulticloudComparison();
        assertTrue(over.getBreakEvenSustainedMbps().compareTo(BigDecimal.valueOf(2_000)) > 0);
        assertTrue(over.getNotes().stream().anyMatch(n -> n.contains("exceeds the link's capacity (2000 Mbps")),
                over.getNotes().toString());
    }

    @Test
    void noBreakEvenWhenTheNativeLinkIsCheaperAtEveryVolume() {
        RateCard cheapNative = RateCard.layered(
                CustomRateCard.builder()
                        .multicloudLinkRate(CloudProviderType.AWS, 10_000, new BigDecimal("1000"))
                        .multicloudLinkRate(CloudProviderType.GOOGLE_CLOUD, 10_000, new BigDecimal("1000")).build(),
                ReferenceRateCard.standard());
        MulticloudPathComparison c = awsToGoogle().rateCard(cheapNative).calculate().getMulticloudComparison();

        assertNull(c.getBreakEvenSustainedMbps());
        assertEquals(EgressPath.MULTICLOUD_INTERCONNECT, c.getLowestCostPath());
        assertTrue(c.getNotes().stream().anyMatch(n -> n.startsWith("No break-even: the native link's fixed fee")
                        && n.contains("its per-GB rate does not exceed the Equinix path's")),
                c.getNotes().toString());
    }

    @Test
    void aReversedCrossingIsReportedAsABreakEvenWithTheOppositeSense() {
        // Regression: native fixed fee (2,000) BELOW the Equinix fixed cost (4,018.50) but native
        // per-GB (0.05 + 0.05) ABOVE the Equinix per-GB (0.0386). The lines cross at
        // (4,018.50 - 2,000) / (0.10 - 0.0386) = 32,874.4 GB each way = 200.1 Mbps summed, and
        // above it the Equinix path is cheaper. The previous code returned empty and stated that
        // the native path costs the same or less at every volume, while the declared 1 PB each
        // way priced Equinix at 42,618.50 against native 102,000.00.
        RateCard reversed = RateCard.layered(
                CustomRateCard.builder()
                        .multicloudLinkRate(CloudProviderType.AWS, 10_000, new BigDecimal("1000"))
                        .multicloudLinkRate(CloudProviderType.GOOGLE_CLOUD, 10_000, new BigDecimal("1000"))
                        .egressRate(CloudProviderType.AWS, EgressPath.MULTICLOUD_INTERCONNECT, new BigDecimal("0.05"))
                        .egressRate(CloudProviderType.GOOGLE_CLOUD, EgressPath.MULTICLOUD_INTERCONNECT, new BigDecimal("0.05"))
                        .build(),
                ReferenceRateCard.standard());
        MulticloudPathComparison c = awsToGoogle().egress(1, DataUnit.PETABYTE).rateCard(reversed).calculate()
                .getMulticloudComparison();

        assertMoney("42618.50", c.getEquinixMonthlyCost());
        assertMoney("102000.00", c.getNativeMonthlyCost());
        assertEquals(EgressPath.PRIVATE, c.getLowestCostPath());
        assertEquals(new BigDecimal("200.1"), c.getBreakEvenSustainedMbps());
        assertFalse(c.isNativeCheaperAboveBreakEven(), "the Equinix path is cheaper above this break-even");
        assertTrue(c.getNotes().stream().anyMatch(n -> n.contains("the sense is reversed")
                && n.contains("above it the Equinix path does")), c.getNotes().toString());
        assertFalse(c.getNotes().stream().anyMatch(n -> n.startsWith("No break-even")), c.getNotes().toString());

        String md = awsToGoogle().egress(1, DataUnit.PETABYTE).rateCard(reversed).calculate().toMarkdown();
        assertTrue(md.contains("Below it the native link costs less per month; above it the Equinix path does."), md);

        // The static formula agrees, and the usual case keeps its sense.
        assertEquals(new BigDecimal("200.1"), MulticloudPathComparison.breakEvenSustainedMbps(
                new BigDecimal("2000"), new BigDecimal("4018.50"), new BigDecimal("0.0386"), new BigDecimal("0.10"))
                .orElseThrow());
        assertFalse(MulticloudPathComparison.nativeCheaperAboveBreakEven(new BigDecimal("2000"), new BigDecimal("4018.50")));
        assertTrue(MulticloudPathComparison.nativeCheaperAboveBreakEven(new BigDecimal("22870.90"), new BigDecimal("4018.50")));
        assertTrue(awsToGoogle().calculate().getMulticloudComparison().isNativeCheaperAboveBreakEven());
    }

    @Test
    void dominatesAtEveryVolumeCoversEveryNonCrossingCase() {
        BigDecimal lo = new BigDecimal("1000");
        BigDecimal hi = new BigDecimal("5000");
        BigDecimal cheap = new BigDecimal("0.01");
        BigDecimal dear = new BigDecimal("0.05");
        // Native fixed lower and native per-GB not higher: native at every volume.
        assertEquals(Optional.of(EgressPath.MULTICLOUD_INTERCONNECT),
                MulticloudPathComparison.dominatesAtEveryVolume(lo, hi, dear, cheap));
        assertEquals(Optional.of(EgressPath.MULTICLOUD_INTERCONNECT),
                MulticloudPathComparison.dominatesAtEveryVolume(lo, hi, cheap, cheap));
        // Native fixed higher and native per-GB not lower: Equinix at every volume.
        assertEquals(Optional.of(EgressPath.PRIVATE),
                MulticloudPathComparison.dominatesAtEveryVolume(hi, lo, cheap, dear));
        assertEquals(Optional.of(EgressPath.PRIVATE),
                MulticloudPathComparison.dominatesAtEveryVolume(hi, lo, cheap, cheap));
        assertEquals(Optional.of(EgressPath.PRIVATE),
                MulticloudPathComparison.dominatesAtEveryVolume(hi, hi, cheap, dear));
        // A crossing in either sense: no dominant path.
        assertTrue(MulticloudPathComparison.dominatesAtEveryVolume(hi, lo, dear, cheap).isEmpty());
        assertTrue(MulticloudPathComparison.dominatesAtEveryVolume(lo, hi, cheap, dear).isEmpty());
        assertTrue(MulticloudPathComparison.breakEvenSustainedMbps(lo, hi, cheap, dear).isPresent());
        // Equal everywhere: either statement holds; the native one is returned.
        assertEquals(Optional.of(EgressPath.MULTICLOUD_INTERCONNECT),
                MulticloudPathComparison.dominatesAtEveryVolume(hi, hi, cheap, cheap));
        assertTrue(MulticloudPathComparison.dominatesAtEveryVolume(null, hi, cheap, cheap).isEmpty());
    }

    @Test
    void breakEvenIsWithheldWhenAPerGbRateIsInAnotherCurrencyEvenAtZeroVolume() {
        // Regression: at the 0 GB default a per-GB cost of zero never entered the currency check,
        // so 0.02 USD + 0.05 EUR were summed into a USD-labelled break-even.
        RateCard euroPerGb = RateCard.layered(
                CustomRateCard.builder().currency(java.util.Currency.getInstance("EUR"))
                        .egressRate(CloudProviderType.GOOGLE_CLOUD, EgressPath.PRIVATE, new BigDecimal("0.05"))
                        .egressRate(CloudProviderType.GOOGLE_CLOUD, EgressPath.INTERNET, new BigDecimal("0.10"))
                        .build(),
                ReferenceRateCard.standard());
        MulticloudPathComparison zeroVolume = SavingsCalculator.builder(null)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .toCloud(CloudProviderType.GOOGLE_CLOUD).toRegion("us-east4")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(euroPerGb).calculate().getMulticloudComparison();

        assertEquals("USD", zeroVolume.getCurrency());
        assertNotNull(zeroVolume.getEquinixMonthlyCost(), "at zero volume the Equinix path's fixed cost is all USD");
        assertNotNull(zeroVolume.getNativeMonthlyCost());
        assertNull(zeroVolume.getBreakEvenSustainedMbps());
        assertTrue(zeroVolume.getNotes().stream().anyMatch(n -> n.startsWith("Break-even not computed: the per-GB rates")
                && n.contains("GOOGLE_CLOUD private (EUR)")), zeroVolume.getNotes().toString());
        assertFalse(zeroVolume.getNotes().stream().anyMatch(n -> n.contains("0.07")), zeroVolume.getNotes().toString());

        // With a non-zero volume the Equinix path itself is withheld as mixed-currency, as before.
        MulticloudPathComparison withVolume = awsToGoogle().rateCard(euroPerGb).calculate().getMulticloudComparison();
        assertNull(withVolume.getEquinixMonthlyCost());
        assertNull(withVolume.getBreakEvenSustainedMbps());
    }

    @Test
    void anInternetPathInAnotherCurrencyDoesNotWithholdTheNativeVersusEquinixBreakEven() {
        // Regression: the comparison currency followed the internet path (first in list order),
        // so a EUR internet card withheld the two USD paths and the break-even between them.
        RateCard euroInternet = RateCard.layered(
                CustomRateCard.builder().currency(java.util.Currency.getInstance("EUR"))
                        .egressRate(CloudProviderType.AWS, EgressPath.INTERNET, new BigDecimal("0.08"))
                        .egressRate(CloudProviderType.GOOGLE_CLOUD, EgressPath.INTERNET, new BigDecimal("0.10"))
                        .build(),
                ReferenceRateCard.standard());
        MulticloudPathComparison c = awsToGoogle().rateCard(euroInternet).calculate().getMulticloudComparison();

        assertEquals("USD", c.getCurrency(), "the currency the Equinix and native paths share");
        assertMoney("7878.50", c.getEquinixMonthlyCost());
        assertMoney("22870.90", c.getNativeMonthlyCost());
        assertEquals(new BigDecimal("2973.5"), c.getBreakEvenSustainedMbps());
        assertEquals(EgressPath.PRIVATE, c.getLowestCostPath());
        assertNull(c.getInternetMonthlyCost(), "the EUR internet path is withheld, never converted");
        assertTrue(c.getNotes().stream().anyMatch(n -> n.startsWith("Public internet path withheld: it is priced in EUR")),
                c.getNotes().toString());
        assertFalse(c.getNotes().stream().anyMatch(n -> n.startsWith("Break-even not computed")), c.getNotes().toString());
    }

    @Test
    void aCustomAwsRateStatesThatTheRequestedFreeTierWasNotApplied() {
        // Regression: a custom card that prices the AWS side ignores useAwsFreeTier(true) and the
        // layered lookup stops there, so the reference card's declined-free-tier note never
        // surfaced; the traffic note still said "AWS free tier requested".
        RateCard custom = RateCard.layered(
                CustomRateCard.builder()
                        .multicloudLinkRate(CloudProviderType.AWS, 500, new BigDecimal("2000"))
                        .multicloudLinkRate(CloudProviderType.GOOGLE_CLOUD, 500, new BigDecimal("1277.50"))
                        .build(),
                ReferenceRateCard.standard());
        MulticloudPathComparison c = awsToGoogle().bandwidthMbps(500).rateCard(custom).useAwsFreeTier(true)
                .calculate().getMulticloudComparison();

        assertMoney("3277.50", c.getNativeFixedMonthlyCost()); // the declared rate applies, not zero
        assertTrue(c.getNotes().stream().anyMatch(n -> n.startsWith("The AWS free tier was requested but not applied")
                && n.contains("2000 USD/month at 500 Mbps") && n.contains("CustomRateCard does not model the free tier")),
                c.getNotes().toString());
        // Without the opt-in the note is absent.
        assertFalse(awsToGoogle().bandwidthMbps(500).rateCard(custom).calculate().getMulticloudComparison()
                .getNotes().stream().anyMatch(n -> n.contains("free tier was requested")));
    }

    @Test
    void reverseEgressOverridesTheSymmetricAssumption() {
        MulticloudPathComparison c = awsToGoogle().reverseEgress(40, DataUnit.TERABYTE).calculate()
                .getMulticloudComparison();

        assertFalse(c.isReverseEgressAssumedSymmetric());
        assertMoney("40000", c.getReverseEgressGb());
        assertMoney("13472.00", c.getInternetMonthlyCost());     // 9,000 + 40,000 x 0.1118
        assertMoney("2744.00", c.getEquinixEgressMonthlyCost()); // 2,000 + 40,000 x 0.0186
    }

    @Test
    void equinixSetupChargesAreCountedPerConnection() {
        RateCard withSetup = RateCard.layered(
                CustomRateCard.builder()
                        .connectionRate(ConnectionType.EVPL_VC, 10_000, new BigDecimal("400"), new BigDecimal("250"))
                        .build(),
                ReferenceRateCard.standard());
        MulticloudPathComparison c = awsToGoogle().rateCard(withSetup).calculate().getMulticloudComparison();

        assertMoney("500", c.getEquinixSetupCost());
        assertMoney("4118.50", c.getEquinixFixedMonthlyCost()); // 2 x 400 + 3,318.50
    }

    @Test
    void theSingleCloudFiguresAreUnaffectedByThePeerCloud() {
        SavingsEstimate single = SavingsCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(ReferenceRateCard.standard()).calculate();
        SavingsEstimate withPeer = awsToGoogle().calculate();

        assertNull(single.getMulticloudComparison());
        assertTrue(single.breakEvenSustainedMbps().isEmpty());
        assertEquals(single.getNetMonthlySavings(), withPeer.getNetMonthlySavings());
        assertEquals(single.getEquinixMonthlyCost(), withPeer.getEquinixMonthlyCost());
        assertEquals(single.getBreakEvenGbPerMonth(), withPeer.getBreakEvenGbPerMonth());
        assertEquals(single.isComplete(), withPeer.isComplete());
    }

    @Test
    void markdownRendersTheThreePathsTheBreakEvenAndProvenance() {
        String md = awsToGoogle().calculate().toMarkdown();

        assertTrue(md.contains("### Cloud-to-cloud paths: AWS (us-east-1) <-> GOOGLE_CLOUD (us-east4)"), md);
        assertTrue(md.contains("| Public internet | USD 0.00 | USD 20180.00 | USD 20180.00 |"), md);
        assertTrue(md.contains("| Equinix Fabric private interconnect (two-sided) | USD 4018.50 | USD 3860.00 | USD 7878.50 |"), md);
        assertTrue(md.contains("| Native multicloud interconnect | USD 22870.90 | USD 0.00 | USD 22870.90 |"), md);
        assertTrue(md.contains("Break-even sustained rate (native vs Equinix): 2973.5 Mbps summed over both "
                + "directions (1486.8 Mbps each way)"), md);
        assertTrue(md.contains("12.33 USD/h x 730 h/month = 9000.90 USD/month"), md);
        assertTrue(md.contains("retrieved 2026-09-21"), md);
        assertTrue(md.contains("730 h/month (MulticloudLinkQuote.HOURS_PER_MONTH)"), md);

        String unpriced = awsToGoogle().bandwidthMbps(1_000).calculate().toMarkdown();
        assertTrue(unpriced.contains("| Native multicloud interconnect | _unpriced_ | _unpriced_ | _unpriced_ |"),
                unpriced);
        assertTrue(unpriced.contains("Break-even sustained rate (native vs Equinix): n/a"), unpriced);
    }

    @Test
    void cloudToCloudLeversFailFast() {
        assertThrows(IllegalArgumentException.class, () -> SavingsCalculator.builder(null).toCloud(null));
        assertThrows(IllegalArgumentException.class, () -> SavingsCalculator.builder(null).pathTier(0));
        assertThrows(IllegalArgumentException.class, () -> SavingsCalculator.builder(null).pathTier(6));
        assertThrows(IllegalArgumentException.class,
                () -> SavingsCalculator.builder(null).reverseEgress(Double.NaN, DataUnit.GIGABYTE));
        assertThrows(IllegalArgumentException.class, () -> SavingsCalculator.builder(null).reverseEgress(1, null));
        assertThrows(IllegalArgumentException.class, () -> SavingsCalculator.builder(null)
                .fromCloud(CloudProviderType.AWS).toCloud(CloudProviderType.AWS));
        assertThrows(IllegalArgumentException.class, () -> SavingsCalculator.builder(null)
                .toCloud(CloudProviderType.AWS).fromCloud(CloudProviderType.AWS));

        assertThrows(IllegalArgumentException.class,
                () -> SavingsCalculator.builder(null).fromCloud(CloudProviderType.AWS).toRegion("us-east4").calculate());
        assertThrows(IllegalArgumentException.class,
                () -> SavingsCalculator.builder(null).fromCloud(CloudProviderType.AWS).pathTier(2).calculate());
        assertThrows(IllegalArgumentException.class, () -> SavingsCalculator.builder(null)
                .fromCloud(CloudProviderType.AWS).useAwsFreeTier(true).calculate());
        assertThrows(IllegalArgumentException.class, () -> SavingsCalculator.builder(null)
                .fromCloud(CloudProviderType.AWS).reverseEgress(1, DataUnit.TERABYTE).calculate());
        assertThrows(IllegalArgumentException.class,
                () -> SavingsCalculator.builder(null).toCloud(CloudProviderType.GOOGLE_CLOUD).calculate());
        assertThrows(IllegalArgumentException.class, () -> SavingsCalculator.builder(null)
                .fromCloud(CloudProviderType.AZURE).toCloud(CloudProviderType.GOOGLE_CLOUD)
                .useAwsFreeTier(true).calculate());
    }
}
