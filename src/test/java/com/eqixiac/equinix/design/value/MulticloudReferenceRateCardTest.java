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

import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkRequest;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The bundled native-multicloud-link reference data
 * ({@code /json/ratecard_multicloud_reference_2026_09.json}) as served by
 * {@link ReferenceRateCard#multicloudLink}.
 *
 * <p>Every expected figure below was read from the provider's public pricing page on
 * 2026-09-21: {@code https://aws.amazon.com/interconnect/multicloud/pricing/} (two hourly rates
 * and their 730-hour monthly totals, the no-per-GB statement),
 * {@code https://docs.aws.amazon.com/interconnect/latest/userguide/interconnect-pricing.html}
 * (the free-tier terms), and
 * {@code https://cloud.google.com/network-connectivity/docs/interconnect/pricing} (the Partner
 * Cross-Cloud Interconnect transport table). Figures those pages do not publish are asserted to
 * be absent, not approximated.</p>
 */
class MulticloudReferenceRateCardTest {

    private static final String AWS_SOURCE = "https://aws.amazon.com/interconnect/multicloud/pricing/";
    private static final String GOOGLE_SOURCE = "https://cloud.google.com/network-connectivity/docs/interconnect/pricing";

    private final ReferenceRateCard card = ReferenceRateCard.standard();

    private static MulticloudLinkRequest.MulticloudLinkRequestBuilder awsToGoogle(int mbps) {
        return MulticloudLinkRequest.builder()
                .providerA(CloudProviderType.AWS).regionA("us-east-1")
                .providerZ(CloudProviderType.GOOGLE_CLOUD).regionZ("us-east4")
                .bandwidthMbps(mbps);
    }

    private MulticloudLinkQuote quote(MulticloudLinkRequest request) {
        return card.multicloudLink(request).orElseThrow();
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    // ── Published figures ──

    @Test
    void tenGigabitTierOneMatchesBothPublishedPricePages() {
        MulticloudLinkQuote quote = quote(awsToGoogle(10_000).build());

        PriceQuote aws = quote.getSideA().orElseThrow();
        PriceQuote google = quote.getSideZ().orElseThrow();
        assertMoney("9000.90", aws.getMonthlyRecurring());      // 12.33 USD/h x 730 h (AWS's own worked total)
        assertMoney("13870.00", google.getMonthlyRecurring());  // 19.00 USD/h x 730 h
        assertMoney("22870.90", quote.combinedMonthly().orElseThrow());
        assertEquals("USD", quote.combinedCurrency().orElseThrow());
        assertEquals(PriceSource.REFERENCE, aws.getSource());
        assertEquals(PriceSource.REFERENCE, google.getSource());
        assertMoney("0", aws.getNonRecurring());
    }

    @Test
    void everyQuoteNoteCarriesSourceUrlRetrievalDateAndTheHourlyConversion() {
        MulticloudLinkQuote quote = quote(awsToGoogle(10_000).build());

        String aws = quote.getSideA().orElseThrow().getNote();
        assertTrue(aws.contains(AWS_SOURCE), aws);
        assertTrue(aws.contains("retrieved 2026-09-21"), aws);
        assertTrue(aws.contains("12.33 USD/h x 730 h/month = 9000.90 USD/month"), aws);
        assertTrue(aws.contains("path tier 1"), aws);

        String google = quote.getSideZ().orElseThrow().getNote();
        assertTrue(google.contains(GOOGLE_SOURCE), google);
        assertTrue(google.contains("retrieved 2026-09-21"), google);
        assertTrue(google.contains("19.00 USD/h x 730 h/month = 13870.00 USD/month"), google);
        assertTrue(google.contains("North America"), google);

        assertTrue(quote.getNotes().stream().anyMatch(n -> n.contains("730 h/month")
                && n.contains("HOURS_PER_MONTH")), "the quote states the conversion constant: " + quote.getNotes());
        assertTrue(quote.getNotes().stream().anyMatch(n -> n.contains("Path tier 1 is a caller input")),
                "the quote states that the tier is an input: " + quote.getNotes());
    }

    @Test
    void awsTierFourTenGigabitIsThePublishedLongHaulRate() {
        MulticloudLinkQuote quote = quote(awsToGoogle(10_000).pathTier(4).build());

        assertMoney("37799.40", quote.getSideA().orElseThrow().getMonthlyRecurring()); // 51.78 x 730
        assertMoney("13870.00", quote.getSideZ().orElseThrow().getMonthlyRecurring());
        assertTrue(quote.getSideZ().orElseThrow().getNote().contains("19.00"),
                "Google has no tier concept; its side ignores the AWS path tier");
    }

    @Test
    void googleTransportTableMatchesThePublishedGrid() {
        // Transport location x {1, 5, 10, 100 Gbps}, USD per hour, as published.
        String[][] grid = {
                {"us-east4", "3.50", "17.30", "19.00", "146.60"},            // North America
                {"northamerica-northeast1", "3.50", "17.30", "19.00", "146.60"},
                {"europe-west3", "3.50", "17.30", "19.00", "146.60"},        // Europe
                {"asia-northeast1", "5.00", "24.90", "26.40", "196.10"},     // APAC
                {"southamerica-east1", "7.60", "38.00", "46.90", "299.60"},  // South America
        };
        int[] sizes = {1_000, 5_000, 10_000, 100_000};
        for (String[] row : grid) {
            for (int i = 0; i < sizes.length; i++) {
                MulticloudLinkQuote quote = quote(MulticloudLinkRequest.builder()
                        .providerA(CloudProviderType.AWS)
                        .providerZ(CloudProviderType.GOOGLE_CLOUD).regionZ(row[0])
                        .bandwidthMbps(sizes[i]).build());
                BigDecimal expected = new BigDecimal(row[i + 1]).multiply(BigDecimal.valueOf(730));
                assertEquals(0, expected.compareTo(quote.getSideZ().orElseThrow().getMonthlyRecurring()),
                        row[0] + " @ " + sizes[i] + " Mbps");
            }
        }
    }

    // ── What the pages do not publish is unpriced, never estimated ──

    @Test
    void awsOneGigabitIsUnpricedBecauseAwsPublishesNoSuchRate() {
        MulticloudLinkQuote quote = quote(awsToGoogle(1_000).build());

        assertTrue(quote.getSideA().isEmpty(), "AWS lists 10 Gbps tier 1 and tier 4 only; "
                + "third-party 1 Gbps figures (1.37 and 3.50 USD/h) conflict and are not bundled");
        assertMoney("2555.00", quote.getSideZ().orElseThrow().getMonthlyRecurring()); // Google 3.50 x 730
        assertFalse(quote.isFullyPriced());
        assertTrue(quote.combinedMonthly().isEmpty());

        String reason = quote.getSideAUnpricedReason();
        assertTrue(reason.contains("1000 Mbps") && reason.contains("path tier 1"), reason);
        assertTrue(reason.contains("published sizes: [10000]"), reason);
        assertTrue(reason.contains(AWS_SOURCE) && reason.contains("2026-09-21"), reason);
        assertTrue(reason.contains("CustomRateCard"), "the reason names the override path: " + reason);
    }

    @Test
    void awsUnpublishedTiersAreUnpriced() {
        for (int tier : new int[] {2, 3, 5}) {
            assertTrue(quote(awsToGoogle(10_000).pathTier(tier).build()).getSideA().isEmpty(), "tier " + tier);
        }
    }

    @Test
    void lookupIsAnExactSizeMatchWithNoRoundingUpOrExtrapolation() {
        // 3 Gbps sits between Google's 1 and 5 Gbps rows; 200 Gbps is above the top row.
        assertTrue(quote(awsToGoogle(3_000).build()).getSideZ().isEmpty(), "no round-up to the 5 Gbps row");
        assertTrue(quote(awsToGoogle(200_000).build()).getSideZ().isEmpty(), "no extrapolation above 100 Gbps");
        assertTrue(quote(awsToGoogle(500).build()).getSideZ().isEmpty(), "no round-up from 500 Mbps to 1 Gbps");
        String reason = quote(awsToGoogle(3_000).build()).getSideZUnpricedReason();
        assertTrue(reason.contains("[1000, 5000, 10000, 100000]"), reason);
    }

    @Test
    void googleRegionOutsideThePublishedTransportLocationsIsUnpriced() {
        MulticloudLinkQuote quote = quote(MulticloudLinkRequest.builder()
                .providerA(CloudProviderType.AWS)
                .providerZ(CloudProviderType.GOOGLE_CLOUD).regionZ("australia-southeast1")
                .bandwidthMbps(10_000).build());

        assertTrue(quote.getSideZ().isEmpty());
        assertTrue(quote.getSideZUnpricedReason().contains("australia-southeast1"), quote.getSideZUnpricedReason());
        assertTrue(quote.getSideA().isPresent(), "the AWS side is priced independently");
    }

    @Test
    void googleSideWithoutARegionUsesNorthAmericaAndSaysSo() {
        MulticloudLinkQuote quote = quote(MulticloudLinkRequest.builder()
                .providerA(CloudProviderType.AWS).providerZ(CloudProviderType.GOOGLE_CLOUD)
                .bandwidthMbps(10_000).build());

        PriceQuote google = quote.getSideZ().orElseThrow();
        assertMoney("13870.00", google.getMonthlyRecurring());
        assertTrue(google.getNote().contains("no GOOGLE_CLOUD region supplied; North America transport location assumed"),
                google.getNote());
    }

    @Test
    void oracleAndAzureSidesAreUnpricedWithAReason() {
        for (CloudProviderType peer : List.of(CloudProviderType.ORACLE_CLOUD, CloudProviderType.AZURE)) {
            MulticloudLinkQuote quote = quote(MulticloudLinkRequest.builder()
                    .providerA(CloudProviderType.AWS).providerZ(peer).bandwidthMbps(10_000).build());
            assertTrue(quote.getSideZ().isEmpty(), peer + " has no verified bundled price");
            assertNotNull(quote.getSideZUnpricedReason());
            assertTrue(quote.getSideZUnpricedReason().contains("CustomRateCard"), quote.getSideZUnpricedReason());
            assertMoney("9000.90", quote.getSideA().orElseThrow().getMonthlyRecurring());
        }
    }

    @Test
    void aPairTheBundleKnowsNothingAboutIsEmpty() {
        Optional<MulticloudLinkQuote> none = card.multicloudLink(MulticloudLinkRequest.builder()
                .providerA(CloudProviderType.IBM_CLOUD).providerZ(CloudProviderType.ALIBABA_CLOUD)
                .bandwidthMbps(10_000).build());
        assertTrue(none.isEmpty());
        assertTrue(card.multicloudLink(null).isEmpty());
    }

    @Test
    void sidesArePricedTheSameWhicheverEndIsNamedFirst() {
        MulticloudLinkQuote reversed = quote(MulticloudLinkRequest.builder()
                .providerA(CloudProviderType.GOOGLE_CLOUD).regionA("us-east4")
                .providerZ(CloudProviderType.AWS).regionZ("us-east-1")
                .bandwidthMbps(10_000).build());

        assertMoney("13870.00", reversed.getSideA().orElseThrow().getMonthlyRecurring());
        assertMoney("9000.90", reversed.getSideZ().orElseThrow().getMonthlyRecurring());
    }

    // ── AWS free tier: an explicit opt-in ──

    @Test
    void freeTierIsNeverAppliedByDefault() {
        MulticloudLinkQuote quote = quote(awsToGoogle(500).build());

        assertTrue(quote.getSideA().isEmpty(),
                "without the opt-in a 500 Mbps AWS side is unpriced (no published paid rate), not free");
    }

    @Test
    void freeTierPricesAnEligibleAwsSideAtZeroOnRequest() {
        MulticloudLinkQuote quote = quote(awsToGoogle(500).useAwsFreeTier(true).build());

        PriceQuote aws = quote.getSideA().orElseThrow();
        assertMoney("0", aws.getMonthlyRecurring());
        assertEquals(PriceSource.REFERENCE, aws.getSource());
        assertTrue(aws.getNote().contains("free tier applied on request"), aws.getNote());
        assertTrue(aws.getNote().contains("one free, local (Tier 1) 500 Mbps interconnect per AWS Region"),
                "the note quotes the AWS terms: " + aws.getNote());
        assertTrue(aws.getNote().contains(
                "https://docs.aws.amazon.com/interconnect/latest/userguide/interconnect-pricing.html"), aws.getNote());
        assertTrue(quote.getSideZ().isEmpty(), "Google publishes no 500 Mbps transport rate");
    }

    @Test
    void freeTierRequestIsDeclinedWithAReasonWhenIneligible() {
        // Above 500 Mbps: the listed rate applies.
        MulticloudLinkQuote tenGig = quote(awsToGoogle(10_000).useAwsFreeTier(true).build());
        assertMoney("9000.90", tenGig.getSideA().orElseThrow().getMonthlyRecurring());
        assertTrue(tenGig.getNotes().stream().anyMatch(n -> n.contains("free tier was requested but not applied")
                && n.contains("10000 Mbps")), tenGig.getNotes().toString());

        // Tier 2: the free interconnect is tier 1 only.
        MulticloudLinkQuote tierTwo = quote(awsToGoogle(500).pathTier(2).useAwsFreeTier(true).build());
        assertTrue(tierTwo.getSideA().isEmpty());
        assertTrue(tierTwo.getNotes().stream().anyMatch(n -> n.contains("path tier 1 only")),
                tierTwo.getNotes().toString());

        // Azure is labelled Preview on the AWS product page; the free tier covers GA providers.
        MulticloudLinkQuote azure = quote(MulticloudLinkRequest.builder()
                .providerA(CloudProviderType.AWS).providerZ(CloudProviderType.AZURE)
                .bandwidthMbps(500).useAwsFreeTier(true).build());
        assertTrue(azure.getSideA().isEmpty());
        assertTrue(azure.getNotes().stream().anyMatch(n -> n.contains("AZURE") && n.contains("Preview")),
                azure.getNotes().toString());
    }

    // ── Per-GB rate on the native link ──

    @Test
    void nativeLinkPerGbRateIsAVerifiedZeroForAwsAndGoogleAndUnknownElsewhere() {
        EgressRate aws = card.egress(CloudProviderType.AWS, "us-east-1", EgressPath.MULTICLOUD_INTERCONNECT,
                Term.MONTH_12).orElseThrow();
        assertMoney("0", aws.getPricePerGb());
        assertEquals(PriceSource.REFERENCE, aws.getSource());
        assertTrue(aws.getNote().contains("There are no per-gigabyte data transfer charges."), aws.getNote());
        assertTrue(aws.getNote().contains(AWS_SOURCE) && aws.getNote().contains("2026-09-21"), aws.getNote());

        EgressRate google = card.egress(CloudProviderType.GOOGLE_CLOUD, null, EgressPath.MULTICLOUD_INTERCONNECT,
                null).orElseThrow();
        assertMoney("0", google.getPricePerGb());
        assertTrue(google.getNote().contains(GOOGLE_SOURCE), google.getNote());

        assertTrue(card.egress(CloudProviderType.AZURE, null, EgressPath.MULTICLOUD_INTERCONNECT, null).isEmpty(),
                "no published statement for Azure: unknown, not zero");
        assertTrue(card.egress(CloudProviderType.ORACLE_CLOUD, null, EgressPath.MULTICLOUD_INTERCONNECT, null)
                .isEmpty());
    }

    @Test
    void existingEgressPathsAreUnchanged() {
        assertMoney("0.09", card.egress(CloudProviderType.AWS, null, EgressPath.INTERNET, null)
                .orElseThrow().getPricePerGb());
        assertMoney("0.02", card.egress(CloudProviderType.AWS, null, EgressPath.PRIVATE, null)
                .orElseThrow().getPricePerGb());
        assertMoney("0.0186", card.egress(CloudProviderType.GOOGLE_CLOUD, null, EgressPath.PRIVATE, null)
                .orElseThrow().getPricePerGb());
    }

    // ── Bundle metadata and integrity ──

    @Test
    void multicloudBundleIsDatedSeparatelyFromTheMainBundle() {
        assertEquals("2026-09", card.multicloudAsOf());
        assertEquals("2026-09-21", card.multicloudRetrieved());
        assertEquals("2026-06", card.asOf(), "the main bundle keeps its own vintage");
        assertNotNull(card.multicloudDisclaimer());
        assertTrue(card.multicloudDisclaimer().contains("730 hours per month"), card.multicloudDisclaimer());
        assertEquals(List.of(10_000), card.multicloudPublishedSizes(CloudProviderType.AWS).toList());
        assertEquals(List.of(1_000, 5_000, 10_000, 100_000),
                card.multicloudPublishedSizes(CloudProviderType.GOOGLE_CLOUD).toList());
        assertTrue(card.multicloudPublishedSizes(CloudProviderType.AZURE).isEmpty());
    }

    @Test
    void everyBundledFigureCarriesAnHttpsSourceAndAnIsoRetrievalDate() throws Exception {
        JsonNode root;
        try (InputStream in = ReferenceRateCard.class.getResourceAsStream(
                "/json/ratecard_multicloud_reference_2026_09.json")) {
            assertNotNull(in, "the multicloud bundle is on the classpath");
            root = new ObjectMapper().readTree(in);
        }
        assertEquals(MulticloudLinkQuote.HOURS_PER_MONTH, root.path("hoursPerMonth").asInt(),
                "the bundle's documented conversion matches the code constant");

        int figures = 0;
        for (String section : List.of("rates", "dataTransfer", "freeTier")) {
            for (JsonNode row : root.path(section)) {
                figures++;
                assertTrue(row.path("source").asText().startsWith("https://"), section + ": " + row);
                assertTrue(row.path("retrieved").asText().matches("\\d{4}-\\d{2}-\\d{2}"), section + ": " + row);
            }
        }
        assertEquals(2 + 16 + 2 + 1, figures, "2 AWS rates, 16 Google rates, 2 per-GB statements, 1 free tier");
        for (JsonNode rate : root.path("rates")) {
            assertTrue(rate.path("hourly").isNumber() && rate.path("hourly").decimalValue().signum() > 0,
                    "every bundled rate is a positive number: " + rate);
        }
    }
}
