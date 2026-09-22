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
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.design.value.savings.DataUnit;
import com.eqixiac.equinix.design.value.tco.CostBreakdown;
import com.eqixiac.equinix.design.value.tco.DeploymentArchetype;
import com.eqixiac.equinix.design.value.tco.TcoCalculator;
import com.eqixiac.equinix.design.value.tco.TcoComparison;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The cloud-to-cloud form of the {@link TcoCalculator}: the native multicloud archetype and the
 * two-sided pricing of the other archetypes that a peer cloud switches on.
 *
 * <p>Scenario (bundled reference data, null Fabric): AWS us-east-1 to Google Cloud us-east4,
 * 10 Gbps, 100 TB per month in each direction unless stated.</p>
 * <pre>
 * Public internet : 100,000 GB x 0.09 (AWS) + 100,000 GB x 0.1118 (Google)          = 20,180.00
 * Equinix         : 2,000 + 1,860 private egress + 2 x 350 VC
 *                   + 1,642.50 (AWS port) + 1,676 (Google port) + 300 cross-connect  =  8,178.50
 * Native          : 12.33 x 730 (AWS) + 19.00 x 730 (Google) + 0 data transfer      = 22,870.90
 * </pre>
 */
class NativeMulticloudTcoTest {

    private static TcoCalculator.Builder awsToGoogle() {
        return TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .toCloud(CloudProviderType.GOOGLE_CLOUD).toRegion("us-east4")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(ReferenceRateCard.standard());
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    private static CostBreakdown breakdown(TcoComparison tco, DeploymentArchetype archetype) {
        return tco.breakdown(archetype).orElseThrow();
    }

    // ── The native archetype ──

    @Test
    void aPeerCloudAddsTheNativeArchetypeToTheDefaultSetAndRemovesOnPrem() {
        TcoComparison tco = awsToGoogle().compare();

        assertEquals(List.of(DeploymentArchetype.PUBLIC_CLOUD_INTERNET,
                        DeploymentArchetype.EQUINIX_INTERCONNECT, DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT),
                tco.getBreakdowns().stream().map(CostBreakdown::getArchetype).collect(Collectors.toList()));
    }

    @Test
    void onPremIsNeverRankedInACloudToCloudComparison() {
        // Regression: at the 1000 Mbps default, on-prem (750 transit + 300 + 250 + 975 = 2,275, no
        // egress at all) undercut every two-sided archetype and was recommended for a question
        // (joining two clouds) its inputs do not answer.
        TcoComparison defaults = TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .toCloud(CloudProviderType.GOOGLE_CLOUD).toRegion("us-east4")
                .viaMetro(MetroCode.DC)
                .rateCard(ReferenceRateCard.standard())
                .compare();
        assertTrue(defaults.breakdown(DeploymentArchetype.ON_PREM).isEmpty(), "ON_PREM is not a default with a peer cloud");
        assertNotEquals(DeploymentArchetype.ON_PREM, defaults.getRecommended());
        assertTrue(defaults.getDisclaimer().contains("on-prem is not priced"), defaults.getDisclaimer());

        // Requested explicitly: present, unpriced with the reason, never recommended.
        TcoComparison explicit = TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .toCloud(CloudProviderType.GOOGLE_CLOUD).toRegion("us-east4")
                .viaMetro(MetroCode.DC)
                .rateCard(ReferenceRateCard.standard())
                .archetypes(DeploymentArchetype.ON_PREM, DeploymentArchetype.EQUINIX_INTERCONNECT)
                .compare();
        CostBreakdown onPrem = breakdown(explicit, DeploymentArchetype.ON_PREM);
        assertFalse(onPrem.isPriced());
        assertMoney("0", onPrem.getMonthlyTotal());
        assertTrue(onPrem.getLineItems().isEmpty());
        assertTrue(onPrem.getNote().contains("no egress from AWS or GOOGLE_CLOUD"), onPrem.getNote());
        assertEquals(DeploymentArchetype.EQUINIX_INTERCONNECT, explicit.getRecommended());
        assertFalse(explicit.toMarkdown().contains("**Recommended:** On-premises"), explicit.toMarkdown());

        // Without a peer cloud the single-cloud comparison still prices on-prem as before.
        CostBreakdown singleCloud = breakdown(TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC)
                .rateCard(ReferenceRateCard.standard())
                .compare(), DeploymentArchetype.ON_PREM);
        assertTrue(singleCloud.isPriced());
        assertMoney("2275.00", singleCloud.getMonthlyTotal());
    }

    @Test
    void nativeArchetypeIsTheTwoFlatFeesPlusAnExplicitZeroDataTransferLine() {
        CostBreakdown nativeLink = breakdown(awsToGoogle().compare(), DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT);

        assertTrue(nativeLink.isPriced());
        assertNull(nativeLink.getNote());
        assertEquals("USD", nativeLink.getCurrency());
        assertMoney("9000.90", nativeLink.getLineItems().get("Native link, AWS side (10000 Mbps)"));
        assertMoney("13870.00", nativeLink.getLineItems().get("Native link, GOOGLE_CLOUD side (10000 Mbps)"));
        assertMoney("0", nativeLink.getLineItems().get("Data transfer over the native link (none charged)"));
        assertEquals(3, nativeLink.getLineItems().size());
        assertMoney("22870.90", nativeLink.getMonthlyTotal());
        assertMoney("0", nativeLink.getSetupTotal());
        assertMoney("274450.80", nativeLink.getTotalOverTerm()); // x 12, the default term
    }

    @Test
    void nativeArchetypeProvenanceCarriesSourcesDatesAndThe730HourConversion() {
        CostBreakdown nativeLink = breakdown(awsToGoogle().compare(), DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT);

        String provenance = String.join("\n", nativeLink.getProvenance());
        assertTrue(provenance.contains("https://aws.amazon.com/interconnect/multicloud/pricing/"), provenance);
        assertTrue(provenance.contains("https://cloud.google.com/network-connectivity/docs/interconnect/pricing"),
                provenance);
        assertTrue(provenance.contains("retrieved 2026-09-21"), provenance);
        assertTrue(provenance.contains("12.33 USD/h x 730 h/month = 9000.90 USD/month"), provenance);
        assertTrue(provenance.contains("730 h/month (MulticloudLinkQuote.HOURS_PER_MONTH)"), provenance);
        assertTrue(provenance.contains("There are no per-gigabyte data transfer charges."), provenance);
    }

    @Test
    void pathTierIsAnInputThatSelectsThePublishedTierFourRate() {
        CostBreakdown nativeLink = breakdown(awsToGoogle().pathTier(4).compare(),
                DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT);

        assertMoney("37799.40", nativeLink.getLineItems().get("Native link, AWS side (10000 Mbps)")); // 51.78 x 730
        assertMoney("51669.40", nativeLink.getMonthlyTotal());
    }

    @Test
    void termMultipliesTheFlatFeeWithoutChangingIt() {
        CostBreakdown nativeLink = breakdown(awsToGoogle().term(Term.MONTH_36).compare(),
                DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT);

        assertMoney("22870.90", nativeLink.getMonthlyTotal());
        assertMoney("823352.40", nativeLink.getTotalOverTerm()); // 22,870.90 x 36
    }

    // ── Fairness: the other archetypes are two-sided too ──

    @Test
    void publicInternetArchetypePricesEgressFromBothClouds() {
        CostBreakdown internet = breakdown(awsToGoogle().compare(), DeploymentArchetype.PUBLIC_CLOUD_INTERNET);

        assertTrue(internet.isPriced());
        assertMoney("9000.00", internet.getLineItems().get("Cloud egress from AWS (public internet)"));
        assertMoney("11180.00", internet.getLineItems().get("Cloud egress from GOOGLE_CLOUD (public internet)"));
        assertMoney("20180.00", internet.getMonthlyTotal());
    }

    @Test
    void equinixArchetypeCarriesTwoConnectionsBothPortsAndEgressBothWays() {
        CostBreakdown equinix = breakdown(awsToGoogle().compare(), DeploymentArchetype.EQUINIX_INTERCONNECT);

        assertTrue(equinix.isPriced());
        assertMoney("2000.00", equinix.getLineItems().get("Cloud egress from AWS (private interconnect)"));
        assertMoney("1860.00", equinix.getLineItems().get("Cloud egress from GOOGLE_CLOUD (private interconnect)"));
        assertMoney("350", equinix.getLineItems().get("Equinix Fabric connection to AWS"));
        assertMoney("350", equinix.getLineItems().get("Equinix Fabric connection to GOOGLE_CLOUD"));
        assertMoney("1642.50", equinix.getLineItems().get("Cloud provider interconnect port (AWS)"));
        assertMoney("1676", equinix.getLineItems().get("Cloud provider interconnect port (GOOGLE_CLOUD)"));
        assertMoney("300", equinix.getLineItems().get("Equinix cross-connect"));
        assertEquals(7, equinix.getLineItems().size());
        assertMoney("8178.50", equinix.getMonthlyTotal());
    }

    @Test
    void equinixConnectionSetupChargeIsCountedOncePerConnection() {
        CustomRateCard withSetup = CustomRateCard.builder()
                .connectionRate(ConnectionType.EVPL_VC, 10_000, new BigDecimal("400"), new BigDecimal("250"))
                .build();
        CostBreakdown equinix = breakdown(
                awsToGoogle().rateCard(RateCard.layered(withSetup, ReferenceRateCard.standard())).compare(),
                DeploymentArchetype.EQUINIX_INTERCONNECT);

        assertMoney("500", equinix.getSetupTotal()); // 2 connections x 250
        assertMoney("8278.50", equinix.getMonthlyTotal()); // 8,178.50 + 2 x (400 - 350)
    }

    @Test
    void reverseEgressSetsThePeerCloudsVolumeAndDefaultsToSymmetric() {
        TcoComparison symmetric = awsToGoogle().compare();
        assertTrue(symmetric.getTrafficNote().contains("assumed equal to the forward volume"),
                symmetric.getTrafficNote());

        TcoComparison oneWay = awsToGoogle().reverseEgress(0, DataUnit.GIGABYTE).compare();
        assertFalse(oneWay.getTrafficNote().contains("assumed"), oneWay.getTrafficNote());
        assertMoney("9000.00", breakdown(oneWay, DeploymentArchetype.PUBLIC_CLOUD_INTERNET).getMonthlyTotal());
        assertMoney("6318.50", breakdown(oneWay, DeploymentArchetype.EQUINIX_INTERCONNECT).getMonthlyTotal());
        assertMoney("22870.90", breakdown(oneWay, DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT).getMonthlyTotal());

        TcoComparison asymmetric = awsToGoogle().reverseEgress(40, DataUnit.TERABYTE).compare();
        assertMoney("13472.00", breakdown(asymmetric, DeploymentArchetype.PUBLIC_CLOUD_INTERNET).getMonthlyTotal());
        assertTrue(asymmetric.getTrafficNote().contains("40000 GB/mo"), asymmetric.getTrafficNote());
    }

    @Test
    void equinixIsRecommendedAtLowVolumeAndTheNativeLinkAtHighVolume() {
        TcoComparison low = awsToGoogle().compare();
        assertEquals(DeploymentArchetype.EQUINIX_INTERCONNECT, low.getRecommended());
        assertMoney("12001.50", low.getMonthlySavingsVsBaseline()); // 20,180.00 - 8,178.50

        // 1 PB each way: Equinix = 20,000 + 18,600 egress + 4,318.50 fixed = 42,918.50 > 22,870.90.
        TcoComparison high = awsToGoogle().egress(1, DataUnit.PETABYTE)
                .archetypes(DeploymentArchetype.PUBLIC_CLOUD_INTERNET, DeploymentArchetype.EQUINIX_INTERCONNECT,
                        DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT)
                .compare();
        assertMoney("42918.50", breakdown(high, DeploymentArchetype.EQUINIX_INTERCONNECT).getMonthlyTotal());
        assertEquals(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT, high.getRecommended());
    }

    @Test
    void equinixArchetypeStatesWhichASideItsFiguresContain() {
        String withCrossConnect = String.join("\n",
                breakdown(awsToGoogle().compare(), DeploymentArchetype.EQUINIX_INTERCONNECT).getProvenance());
        assertTrue(withCrossConnect.contains("Equinix A-side: no Fabric Cloud Router was requested"), withCrossConnect);

        String noASide = String.join("\n", breakdown(awsToGoogle().crossConnects(0).compare(),
                DeploymentArchetype.EQUINIX_INTERCONNECT).getProvenance());
        assertTrue(noASide.contains("is not priced and this total is understated"), noASide);

        CostBreakdown withRouter = breakdown(awsToGoogle().includeCloudRouter("STANDARD").compare(),
                DeploymentArchetype.EQUINIX_INTERCONNECT);
        assertMoney("9378.50", withRouter.getMonthlyTotal()); // + 1,200 reference router, once
        assertFalse(String.join("\n", withRouter.getProvenance()).contains("Equinix A-side"));
    }

    @Test
    void equinixArchetypeIsPartialWhenAPeerCloudHasNoReferencePortOrEgressRate() {
        // ORACLE_CLOUD: no reference egress rates and no reference CSP port figure.
        TcoComparison tco = TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).toCloud(CloudProviderType.ORACLE_CLOUD)
                .bandwidthMbps(10_000).rateCard(ReferenceRateCard.standard()).compare();

        CostBreakdown equinix = breakdown(tco, DeploymentArchetype.EQUINIX_INTERCONNECT);
        assertFalse(equinix.isPriced(), "a missing peer-side component must not be silently dropped");
        assertTrue(equinix.getNote().contains("Private egress rate unavailable for ORACLE_CLOUD"), equinix.getNote());
        assertTrue(equinix.getNote().contains("No reference CSP interconnect-port figure for ORACLE_CLOUD"),
                equinix.getNote());
        CostBreakdown internet = breakdown(tco, DeploymentArchetype.PUBLIC_CLOUD_INTERNET);
        assertFalse(internet.isPriced());
        assertMoney("9000.00", internet.getMonthlyTotal()); // the AWS direction stays visible
        assertTrue(internet.getNote().contains("ORACLE_CLOUD"), internet.getNote());
    }

    // ── Partial pricing ──

    @Test
    void anUnpricedSideLeavesTheNativeArchetypePartialWithNoZeroDataTransferLine() {
        TcoComparison tco = awsToGoogle().bandwidthMbps(1_000).compare();
        CostBreakdown nativeLink = breakdown(tco, DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT);

        assertFalse(nativeLink.isPriced());
        assertMoney("2555.00", nativeLink.getMonthlyTotal()); // the Google side only; labelled partial
        assertEquals(List.of("Native link, GOOGLE_CLOUD side (1000 Mbps)"),
                List.copyOf(nativeLink.getLineItems().keySet()),
                "no AWS line and no data-transfer line when a side is unpriced");
        assertTrue(nativeLink.getNote().contains("The AWS side is unpriced"), nativeLink.getNote());
        assertTrue(nativeLink.getNote().contains("totals are partial"), nativeLink.getNote());
        assertNotEquals(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT, tco.getRecommended(),
                "a partially priced archetype is never recommended, however low its partial total");
    }

    @Test
    void nativeArchetypeIsUnpricedWhenNoCardHoldsNativeLinkPrices() {
        CustomRateCard egressOnly = CustomRateCard.builder()
                .egressRate(CloudProviderType.AWS, EgressPath.INTERNET, new BigDecimal("0.09"))
                .build();
        CostBreakdown nativeLink = breakdown(awsToGoogle().rateCard(egressOnly).compare(),
                DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT);

        assertFalse(nativeLink.isPriced());
        assertMoney("0", nativeLink.getMonthlyTotal());
        assertTrue(nativeLink.getNote().contains("No rate card in the chain holds native multicloud link prices"),
                nativeLink.getNote());
    }

    @Test
    void nativeArchetypeIsPartialWhenThePerGbRateOnTheLinkIsUnknown() {
        // A custom card alone prices both flat fees but declares no MULTICLOUD_INTERCONNECT egress rate.
        CustomRateCard flatFeesOnly = CustomRateCard.builder()
                .multicloudLinkRate(CloudProviderType.AWS, 10_000, new BigDecimal("8000"))
                .multicloudLinkRate(CloudProviderType.GOOGLE_CLOUD, 10_000, new BigDecimal("12000"))
                .build();
        CostBreakdown unknownPerGb = breakdown(awsToGoogle().rateCard(flatFeesOnly).compare(),
                DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT);
        assertFalse(unknownPerGb.isPriced());
        assertTrue(unknownPerGb.getNote().contains("Per-GB rate on the native link unavailable for AWS and GOOGLE_CLOUD"),
                unknownPerGb.getNote());
        assertFalse(unknownPerGb.getLineItems().keySet().stream().anyMatch(k -> k.startsWith("Data transfer")));

        // Declared (non-zero here): the line is priced on each direction's volume.
        CustomRateCard withPerGb = CustomRateCard.builder()
                .multicloudLinkRate(CloudProviderType.AWS, 10_000, new BigDecimal("8000"))
                .multicloudLinkRate(CloudProviderType.GOOGLE_CLOUD, 10_000, new BigDecimal("12000"))
                .egressRate(CloudProviderType.AWS, EgressPath.MULTICLOUD_INTERCONNECT, new BigDecimal("0.001"))
                .egressRate(CloudProviderType.GOOGLE_CLOUD, EgressPath.MULTICLOUD_INTERCONNECT, BigDecimal.ZERO)
                .build();
        CostBreakdown priced = breakdown(awsToGoogle().rateCard(withPerGb).compare(),
                DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT);
        assertTrue(priced.isPriced());
        assertMoney("100.00", priced.getLineItems().get("Data transfer over the native link")); // 100,000 GB x 0.001
        assertMoney("20100.00", priced.getMonthlyTotal());
    }

    // ── AWS free tier ──

    @Test
    void awsFreeTierIsAppliedOnlyOnRequest() {
        // Google publishes no 500 Mbps rate, so a negotiated Google figure completes the link.
        RateCard card = RateCard.layered(
                CustomRateCard.builder()
                        .multicloudLinkHourlyRate(CloudProviderType.GOOGLE_CLOUD, 500, new BigDecimal("1.75")).build(),
                ReferenceRateCard.standard());

        CostBreakdown withoutOptIn = breakdown(awsToGoogle().bandwidthMbps(500).rateCard(card).compare(),
                DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT);
        assertFalse(withoutOptIn.isPriced(), "no opt-in: the AWS side is unpriced, not free");
        assertFalse(withoutOptIn.getLineItems().containsKey("Native link, AWS side (500 Mbps)"));

        TcoComparison optedIn = awsToGoogle().bandwidthMbps(500).rateCard(card).useAwsFreeTier(true).compare();
        CostBreakdown withOptIn = breakdown(optedIn, DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT);
        assertTrue(withOptIn.isPriced());
        assertMoney("0", withOptIn.getLineItems().get("Native link, AWS side (500 Mbps)"));
        assertMoney("1277.50", withOptIn.getMonthlyTotal()); // 1.75 x 730, Google side only
        assertTrue(String.join("\n", withOptIn.getProvenance()).contains("free tier applied on request"));
        assertTrue(optedIn.getTrafficNote().contains("AWS free tier requested"), optedIn.getTrafficNote());
        assertTrue(optedIn.getDisclaimer().contains("applied only when useAwsFreeTier(true) is set"));
    }

    @Test
    void aCustomAwsRateStatesThatTheRequestedFreeTierWasNotApplied() {
        // Regression: with a custom card pricing the AWS side, useAwsFreeTier(true) changed no
        // figure and no provenance line said so, while the traffic note read "AWS free tier
        // requested" and the disclaimer described when the tier is applied.
        RateCard card = RateCard.layered(
                CustomRateCard.builder()
                        .multicloudLinkRate(CloudProviderType.AWS, 500, new BigDecimal("2000"))
                        .multicloudLinkRate(CloudProviderType.GOOGLE_CLOUD, 500, new BigDecimal("1277.50"))
                        .build(),
                ReferenceRateCard.standard());
        TcoComparison tco = awsToGoogle().bandwidthMbps(500).rateCard(card).useAwsFreeTier(true)
                .archetypes(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT).compare();
        CostBreakdown nativeLink = breakdown(tco, DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT);

        assertTrue(nativeLink.isPriced());
        assertMoney("2000", nativeLink.getLineItems().get("Native link, AWS side (500 Mbps)"));
        String provenance = String.join("\n", nativeLink.getProvenance());
        assertTrue(provenance.contains("The AWS free tier was requested but not applied"), provenance);
        assertTrue(provenance.contains("CustomRateCard does not model the free tier"), provenance);
        assertTrue(tco.toMarkdown().contains("The AWS free tier was requested but not applied"), tco.toMarkdown());
    }

    // ── Report ──

    @Test
    void markdownRendersTheNativeRowsWithProvenanceAndThe730HourNote() {
        String md = awsToGoogle().compare().toMarkdown();

        assertTrue(md.contains("**Traffic:** AWS (us-east-1) -> GOOGLE_CLOUD (us-east4): 100000 GB/mo"), md);
        assertTrue(md.contains("| Native cloud-to-cloud interconnect | USD 22870.90 | USD 0.00 | USD 274450.80 |"), md);
        assertTrue(md.contains("### Native cloud-to-cloud interconnect: line items and provenance"), md);
        assertTrue(md.contains("| Native link, AWS side (10000 Mbps) | USD 9000.90 |"), md);
        assertTrue(md.contains("| Data transfer over the native link (none charged) | USD 0.00 |"), md);
        assertTrue(md.contains("730 h/month"), md);
        assertTrue(md.contains("retrieved 2026-09-21"), md);
        assertTrue(md.contains("### Equinix interconnected: line items and provenance"), md);

        String partial = awsToGoogle().bandwidthMbps(1_000).compare().toMarkdown();
        assertTrue(partial.contains("| Native cloud-to-cloud interconnect | _unavailable_ |"), partial);
        assertTrue(partial.contains("- Not fully priced: The AWS side is unpriced"), partial);
    }

    // ── Fail-fast lever validation ──

    @Test
    void nativeArchetypeRequiresAPeerCloud() {
        TcoCalculator.Builder b = TcoCalculator.builder(null)
                .fromCloud(CloudProviderType.AWS)
                .archetypes(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, b::compare);
        assertTrue(e.getMessage().contains("toCloud"), e.getMessage());
    }

    @Test
    void cloudToCloudLeversFailFast() {
        assertThrows(IllegalArgumentException.class, () -> TcoCalculator.builder(null).toCloud(null));
        assertThrows(IllegalArgumentException.class, () -> TcoCalculator.builder(null).pathTier(0));
        assertThrows(IllegalArgumentException.class, () -> TcoCalculator.builder(null).pathTier(6));
        assertThrows(IllegalArgumentException.class,
                () -> TcoCalculator.builder(null).reverseEgress(-1, DataUnit.GIGABYTE));
        assertThrows(IllegalArgumentException.class, () -> TcoCalculator.builder(null).reverseEgress(1, null));

        // The peer must differ from fromCloud, whichever is set first.
        assertThrows(IllegalArgumentException.class, () -> TcoCalculator.builder(null)
                .fromCloud(CloudProviderType.AWS).toCloud(CloudProviderType.AWS));
        assertThrows(IllegalArgumentException.class, () -> TcoCalculator.builder(null)
                .toCloud(CloudProviderType.AWS).fromCloud(CloudProviderType.AWS));

        // Levers that only mean something with a peer cloud are rejected without one.
        assertThrows(IllegalArgumentException.class,
                () -> TcoCalculator.builder(null).fromCloud(CloudProviderType.AWS).toRegion("us-east4").compare());
        assertThrows(IllegalArgumentException.class,
                () -> TcoCalculator.builder(null).fromCloud(CloudProviderType.AWS).pathTier(2).compare());
        assertThrows(IllegalArgumentException.class,
                () -> TcoCalculator.builder(null).fromCloud(CloudProviderType.AWS).useAwsFreeTier(true).compare());
        assertThrows(IllegalArgumentException.class, () -> TcoCalculator.builder(null)
                .fromCloud(CloudProviderType.AWS).reverseEgress(1, DataUnit.TERABYTE).compare());

        // toCloud needs fromCloud; the AWS free tier needs AWS.
        assertThrows(IllegalArgumentException.class,
                () -> TcoCalculator.builder(null).toCloud(CloudProviderType.GOOGLE_CLOUD).compare());
        assertThrows(IllegalArgumentException.class, () -> TcoCalculator.builder(null)
                .fromCloud(CloudProviderType.AZURE).toCloud(CloudProviderType.GOOGLE_CLOUD)
                .useAwsFreeTier(true).compare());

        // useAwsFreeTier(false) is the default and is accepted without a peer cloud.
        assertDoesNotThrow(() -> TcoCalculator.builder(null).fromCloud(CloudProviderType.AWS)
                .rateCard(ReferenceRateCard.standard()).useAwsFreeTier(false).compare());
    }
}
